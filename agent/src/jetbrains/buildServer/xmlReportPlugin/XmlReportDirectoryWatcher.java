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

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.AntPathMatcher;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin.LOG;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.SUPPORTED_REPORT_TYPES;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.isInspection;


public class XmlReportDirectoryWatcher extends Thread {
  private static final int SCAN_INTERVAL = 100;

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private final Parameters myParameters;

  private final LinkedBlockingQueue<ReportData> myReportQueue;
  private final Map<String, Set<File>> myPaths;
  private final Map<File, MaskData> myMaskHash;
  private final Map<String, TypeStatistics> myStatistics;
  private final List<String> myPathsToExclude;

  private volatile boolean myStopSignaled;

  public interface Parameters {
    boolean isVerbose();
    @NotNull BuildProgressLogger getLogger();
    boolean parseOutOfDate(@NotNull File path);
    @NotNull List<String> getPathsToExclude();
    long getBuildStartTime();
    @NotNull
    String getWhenNoDataPublished(@NotNull File path);
  }


  public XmlReportDirectoryWatcher(@NotNull final Parameters parameters,
                                   @NotNull final Set<File> input,
                                   @NotNull final String type,
                                   @NotNull final LinkedBlockingQueue<ReportData> queue) {
    super("xml-report-plugin-DirectoryWatcher");

    myParameters = parameters;
    myPaths = new HashMap<String, Set<File>>();
    myReportQueue = queue;
    myMaskHash = new HashMap<File, MaskData>();
    myStatistics = new HashMap<String, TypeStatistics>();
    myPathsToExclude = myParameters.getPathsToExclude();
    addPathInt(input, type, myParameters.getLogger());
  }

  private static boolean isAntMask(File f) {
    return isAntMask(f.getAbsolutePath());
  }

  private static boolean isAntMask(String s) {
    return MATCHER.isPattern(s);
  }

  @Override
  public void run() {
    FlowLogger threadLogger = myParameters.getLogger().getThreadLogger();
    try {
      while (!myStopSignaled) {
        try {
          scanInput(threadLogger);
          Thread.sleep(SCAN_INTERVAL);
        } catch (Throwable e) {
          threadLogger.exception(e);
        }
      }
      scanInput(threadLogger);
    } finally {
      threadLogger.disposeFlow();
    }
  }

  public void signalStop() {
    myStopSignaled = true;
  }

  private void message(final BuildProgressLogger logger, String message) {
    logger.message(message);
    LOG.debug(message);
  }

  private void warning(final BuildProgressLogger logger, String message) {
    logger.warning(message);
    LOG.debug(message);
  }

  private void error(final BuildProgressLogger logger, String message) {
    logger.error(message);
    LOG.debug(message);
  }

  public void addPaths(final Collection<File> paths, final String type) {
    // TODO since this method is called from different threads a thread logger is obtained.
    // Don't use myScanThreadLogger here. It's supposed for use only from watcher thread.
    addPathInt(paths, type, myParameters.getLogger().getThreadLogger());
  }

  private void addPathInt(final Collection<File> paths, final String type, final BuildProgressLogger logger) {
    Set<File> newPaths = new HashSet<File>(paths);
    synchronized (myPaths) {
      if (!myPaths.containsKey(type)) {
        if (isInspection(type) && hasInspections()) {
          warning(logger,
                  "Two different inspections can not be processed during one build, skip " + SUPPORTED_REPORT_TYPES.get(type) + " reports"
          );
          if (newPaths.size() > 0) {
            logPathsInTarget(newPaths, type, "Skip watching:", logger);
          }
          return;
        }
        myStatistics.put(type, new TypeStatistics());
        myPaths.put(type, newPaths);
      } else {
        newPaths.removeAll(myPaths.get(type));
        myPaths.get(type).addAll(newPaths);
      }
      logWatchingPaths(newPaths, type, logger);
      checkExistingPaths(newPaths, type);
    }
  }

