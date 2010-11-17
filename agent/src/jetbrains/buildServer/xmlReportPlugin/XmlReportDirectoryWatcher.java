/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin.LOG;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.SUPPORTED_REPORT_TYPES;


public class XmlReportDirectoryWatcher extends Thread implements XmlReportPluginParametersImpl.ParametersListener {
  private static final int SCAN_INTERVAL = 100;

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private final XmlReportPluginParameters myParameters;

  private final LinkedBlockingQueue<ReportData> myReportQueue;
  private final Map<File, MaskData> myMaskHash = new HashMap<File, MaskData>();
  private final Map<String, TypeStatistics> myStatistics = new HashMap<String, TypeStatistics>();

  private volatile boolean myStopSignaled;

  private static boolean isAntMask(File f) {
    return isAntMask(f.getAbsolutePath());
  }

  private static boolean isAntMask(String s) {
    return MATCHER.isPattern(s);
  }

  public XmlReportDirectoryWatcher(@NotNull final XmlReportPluginParameters parameters,
                                   @NotNull final LinkedBlockingQueue<ReportData> queue) {
    super("xml-report-plugin-DirectoryWatcher");

    myParameters = parameters;
    myParameters.setListener(this);
    myReportQueue = queue;
  }

  public void pathsAdded(@NotNull String type, @NotNull Set<File> paths) {
    // we use thread logger here, as pathsAdded is called outside watcher thread
    logWatchingPaths(paths, type, myParameters.getLogger().getThreadLogger());
    checkExistingPaths(paths);
  }

  private void logWatchingPaths(final Set<File> paths, String type, final BuildProgressLogger logger) {
    logInTarget(type, new Runnable() {
      public void run() {
        String message = "Watching paths: ";
        if (paths.size() == 0) {
          message += "<no paths>";
          warning(logger, message);
        } else {
          message(logger, message);
          for (File f : paths) {
            message(logger, f.getAbsolutePath());
          }
        }
      }
    }, logger);
  }

  private void warning(final BuildProgressLogger logger, String message) {
    logger.warning(message);
  }

  private void message(final BuildProgressLogger logger, String message) {
    logger.message(message);
  }

  private void checkExistingPaths(Set<File> paths) {
    final Set<File> existingPaths = new HashSet<File>();
    for (File f : paths) {
      if (f.isFile() && isOutOfDate(f)) {
        existingPaths.add(f);
      } else if (f.isDirectory()) {
        final File[] files = f.listFiles();
        if ((files == null) || (files.length == 0)) return;
        for (File file : files) {
          if (file.isFile() && isOutOfDate(file)) {
            existingPaths.add(file);
          }
        }
      } else if (isAntMask(f)) {
        final MaskData md = getMask(f);
        for (File file : collectFiles(md.getPattern(), md.getBaseDir())) {
          if (isOutOfDate(file)) {
            existingPaths.add(file);
          }
        }
      }
    }
    if (existingPaths.size() > 0) {
      LOG.info("Found files from previous builds or build steps:");
      for (File f : existingPaths) {
        LOG.info(f.getAbsolutePath());
      }
    }
  }

  private boolean isOutOfDate(File k) {
    return k.lastModified() < myParameters.getBuildStartTime();
  }

