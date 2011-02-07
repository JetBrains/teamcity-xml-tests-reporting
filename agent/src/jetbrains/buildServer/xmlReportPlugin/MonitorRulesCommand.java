/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.AntPathMatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:17
 */
public class MonitorRulesCommand {
  public static interface MonitorRulesParameters {
    @NotNull Rules getRules();

    @NotNull String getType();

    boolean isParseOutOfDate();

    long getStartTime();

    @NotNull BuildProgressLogger getThreadLogger();
  }

  public static interface MonitorRulesListener {
    void modificationDetected(@NotNull File file);
  }

  @NotNull
  private final MonitorRulesParameters myParameters;

  @NotNull
  private final FilesState myFilesState;

  @NotNull
  private final MonitorRulesListener myListener;

  private boolean myFirstRun;

  @NotNull
  private final Map<File, MaskData> myMasks = new HashMap<File, MaskData>();

  @NotNull
  private final Map<File, FileState> myFileStates = new HashMap<File, FileState>();

  public MonitorRulesCommand(@NotNull MonitorRulesParameters parameters,
                             @NotNull FilesState filesState,
                             @NotNull MonitorRulesListener listener) {
    myParameters = parameters;
    myFilesState = filesState;
    myListener = listener;

    myFirstRun = true;
  }

  public void run() {
    if (myFirstRun) {
      logWatchingPaths();
      checkExistingPaths();
      myFirstRun = false;
    }

    monitorRules(
      new MonitorRulesFileProcessor() {
        public void processFile(@NotNull File file) {
          if (acceptFile(file)) { //TODO: also grows
            switch (myFilesState.getFileState(file)) {
              case ON_PROCESSING:
                return;
              case PROCESSED:
                if (myFileStates.containsKey(file)) {
                  myFileStates.remove(file);
                }
                return;
              case UNKNOWN:
                final long lastModified = file.lastModified();
                final long length = file.length();

                final FileState fileState = myFileStates.get(file);

                if (fileState == null) {
                  myFileStates.put(file, new FileState(lastModified, length));

                  modificationDetected(file);
                } else {
                  if (lastModified > fileState.lastModified || length > fileState.length) {
                    fileState.lastModified = lastModified;
                    fileState.length = length;

                    modificationDetected(file);
                  }
                }
            }
          }
        }
      }
    );
  }

  public void logWatchingPaths() {
    LoggingUtils.logInTarget(LoggingUtils.getTypeDisplayName(myParameters.getType()) + " report watcher",
      new Runnable() {
        public void run() {
          String message = "Watching paths:";
          final List<String> rulesList = myParameters.getRules().getBody();
          if (rulesList.isEmpty()) {
            message += " <no paths>";
            LoggingUtils.warn(message, getThreadLogger());
          } else {
            LoggingUtils.message(message, getThreadLogger());
            for (String r : rulesList) {
              LoggingUtils.message(r, getThreadLogger());
            }
          }
        }
      }, getThreadLogger());
  }

  @NotNull
  private BuildProgressLogger getThreadLogger() {
    return myParameters.getThreadLogger();
  }

  private void modificationDetected(File file) {
    myFilesState.addFile(file);
    myListener.modificationDetected(file);
  }

  private void checkExistingPaths() {
    final List<File> existingPaths = new ArrayList<File>();

    monitorRules(
      new MonitorRulesFileProcessor() {
        public void processFile(@NotNull File file) {
          if (file.isFile() && file.canRead() &&
            !isFresh(file) &&
            isIncluded(file)) {
            existingPaths.add(file);
          }
        }
      });

    if (existingPaths.size() > 0) {
      LoggingUtils.LOG.info("Found " + existingPaths.size() + " files from previous builds or build steps:");
      for (File f : existingPaths) {
        LoggingUtils.LOG.info(f.getPath());
      }
    }
  }

  private interface MonitorRulesFileProcessor {
    void processFile(@NotNull File file);
  }

  private void monitorRules(@NotNull MonitorRulesFileProcessor monitorRulesFileProcessor) {
    for (File rule : myParameters.getRules().getPaths()) {
      if (rule.isFile()) {
        monitorRulesFileProcessor.processFile(rule);
        continue;
      }
      if (rule.isDirectory()) {
        final File[] files = rule.listFiles();
        if ((files != null) && (files.length > 0)) {
          for (File file : files) {
            monitorRulesFileProcessor.processFile(file);
          }
        }
        continue;
      }
      if (isAntMask(rule)) {
        final MaskData md = getMask(rule);
        for (File file : collectFiles(md.getPattern(), md.getBaseDir())) {
          monitorRulesFileProcessor.processFile(file);
        }
      }
    }
  }

  private boolean acceptFile(@NotNull File f) {
    return f.isFile() && f.canRead() &&
           timeConstraintsSatisfied(f) &&
           isIncluded(f);
  }

  private boolean timeConstraintsSatisfied(@NotNull File file) {
    return myParameters.isParseOutOfDate() || isFresh(file);
  }

  private boolean isFresh(@NotNull File file) {
    return file.lastModified() >= myParameters.getStartTime();
  }

  private boolean isIncluded(@NotNull File file) {
    return myParameters.getRules().shouldInclude(file);
  }

  private MaskData getMask(@NotNull File file) {
    MaskData md;
    if (!myMasks.containsKey(file)) {
      final File baseDir = new File(getDirWithoutPattern(file.getPath()));
      final Pattern pattern = Pattern.compile(FileUtil.convertAntToRegexp(file.getPath().replace(baseDir.getPath(), "")));
      md = new MaskData(baseDir, pattern);
      myMasks.put(file, md);
    } else {
      md = myMasks.get(file);
    }
    return md;
  }

  private static String getDirWithoutPattern(@NotNull String pathWithWildCard) {
    String t = pathWithWildCard.replace('\\', '/');
    final int firstStar = t.indexOf('*');
    final int firstQuest = t.indexOf('?');
    int mark = firstStar < 0 ? firstQuest :
      ((firstStar < firstQuest || firstQuest < 0) ? firstStar : firstQuest);

    final int lastSlash = t.lastIndexOf('/', mark);
    return lastSlash > 0 ? pathWithWildCard.substring(0, lastSlash) : "";
  }

  private static ArrayList<File> collectFiles(@NotNull Pattern pattern, @NotNull File basePatternDir) {
    final ArrayList<File> files = new ArrayList<File>();
    FileUtil.collectMatchedFiles(basePatternDir, pattern, files);
    return files;
  }

  private static final AntPathMatcher MATCHER = new AntPathMatcher();

  private static boolean isAntMask(@NotNull File f) {
    return isAntMask(f.getPath());
  }

  private static boolean isAntMask(@NotNull String s) {
    return MATCHER.isPattern(s);
  }

  private static class MaskData {
    @NotNull
    private final File myBaseDir;
    @NotNull
    private final Pattern myPattern;

    public MaskData(@NotNull File baseDir, @NotNull Pattern pattern) {
      myBaseDir = baseDir;
      myPattern = pattern;
    }

    @NotNull
    public File getBaseDir() {
      return myBaseDir;
    }

    @NotNull
    public Pattern getPattern() {
      return myPattern;
    }
  }

  private static class FileState {
    private long lastModified;
    private long length;

    private FileState(long lastModified, long length) {
      this.lastModified = lastModified;
      this.length = length;
    }
  }
}