  private void logInTarget(String type, Runnable activity, final BuildProgressLogger logger) {
    final String target = startTarget(type, logger);
    activity.run();
    logger.targetFinished(target);
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

  private boolean hasInspections() {
    for (String type : myPaths.keySet()) {
      if (isInspection(type)) {
        return true;
      }
    }
    return false;
  }

  private void logWatchingPaths(final Set<File> paths, String type, final BuildProgressLogger logger) {
    if (!SUPPORTED_REPORT_TYPES.containsKey(type)) {
      error(logger, "Illegal report type: " + type);
      return;
    }
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

  private void checkExistingPaths(Set<File> paths, String type) {
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

  private void scanInput(final BuildProgressLogger logger) {
    synchronized (myPaths) { // TODO very ineffective synchronization - needs refactoring
      for (Map.Entry<String, Set<File>> entry : myPaths.entrySet()) {
        final String type = entry.getKey();

        final TypeStatistics s = myStatistics.get(type);
        for (File f : entry.getValue()) {
          if (isGoodFile(f, f) && !s.getFiles().contains(f)) {  // TODO complete duplicate of processFile()
            s.getFiles().add(f);
            sendToQueue(type, f, f, logger);
          } else if (f.isDirectory()) {
            final File[] files = f.listFiles();
            final Set<File> filesInDir = new HashSet<File>();
            if ((files != null) && (files.length > 0)) {
              for (File file : files) {
                processFile(type, f, s, filesInDir, file, logger);
              }
            }
            addToStatistics(s.getDirs(), f, filesInDir);
          } else if (isAntMask(f)) {
            final Set<File> filesForMask = new HashSet<File>();
            final MaskData md = getMask(f);
            for (File file : collectFiles(md.getPattern(), md.getBaseDir())) {
              processFile(type, f, s, filesForMask, file, logger);
            }
            addToStatistics(s.getMasks(), f, filesForMask);
          }
        }
      }
    }
  }

  private boolean isGoodFile(File f, File path) {
    return f.getName().endsWith(".xml") && f.isFile() && f.canRead() && timeConstraintsSatisfied(f, path) && !isExcluded(f);
  }

  private void processFile(String type, File path, TypeStatistics s, Set<File> files, File file, final BuildProgressLogger logger) {
    if (isGoodFile(file, path)) {
      if (!s.getFiles().contains(file)) {
        sendToQueue(type, file, path, logger);
        s.getFiles().add(file);
      }
      files.add(file);
    }
  }

  private void sendToQueue(String type, File f, File importRequestPath, final BuildProgressLogger logger) {
    LOG.debug("Sending " + f.getAbsolutePath() + " to report queue");
    try {
      myReportQueue.put(new ReportData(f, type, importRequestPath));
    } catch (InterruptedException e) {
      logger.exception(e);
      LOG.warn(e.toString(), e);
    }
  }

  private static void addToStatistics(Map<File, Set<File>> paths, File keyFile, Set<File> files) {
    if (paths.containsKey(keyFile)) {
      paths.get(keyFile).addAll(files);
    } else {
      paths.put(keyFile, files);
    }
  }

  private boolean timeConstraintsSatisfied(File file, File path) {
    return myParameters.parseOutOfDate(path) || !isOutOfDate(file);
  }

  private boolean isExcluded(File file) {
    final String path = file.getAbsolutePath();
    for (final String s : myPathsToExclude) {
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

  private boolean isOutOfDate(File k) {
    return k.lastModified() < myParameters.getBuildStartTime();
  }

  public void logTotals(@NotNull final BuildProgressLogger logger) {
    synchronized (myPaths) {
      for (final String type : myPaths.keySet()) {
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
              myPaths.get(type).removeAll(s.getDirs().get(d));
              myPaths.get(type).remove(d);
            }
            for (File m : s.getMasks().keySet()) {
              logFiles(s, m, s.getMasks().get(m), logger);
              if (myParameters.isVerbose()) {
                for (File f : s.getMasks().get(m)) {
                  message(logger, f.getAbsolutePath() + " found");
                }
              }
              myPaths.get(type).removeAll(s.getMasks().get(m));
              myPaths.get(type).remove(m);
            }
            for (File f : s.getFiles()) {
              if (myParameters.isVerbose()) {
                message(logger, f.getAbsolutePath() + " found");
              }
              myPaths.get(type).remove(f);
            }
            for (File f : myPaths.get(type)) {
              logNoDataPublished(f, " couldn't find any matching files", logger);
            }
          }
        }, logger);
      }
    }
  }

  private void logNoDataPublished(File path, String suffix, final BuildProgressLogger logger) {
    final String whenNoDataPublished = myParameters.getWhenNoDataPublished(path);
    final String message = path.getAbsolutePath() + suffix;

    if ("nothing".equals(whenNoDataPublished)) return;

    if ("warning".equals(whenNoDataPublished) || "warn".equals(whenNoDataPublished)) {
      warning(logger, message);
      return;
    }

    error(logger, message);
  }

  private String startTarget(String type, final BuildProgressLogger logger) {
    final String target = SUPPORTED_REPORT_TYPES.get(type) + " report watcher";
    logger.targetStarted(target);
    return target;
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

  private static class TypeStatistics {
    private final Set<File> myFiles;
    private final Map<File, Set<File>> myDirs;
    private final Map<File, Set<File>> myMasks;

    public TypeStatistics() {
      myFiles = new HashSet<File>();
      myDirs = new HashMap<File, Set<File>>();
      myMasks = new HashMap<File, Set<File>>();
    }

    public TypeStatistics(@NotNull TypeStatistics copyOf) {
      myFiles = new HashSet<File>(copyOf.myFiles);
      myDirs = new HashMap<File, Set<File>>();
      myMasks = new HashMap<File, Set<File>>();

      for(Map.Entry<File, Set<File>> entry : copyOf.myDirs.entrySet())
        myDirs.put(entry.getKey(), new HashSet<File>(entry.getValue()));

      for(Map.Entry<File, Set<File>> entry : copyOf.myMasks.entrySet())
        myMasks.put(entry.getKey(), new HashSet<File>(entry.getValue()));
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