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

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.*;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter {
  private TestReportDirectoryWatcher myDirectoryWatcher;
  private TestReportProcessor myReportProcessor;
  private TestReportLogger myLogger;
  private InspectionReporter myInspectionReporter;

  private Map<String, String> myParameters;

  private volatile boolean myStopped;

  public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                                @NotNull final InspectionReporter inspectionReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
  }

  public void buildStarted(@NotNull AgentRunningBuild build) {
    myStopped = false;
    myParameters = new HashMap<String, String>(build.getRunnerParameters());
    myParameters.put(TEST_REPORT_PARSING_BUILD_START, "" + new Date().getTime());
    myParameters.put(TEST_REPORT_PARSING_WORKING_DIR, build.getWorkingDirectory().getAbsolutePath());
    myParameters.put(TEST_REPORT_PARSING_TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
  }

  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    obtainLogger(build);
    if (!isParsingEnabled(myParameters)) {
      return;
    }

    final String dirProperty = getTestReportDirs(myParameters);
    final List<File> reportDirs = getReportDirsFromDirProperty(dirProperty, myParameters.get(TEST_REPORT_PARSING_WORKING_DIR));

    if (reportDirs.size() == 0) {
      myLogger.warning("No report directories specified");
    }
    startProcessing(reportDirs, getReportType(myParameters));
  }

  private void obtainLogger(AgentRunningBuild agentRunningBuild) {
    final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
    if (logger instanceof BaseServerLoggerFacade) {
      myLogger = new TestReportLogger((BaseServerLoggerFacade) logger, isOutputVerbose(myParameters));
    } else {
      // not expected
    }
  }

  public void processReports(Map<String, String> params, List<File> reportDirs) {
    myLogger.setVerboseOutput(Boolean.parseBoolean(params.get(TEST_REPORT_PARSING_VERBOSE_OUTPUT)));
    final boolean wasParsingEnabled = isParsingEnabled(myParameters);
    final String type = getReportType(params);
    myParameters.putAll(params);
    if (!wasParsingEnabled) {
      startProcessing(reportDirs, type);
    } else {
      myDirectoryWatcher.addDirectories(reportDirs, type);
    }
  }

  private void startProcessing(List<File> reportDirs, String type) {
    final LinkedBlockingQueue<Pair<String, File>> reportsQueue = new LinkedBlockingQueue<Pair<String, File>>();

    myDirectoryWatcher = new TestReportDirectoryWatcher(this, reportDirs, type, reportsQueue);
    myReportProcessor = new TestReportProcessor(this, reportsQueue, myDirectoryWatcher);

    myDirectoryWatcher.start();
    myReportProcessor.start();
  }

  //dirs are not supposed to contain ';' in their path, as it is separator
  private static List<File> getReportDirsFromDirProperty(String dirProperty, String workingDir) {
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
      dirs.add(FileUtil.resolvePath(new File(workingDir), dirProperty.substring(from, to)));
      from = to + 1;
      to = dirProperty.indexOf(separator, from);
    }
    return dirs;
  }

  public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
    myStopped = true;

    if (!isParsingEnabled(myParameters)) {
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

  public boolean isStopped() {
    return myStopped;
  }

  public long getBuildStartTime() {
    return Long.parseLong(myParameters.get(TEST_REPORT_PARSING_BUILD_START));
  }

  public String getCurrentReportType() {
    return getReportType(myParameters);
  }

  public String getTmpDir() {
    return myParameters.get(TEST_REPORT_PARSING_TMP_DIR);
  }

  public String getWorkingDir() {
    return myParameters.get(TEST_REPORT_PARSING_WORKING_DIR);
  }

  public boolean parseOutOfDate() {
    return Boolean.parseBoolean(myParameters.get(TEST_REPORT_PARSING_PARSE_OUT_OF_DATE));
  }

  public Map<String, String> getParameters() {
    return myParameters;
  }
}