/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;

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
  private final FileStates myFileStates;

  @NotNull
  private final MonitorRulesListener myListener;

  private boolean myFirstRun;

  @NotNull
  private final Map<File, FileState> myKnownStates = new HashMap<File, FileState>();

  public MonitorRulesCommand(@NotNull MonitorRulesParameters parameters,
                             @NotNull FileStates fileStates,
                             @NotNull MonitorRulesListener listener) {
    myParameters = parameters;
    myFileStates = fileStates;
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
            switch (myFileStates.getFileState(file)) {
              case ON_PROCESSING:
                return;
              case PROCESSED:
                if (myKnownStates.containsKey(file)) {
                  myKnownStates.remove(file);
                }
                return;
              case UNKNOWN:
                final long lastModified = file.lastModified();
                final long length = file.length();

                final FileState fileState = myKnownStates.get(file);

                if (fileState == null) {
                  myKnownStates.put(file, new FileState(lastModified, length));

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
          final Collection<String> rulesList = myParameters.getRules().getBody();
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
    myFileStates.addFile(file);
    myListener.modificationDetected(file);
  }

  private void checkExistingPaths() {
    final List<File> existingPaths = new ArrayList<File>();

    monitorRules(
      new MonitorRulesFileProcessor() {
        public void processFile(@NotNull File file) {
          if (file.isFile() && file.canRead() &&
            !isFresh(file)) {
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
    for (File file : myParameters.getRules().collectFiles()) {
      monitorRulesFileProcessor.processFile(file);
    }
  }

  private boolean acceptFile(@NotNull File f) {
    return f.isFile() && f.canRead() &&
           timeConstraintsSatisfied(f);
  }

  private boolean timeConstraintsSatisfied(@NotNull File file) {
    return myParameters.isParseOutOfDate() || isFresh(file);
  }

  private boolean isFresh(@NotNull File file) {
    return file.lastModified() >= myParameters.getStartTime();
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
