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

  private final Map<File, Entry> myEntries;

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

    myEntries = new HashMap<File, Entry>();
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
        if (myEntries.containsKey(f)) {
          continue;
        }
        if (f.isFile()) {
          myEntries.put(f, new FileEntry(type));
        } else if (f.isDirectory()) {
          myEntries.put(f, new DirEntry(type));
        } else {
          final String mask = f.getAbsolutePath();
          if ((mask.contains("*") || mask.contains("?"))) {
            final File baseDir = new File(getDirWithoutPattern(mask));
            final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(FileUtil.getRelativePath(baseDir, f)));
            myEntries.put(f, new MaskEntry(type, baseDir, pattern));
          }
        }
      }
    }
  }

  private synchronized void processEntries() {
    for (Map.Entry f : myEntries.entrySet()) {
      final File k = (File) f.getKey();
      final Entry e = (Entry) f.getValue();
      final String type = e.getEntryType();
      if (FileEntry.TYPE.equals(type)) {
        processFile(k, (FileEntry) e);
      } else if (DirEntry.TYPE.equals(type)) {
        processDir(k, (DirEntry) e);
      } else if (MaskEntry.TYPE.equals(type)) {
        processMask(k, (MaskEntry) e);
      }
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
    return myPlugin.parseOutOfDate() || (file.lastModified() >= myPlugin.getBuildStartTime());
  }

  public void logTotals() {
    for (Map.Entry f : myEntries.entrySet()) {
      final File k = (File) f.getKey();
      final Entry e = (Entry) f.getValue();
      final String type = e.getEntryType();
      if (FileEntry.TYPE.equals(type)) {
        logFileTotals(k, (FileEntry) e);
      } else if (DirEntry.TYPE.equals(type)) {
        logDirTotals(k, (DirEntry) e);
      } else if (MaskEntry.TYPE.equals(type)) {
        logMaskTotals(k, (MaskEntry) e);
      }
      myInput.get(e.getType()).remove(k);
    }
    logUnknownTotals();
  }

  private void logUnknownTotals() {
    for (String type : myInput.keySet()) {
      for (File f : myInput.get(type)) {
        myPlugin.getLogger().warning(f.getAbsolutePath() + " didn't appear on disk during the build");
      }
    }
  }

  private void logMaskTotals(File m, MaskEntry me) {
    if (!me.isActive()) {
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

  private void logDirTotals(File d, DirEntry de) {
    if (!de.isActive()) {
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

  private static abstract class Entry {
    private final String myType;
    private boolean myActive;

    public Entry(String type) {
      myType = type;
      myActive = false;
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

    public abstract String getEntryType();
  }

  private static class FileEntry extends Entry {
    public static final String TYPE = "FILE";

    private String myMessage;

    public FileEntry(String type) {
      super(type);
      myMessage = "";
    }

    public String getEntryType() {
      return TYPE;
    }

    public String getMessage() {
      return myMessage;
    }

    public void setMessage(String message) {
      myMessage = message;
    }

  }

  private static class DirEntry extends Entry {
    public static final String TYPE = "DIR";

    private final Map<File, FileEntry> myFiles;

    public DirEntry(String type) {
      super(type);
      myFiles = new HashMap<File, FileEntry>();
    }

    public String getEntryType() {
      return TYPE;
    }

    public Map<File, FileEntry> getFiles() {
      return myFiles;
    }
  }

  private static class MaskEntry extends DirEntry {
    public static final String TYPE = "MASK";

    private final Map<File, DirEntry> myDirs;
    private final File myBaseDir;
    private final Pattern myPattern;

    public MaskEntry(String type, File baseDir, Pattern pattern) {
      super(type);
      myDirs = new HashMap<File, DirEntry>();
      myBaseDir = baseDir;
      myPattern = pattern;
    }

    public String getEntryType() {
      return TYPE;
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