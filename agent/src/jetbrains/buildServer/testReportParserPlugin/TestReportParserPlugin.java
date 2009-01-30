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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.getTestReportDirs;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter {
  private TestReportDirectoryWatcher myDirectoryWatcher;
  private TestReportProcessor myReportProcessor;
  private TestReportLogger myLogger;
  private InspectionReporter myInspectionReporter;

  private TestReportParsingParameters myParameters;

  private volatile boolean myStopped;

  public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                                @NotNull final InspectionReporter inspectionReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
  }

  public void buildStarted(@NotNull AgentRunningBuild agentRunningBuild) {
    myStopped = false;
    myParameters = new TestReportParsingParameters(agentRunningBuild);
  }

  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    obtainLogger(build);
    if (!myParameters.isParsingEnabled()) {
      return;
    }

    final String dirProperty = getTestReportDirs(build.getRunnerParameters());
    final List<File> reportDirs = getReportDirsFromDirProperty(dirProperty, myParameters.getRunnerWorkingDir());

    if (reportDirs.size() == 0) {
      myLogger.warning("No report directories specified");
    }
    myLogger.debugToAgentLog("Plugin expects reports of type: " + myParameters.getReportType());

    startProcessing(reportDirs);
  }

  public void processReports(String reportType, List<File> reportDirs) {
    if (!myParameters.isParsingEnabled()) {
      myParameters.setParsingEnabled(true);
      myParameters.setReportType(reportType);
      startProcessing(reportDirs);
    } else {
      if (!myParameters.getReportType().equals(reportType)) {
        myLogger.error("Report type '" + reportType + "' is illegal");
      } else {
        myDirectoryWatcher.addDirectories(reportDirs);
      }
    }
  }

  private void startProcessing(List<File> reportDirs) {
    final LinkedBlockingQueue<File> reportsQueue = new LinkedBlockingQueue<File>();

    myDirectoryWatcher = new TestReportDirectoryWatcher(this, reportDirs, reportsQueue);
    myReportProcessor = new TestReportProcessor(this, reportsQueue, myDirectoryWatcher);

    myDirectoryWatcher.start();
    myReportProcessor.start();
  }

  private void obtainLogger(AgentRunningBuild agentRunningBuild) {
    final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
    if (logger instanceof BaseServerLoggerFacade) {
      myLogger = new TestReportLogger((BaseServerLoggerFacade) logger, myParameters.isVerboseOutput());
    } else {
      // not expected
    }
  }

  //dirs are not supposed to contain ';' in their path, as it is separator
  private static List<File> getReportDirsFromDirProperty(String dirProperty, final File workingDir) {
    if (dirProperty == null) {
      return Collections.emptyList();
    }

    final String separator = ";";
    final List<File> dirs = new ArrayList<File>();

    if (!dirProperty.endsWith(separator)) {
      dirProperty += separator;
    }

    int from = 0;
    int to = dirProperty.indexOf(separator);

    while (to != -1) {
      dirs.add(FileUtil.resolvePath(workingDir, dirProperty.substring(from, to)));
      from = to + 1;
      to = dirProperty.indexOf(separator, from);
    }
    return dirs;
  }

  public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
    myStopped = true;

    if (!myParameters.isParsingEnabled()) {
      return;
    }

    switch (buildFinishedStatus) {
      case INTERRUPTED:
        myLogger.warning("Build interrupted, plugin may not finish it's work");
      case FINISHED_SUCCESS:
      case FINISHED_FAILED:
        synchronized (myReportProcessor) {
          try {
            myReportProcessor.join();
          } catch (InterruptedException e) {
            myLogger.debugToAgentLog("Plugin thread interrupted");
          }
        }
        myDirectoryWatcher.logDirectoriesTotals();
        break;
    }
  }

  public void buildFinished(@NotNull final BuildFinishedStatus buildStatus) {
    myDirectoryWatcher = null;
    myReportProcessor = null;
    myLogger = null;
    myParameters = null;

    myStopped = true;
  }

  public TestReportLogger getLogger() {
    return myLogger;
  }

  public InspectionReporter getInspectionReporter() {
    return myInspectionReporter;
  }

  public TestReportParsingParameters getParameters() {
    return myParameters;
  }

  public boolean isStopped() {
    return myStopped;
  }
}