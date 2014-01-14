/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
  private final ReportStateHolder myReportStateHolder;

  @NotNull
  private final MonitorRulesListener myListener;

  private boolean myFirstRun;

  public MonitorRulesCommand(@NotNull MonitorRulesParameters parameters,
                             @NotNull ReportStateHolder reportStateHolder,
                             @NotNull MonitorRulesListener listener) {
    myParameters = parameters;
    myReportStateHolder = reportStateHolder;
    myListener = listener;

    myFirstRun = true;
  }

  public void run() {
    if (myFirstRun) {
      logWatchingPaths();
      myFirstRun = false;
    }

    monitorRules(
      new MonitorRulesFileProcessor() {
        public void processFile(@NotNull File file) {
          if (acceptFile(file)) {

            final long fileLastModified = file.lastModified();

            if (timeConstraintsSatisfied(file.lastModified())) {
              switch (myReportStateHolder.getReportState(file)) {
                case ON_PROCESSING:
                case PROCESSED:
                  return;
                case UNKNOWN:
                  myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.ON_PROCESSING, fileLastModified, file.length());
                  modificationDetected(file);
                  return;
                case ERROR:
                case OUT_OF_DATE:
                  final long fileLength = file.length();

                  final Long lastModified = myReportStateHolder.getLastModified(file);
                  final Long length = myReportStateHolder.getLength(file);

                  assert lastModified != null;
                  assert length != null;

                  if (fileLastModified > lastModified || fileLength > length) {
                    myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.ON_PROCESSING, file.lastModified(), file.length());
                    modificationDetected(file);
                  }
              }
            } else {
              myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.OUT_OF_DATE, fileLastModified, file.length());
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
    myListener.modificationDetected(file);
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
    return f.isFile() && f.canRead();
  }

  private boolean timeConstraintsSatisfied(long lastModified) {
    return myParameters.isParseOutOfDate() || isFresh(lastModified);
  }

  private boolean isFresh(long lastModified) {
    return lastModified >= myParameters.getStartTime();
  }
}
