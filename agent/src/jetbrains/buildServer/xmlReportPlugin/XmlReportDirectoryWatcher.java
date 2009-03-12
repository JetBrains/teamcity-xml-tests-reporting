/*
 * Copyright 2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.xmlReportPlugin;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;


public class XmlReportDirectoryWatcher extends Thread {
  private static final int SCAN_INTERVAL = 50;

  private final XmlReportPlugin myPlugin;

  private final LinkedBlockingQueue<Pair<String, File>> myReportQueue;
  private final Map<String, List<File>> myInput;

  private final Map<File, FileEntry> myFiles;
  private final Map<File, DirEntry> myDirs;
  private final Map<File, MaskEntry> myMasks;

  public XmlReportDirectoryWatcher(@NotNull final XmlReportPlugin plugin,
                                   @NotNull final List<File> input,
                                   @NotNull final String type,
                                   @NotNull final LinkedBlockingQueue<Pair<String, File>> queue) {
    super("xml-report-plugin-DirectoryWatcher");

    myPlugin = plugin;
    myInput = new LinkedHashMap<String, List<File>>();
    if (isTypeValid(type)) {
      myInput.put(type, input);
    }
    myReportQueue = queue;

    myFiles = new HashMap<File, FileEntry>();
    myDirs = new HashMap<File, DirEntry>();
    myMasks = new HashMap<File, MaskEntry>();
  }

  public void run() {
    while (!myPlugin.isStopped()) {
      try {
        scanInput();
        processEntries();
        Thread.sleep(SCAN_INTERVAL);
      } catch (Throwable e) {
        myPlugin.getLogger().exception(e);
      }
    }
    scanInput();
    processEntries();
  }

  public synchronized void addParams(List<File> params, String type) {
    if (!isTypeValid(type)) return;
    if (!myInput.containsKey(type)) {
      myInput.put(type, params);
    } else {
      myInput.get(type).addAll(params);
    }
  }

  private synchronized void scanInput() {
    for (String type : myInput.keySet()) {
      final List<File> input = myInput.get(type);
      for (File f : input) {
        if (f.isFile() && !myFiles.containsKey(f)) {
          myFiles.put(f, new FileEntry(type));
        } else if (f.isDirectory() && !myDirs.containsKey(f)) {
          myDirs.put(f, new DirEntry(type));
        } else {
          final String mask = f.getAbsolutePath();
          if ((mask.contains("*") || mask.contains("?")) && !myMasks.containsKey(f)) {
            final File baseDir = new File(getDirWithoutPattern(mask));
            final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(FileUtil.getRelativePath(baseDir, f)));
            myMasks.put(f, new MaskEntry(type, baseDir, pattern));
          }
        }
      }
    }
  }

  private synchronized void processEntries() {
    processFiles();
    processDirs();
    processMasks();
  }

  private void processFiles() {
    for (Map.Entry f : myFiles.entrySet()) {
      processFile((File) f.getKey(), (FileEntry) f.getValue());
    }
  }

  private void processFile(File f, FileEntry fe) {
    if (fe.isActive()) return;
    if (!f.canRead()) {
      fe.setMessage(": unable to read file");
    } else if (!timeConstraintsSatisfied(f)) {
      fe.setMessage(" has modification date preceding build start time");
    } else {
      fe.setActive(true);
      try {
        myReportQueue.put(new Pair<String, File>(fe.getType(), f));
      } catch (InterruptedException e) {
      }
    }
  }

  private void processDirs() {
    for (Map.Entry d : myDirs.entrySet()) {
      processDir((File) d.getKey(), (DirEntry) d.getValue());
    }
  }

  private void processDir(File d, DirEntry de) {
    final File[] files = d.listFiles();
    if ((files == null) || (files.length == 0)) return;
    for (File f : files) {
      processFileInDir(de, f);
    }
  }

  private void processFileInDir(DirEntry de, File f) {
    final String type = de.getType();
    if (myInput.get(type).contains(f)) return;
    FileEntry fe = new FileEntry(type);
    if (de.getFiles().containsKey(f)) {
      fe = de.getFiles().get(f);
    } else {
      de.getFiles().put(f, fe);
      de.setActive(true);
    }
    processFile(f, fe);
  }

  private void processMasks() {
    for (Map.Entry m : myMasks.entrySet()) {
      processMask((File) m.getKey(), (MaskEntry) m.getValue());
    }
  }

  private void processMask(File m, MaskEntry me) {
    for (File f : collectFiles(me.getPattern(), me.getBaseDir())) {
      if (f.isFile()) {
        processFileInDir(me, f);
      } else if (f.isDirectory()) {
        processDirInMask(me, f);
      }
    }
  }

  private void processDirInMask(MaskEntry me, File d) {
    final String type = me.getType();
    if (myInput.get(type).contains(d)) return;
    DirEntry de = new DirEntry(type);
    if (me.getDirs().containsKey(d)) {
      de = me.getDirs().get(d);
    } else {
      me.getDirs().put(d, de);
      me.setActive(true);
    }
    processDir(d, de);
  }

  private boolean isTypeValid(String type) {
    if (XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.containsKey(type)) {
      return true;
    }
    myPlugin.getLogger().error("Illegal report type value specified");
    return false;
  }

  private boolean timeConstraintsSatisfied(File file) {
    return myPlugin.parseOutOfDate()
      || (file.lastModified() >= myPlugin.getBuildStartTime());
  }

  public void logTotals() {
    logFilesTotals();
    logDirsTotals();
    logMasksTotals();
    logUnknownTotals();
  }

  private void logUnknownTotals() {
    for (String type : myInput.keySet()) {
      for (File f : myInput.get(type)) {
        myPlugin.getLogger().warning(f.getAbsolutePath() + " didn't appear on disk during the build");
      }
    }
  }

  private void logMasksTotals() {
    for (File m : myMasks.keySet()) {
      final MaskEntry me = myMasks.get(m);
      logMaskTotals(m, me);
      myInput.get(me.getType()).remove(m);
    }
  }

  private void logMaskTotals(File m, MaskEntry me) {
    if (!me.isActive()) {
      //TODO: remove
      if (me.getFiles().values().size() > 0) {
        myPlugin.getLogger().exception(new Exception("MUST BE ACTIVE"));
      }
      //TODO: remove
      if (me.getDirs().values().size() > 0) {
        myPlugin.getLogger().exception(new Exception("MUST BE ACTIVE"));
      }
      myPlugin.getLogger().warning(m.getAbsolutePath() + ": nothing matching found");
    } else {
      final Map<File, DirEntry> dirs = me.getDirs();
      if (dirs.size() > 0) {
        myPlugin.getLogger().message(m.getAbsolutePath() + ": " + dirs.size() + " matching directory(ies) found");
      }
      for (File d : dirs.keySet()) {
        final DirEntry fe = dirs.get(d);
        logDirTotals(d, fe);
      }
      final Map<File, FileEntry> files = me.getFiles();
      if (files.size() > 0) {
        myPlugin.getLogger().message(m.getAbsolutePath() + ": " + files.size() + " matching files(s) found");
      }
      for (File f : files.keySet()) {
        final FileEntry fe = files.get(f);
        logFileTotals(f, fe);
      }
    }
  }

  private void logDirsTotals() {
    for (File d : myDirs.keySet()) {
      final DirEntry de = myDirs.get(d);
      logDirTotals(d, de);
      myInput.get(de.getType()).remove(d);
    }
  }

  private void logDirTotals(File d, DirEntry de) {
    if (!de.isActive()) {
      //TODO: remove
      if (de.getFiles().values().size() > 0) {
        myPlugin.getLogger().exception(new Exception("MUST BE ACTIVE"));
      }
      myPlugin.getLogger().warning(d.getAbsolutePath() + ": no reports found in directory");
    } else {
      final Map<File, FileEntry> files = de.getFiles();
      myPlugin.getLogger().message(d.getAbsolutePath() + " directory: " + files.size() + " files(s) found");
      for (File f : files.keySet()) {
        final FileEntry fe = files.get(f);
        logFileTotals(f, fe);
      }
    }
  }

  private void logFilesTotals() {
    for (File f : myFiles.keySet()) {
      final FileEntry fe = myFiles.get(f);
      logFileTotals(f, fe);
      myInput.get(fe.getType()).remove(f);
    }
  }

  private void logFileTotals(File f, FileEntry fe) {
    if (!fe.isActive()) {
      myPlugin.getLogger().warning(f.getAbsolutePath() + fe.getMessage());
    }
  }

  private static String getDirWithoutPattern(String pathWithWildCard) {
    String t = pathWithWildCard.replace('\\', '/');
    final int firstStar = t.indexOf('*');
    final int firstQuest = t.indexOf('?');
    int mark = firstStar < 0 ? firstQuest :
      (firstStar < firstQuest || firstQuest < 0 ? firstStar : firstQuest);

    final int lastSlash = t.lastIndexOf('/', mark);
    return lastSlash > 0 ? pathWithWildCard.substring(0, lastSlash) : "";
  }

  private static ArrayList<File> collectFiles(final Pattern pattern, final File basePatternDir) {
    final ArrayList<File> files = new ArrayList<File>();
    FileUtil.collectMatchedFiles(basePatternDir, pattern, files);
    return files;
  }

//  private void logDirectoryTotals(File dir) {
//    if (!dir.exists()) {
//      myPlugin.getLogger().warning(dir.getPath() + " directory didn't appear on disk during the build");
//    } else if (!dir.isDirectory()) {
//      myPlugin.getLogger().warning(dir.getPath() + " is not actually a directory");
//    } else if (!myActiveDirectories.containsKey(dir)) {
//      myPlugin.getLogger().warning(dir.getPath() + ": no reports found in directory");
//    } else {
//      logActiveDirectoryTotals(dir);
//    }
//  }
//
//  private void logActiveDirectoryTotals(File dir) {
//    final List<File> processedFiles = myActiveDirectories.get(dir)[0];
//    final List<File> unprocessedFiles = myActiveDirectories.get(dir)[1];
//    final int fileNumber = processedFiles.size() + unprocessedFiles.size();
//
//    String message = dir.getPath() + " directory: " + fileNumber + " files(s) found";
//    if (unprocessedFiles.size() > 0) {
//      message = message.concat(", " + unprocessedFiles.size() + " of them unprocessed (see reasons below):");
//    }
//    myPlugin.getLogger().message(message);
//    logUnprocessedFilesTotals(unprocessedFiles);
//  }
//
//  private void logUnprocessedFilesTotals(List<File> unprocessedFiles) {
//    for (File file : unprocessedFiles) {
//      if (!file.isFile()) {
//        myPlugin.getLogger().warning(file.getPath() + " is not actually a file");
//        continue;
//      }
//      if (!file.canRead()) {
//        myPlugin.getLogger().warning(file.getPath() + ": unable to read file");
//        continue;
//      }
//      if (file.lastModified() < myPlugin.getBuildStartTime()) {
//        myPlugin.getLogger().warning(file.getPath() + " file has modification date preceding build start time");
//      }
//    }
//  }

  private static class FileEntry {
    private final String myType;
    private boolean myActive;
    private String myMessage;

    public FileEntry(String type) {
      myType = type;
      myActive = false;
      myMessage = "";
    }

    public String getMessage() {
      return myMessage;
    }

    public void setMessage(String message) {
      myMessage = message;
    }

    public boolean isActive() {
      return myActive;
    }

    public void setActive(boolean active) {
      myActive = active;
    }

    public String getType() {
      return myType;
    }
  }

  private static class DirEntry extends FileEntry {
    private final Map<File, FileEntry> myFiles;

    public DirEntry(String type) {
      super(type);
      myFiles = new HashMap<File, FileEntry>();
    }

    public Map<File, FileEntry> getFiles() {
      return myFiles;
    }
  }

  private static class MaskEntry extends DirEntry {
    private final Map<File, DirEntry> myDirs;
    private final File myBaseDir;
    private final Pattern myPattern;

    public MaskEntry(String type, File baseDir, Pattern pattern) {
      super(type);
      myDirs = new HashMap<File, DirEntry>();
      myBaseDir = baseDir;
      myPattern = pattern;
    }

    public Map<File, DirEntry> getDirs() {
      return myDirs;
    }

    public File getBaseDir() {
      return myBaseDir;
    }

    public Pattern getPattern() {
      return myPattern;
    }
  }
}