  private MaskData getMask(File f) {
    MaskData md;
    if (!myMaskHash.containsKey(f)) {
      final File baseDir = new File(getDirWithoutPattern(f.getAbsolutePath()));
      final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(f.getAbsolutePath().replace(baseDir.getAbsolutePath(), "")));
      md = new MaskData(baseDir, pattern);
      myMaskHash.put(f, md);
    } else {
      md = myMaskHash.get(f);
    }
    return md;
  }

  private static String getDirWithoutPattern(String pathWithWildCard) {
    String t = pathWithWildCard.replace('\\', '/');
    final int firstStar = t.indexOf('*');
    final int firstQuest = t.indexOf('?');
    int mark = firstStar < 0 ? firstQuest :
      ((firstStar < firstQuest || firstQuest < 0) ? firstStar : firstQuest);

    final int lastSlash = t.lastIndexOf('/', mark);
    return lastSlash > 0 ? pathWithWildCard.substring(0, lastSlash) : "";
  }

  private static ArrayList<File> collectFiles(final Pattern pattern, final File basePatternDir) {
    final ArrayList<File> files = new ArrayList<File>();
    FileUtil.collectMatchedFiles(basePatternDir, pattern, files);
    return files;
  }

  public void pathsSkipped(@NotNull String type, @NotNull Set<File> paths) {
    if (!paths.isEmpty()) {
    // we use thread logger here, as pathsSkipped is called outside watcher thread
      logPathsInTarget(paths, type, "Skip watching:",  myParameters.getLogger().getThreadLogger());
    }
  }

  private void logPathsInTarget(final Collection<File> paths, String type, final String header, final BuildProgressLogger logger) {
    logInTarget(type, new Runnable() {
      public void run() {
        warning(logger, header);
        for (File f : paths) {
          warning(logger, f.getAbsolutePath());
        }
      }
    }, logger);
  }

  private void logInTarget(String type, Runnable activity, final BuildProgressLogger logger) {
    final String target = startTarget(type, logger);
    activity.run();
    logger.targetFinished(target);
  }

  private String startTarget(String type, final BuildProgressLogger logger) {
    final String target = SUPPORTED_REPORT_TYPES.get(type) + " report watcher";
    logger.targetStarted(target);
    return target;
  }

  @Override
  public void run() {
    final FlowLogger threadLogger = myParameters.getLogger().getThreadLogger();
    try {
      while (!myStopSignaled) {
        scanInput();
        Thread.sleep(SCAN_INTERVAL);
      }
      scanInput();
    } catch (Throwable th) {
      threadLogger.exception(th);
    } finally {
      threadLogger.disposeFlow();
    }
  }

  private void scanInput() throws Exception {
    for (final String type : myParameters.getTypes()) {
      final TypeStatistics s = getTypeStatistics(type);
      for (File f : myParameters.getPaths(type)) {
        if (isGoodFile(f, f) && !s.getFiles().contains(f)) {  // TODO complete duplicate of processFile()
          s.getFiles().add(f);
          sendToQueue(type, f, f);
        } else if (f.isDirectory()) {
          final File[] files = f.listFiles();
          final Set<File> filesInDir = new HashSet<File>();
          if ((files != null) && (files.length > 0)) {
            for (File file : files) {
              processFile(type, f, s, filesInDir, file);
            }
          }
          addToStatistics(s.getDirs(), f, filesInDir);
        } else if (isAntMask(f)) {
          final Set<File> filesForMask = new HashSet<File>();
          final MaskData md = getMask(f);
          for (File file : collectFiles(md.getPattern(), md.getBaseDir())) {
            processFile(type, f, s, filesForMask, file);
          }
          addToStatistics(s.getMasks(), f, filesForMask);
        }
      }
    }
  }

  private TypeStatistics getTypeStatistics(String type) {
    TypeStatistics s;
    if (myStatistics.containsKey(type)) {
      s = myStatistics.get(type);
    } else {
      s = new TypeStatistics();
      myStatistics.put(type, s);
    }
    return s;
  }

  private boolean isGoodFile(File f, File path) {
    return f.getName().endsWith(".xml") && f.isFile() && f.canRead() && timeConstraintsSatisfied(f, path) && !isExcluded(f);
  }

  private boolean timeConstraintsSatisfied(File file, File path) {
    return myParameters.getPathParameters(path).isParseOutOfDate() || !isOutOfDate(file);
  }

  private boolean isExcluded(File file) {
    final String path = file.getAbsolutePath();
    for (final String s : myParameters.getPathsToExclude()) {
      if (isAntMask(s)) {
       if (MATCHER.match(s, path)) {
         return true;
       }
      } else if (s.startsWith(path)) {
        return true;
      }
    }
    return false;
  }

  private void sendToQueue(String type, File f, File importRequestPath) throws InterruptedException {
    LOG.debug("Sending " + f.getAbsolutePath() + " to report queue");
    myReportQueue.put(new ReportData(f, type, importRequestPath));
  }

  private void processFile(String type, File path, TypeStatistics s, Set<File> files, File file) throws Exception {
    if (isGoodFile(file, path)) {
      if (!s.getFiles().contains(file)) {
        sendToQueue(type, file, path);
        s.getFiles().add(file);
      }
      files.add(file);
    }
  }

  private static void addToStatistics(Map<File, Set<File>> paths, File keyFile, Set<File> files) {
    if (paths.containsKey(keyFile)) {
      paths.get(keyFile).addAll(files);
    } else {
      paths.put(keyFile, files);
    }
  }

  public void signalStop() {
    myStopSignaled = true;
  }

  public void logTotals(@NotNull final BuildProgressLogger logger) {
    for (final String type : myParameters.getTypes()) {
      final Collection<File> paths = myParameters.getPaths(type);
      final TypeStatistics s = myStatistics.get(type);

      logInTarget(type, new Runnable() {
        public void run() {
          if (s.getFiles().size() > 0) {
            message(logger, s.getFiles().size() + " file(s) found");
          }
          for (File d : s.getDirs().keySet()) {
            logFiles(s, d, s.getDirs().get(d), logger);
            if (myParameters.isVerbose()) {
              for (File f : s.getDirs().get(d)) {
                message(logger, f.getAbsolutePath() + " found");
              }
            }
            paths.removeAll(s.getDirs().get(d));
            paths.remove(d);
          }
          for (File m : s.getMasks().keySet()) {
            logFiles(s, m, s.getMasks().get(m), logger);
            if (myParameters.isVerbose()) {
              for (File f : s.getMasks().get(m)) {
                message(logger, f.getAbsolutePath() + " found");
              }
            }
            paths.removeAll(s.getMasks().get(m));
            paths.remove(m);
          }
          for (File f : s.getFiles()) {
            if (myParameters.isVerbose()) {
              message(logger, f.getAbsolutePath() + " found");
            }
            paths.remove(f);
          }
          for (File f : paths) {
            logNoDataPublished(f, " couldn't find any matching files", logger);
          }
        }
      }, logger);
    }
  }

  private void logFiles(TypeStatistics s, File d, Set<File> files, final BuildProgressLogger logger) {
    if (files.size() > 0) {
      if (myParameters.isVerbose()) {
        message(logger, d.getAbsolutePath() + ": " + files.size() + " file(s) found");
      }
      s.getFiles().removeAll(files);
    } else {
      logNoDataPublished(d, ": no files found", logger);
    }
  }

  private void logNoDataPublished(File path, String suffix, final BuildProgressLogger logger) {
    final String message = path.getAbsolutePath() + suffix;
    myParameters.getPathParameters(path).getWhenNoDataPublished().doLogAction(message, logger);
  }

  private static class TypeStatistics {
    private final Set<File> myFiles;
    private final Map<File, Set<File>> myDirs;
    private final Map<File, Set<File>> myMasks;

    public TypeStatistics() {
      myFiles = new HashSet<File>();
      myDirs = new HashMap<File, Set<File>>();
      myMasks = new HashMap<File, Set<File>>();
    }

    public Set<File> getFiles() {
      return myFiles;
    }

    public Map<File, Set<File>> getDirs() {
      return myDirs;
    }

    public Map<File, Set<File>> getMasks() {
      return myMasks;
    }
  }

  private static class MaskData {
    private final File myBaseDir;
    private final Pattern myPattern;

    public MaskData(File baseDir, Pattern pattern) {
      myBaseDir = baseDir;
      myPattern = pattern;
    }

    public File getBaseDir() {
      return myBaseDir;
    }

    public Pattern getPattern() {
      return myPattern;
    }
  }
}