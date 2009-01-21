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
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.*;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter implements DataProcessor {
  private static final String DATA_PROCESSOR_ID = "XmlReport";
  private static final String DATA_PROCESSOR_VERBOSE_ARGUMENT = "verbose";
  private static final String DATA_PROCESSOR_REPORT_TYPE_ARGUMENT = "reportType";

  private TestReportDirectoryWatcher myDirectoryWatcher;
  private TestReportProcessor myReportProcessor;
  private TestReportLogger myLogger;

  private boolean myTestReportParsingEnabled = false;
  private boolean myVerboseOutput = false;
  private String myReportType = "";
  private long myBuildStartTime;
  private File myRunnerWorkingDir;
  private File myTmpDir;

  private volatile boolean myStopped;

  public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
    agentDispatcher.addListener(this);
  }

  public void buildStarted(@NotNull AgentRunningBuild agentRunningBuild) {
    myStopped = false;
    myBuildStartTime = new Date().getTime();
  }

  public void beforeRunnerStart(@NotNull AgentRunningBuild agentRunningBuild) {
    myRunnerWorkingDir = agentRunningBuild.getWorkingDirectory();
    myTmpDir = agentRunningBuild.getBuildTempDirectory();

    final Map<String, String> runnerParameters = agentRunningBuild.getRunnerParameters();

    myTestReportParsingEnabled = isTestReportParsingEnabled(runnerParameters);
    myVerboseOutput = isOutputVerbose(runnerParameters);

    obtainLogger(agentRunningBuild);

    if (!myTestReportParsingEnabled) {
      return;
    }

    final String dirProperty = getTestReportDirs(runnerParameters);
    final List<File> reportDirs = getReportDirsFromDirProperty(dirProperty, myRunnerWorkingDir);

    if (reportDirs.size() == 0) {
      myLogger.warning("No report directories specified");
    }

    myReportType = getReportType(runnerParameters);
    myLogger.debugToAgentLog("Plugin expects reports of type: " + myReportType);

    startReportProcessing(reportDirs);
  }

  private void startReportProcessing(List<File> reportDirs) {
    final LinkedBlockingQueue<File> reportsQueue = new LinkedBlockingQueue<File>();
    myDirectoryWatcher = new TestReportDirectoryWatcher(this, reportDirs, reportsQueue);
    myReportProcessor = new TestReportProcessor(this, reportsQueue, myDirectoryWatcher);

    myDirectoryWatcher.start();
    myReportProcessor.start();
  }

  private void obtainLogger(AgentRunningBuild agentRunningBuild) {
    final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
    if (logger instanceof BaseServerLoggerFacade) {
      myLogger = new TestReportLogger((BaseServerLoggerFacade) logger, myVerboseOutput);
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

    if (!myTestReportParsingEnabled) {
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

  public TestReportLogger getLogger() {
    return myLogger;
  }

  public long getBuildStartTime() {
    return myBuildStartTime;
  }

  public File getRunnerWorkingDir() {
    return myRunnerWorkingDir;
  }

  public File getTmpDir() {
    return myTmpDir;
  }

  public String getSelectedReportType() {
    return myReportType;
  }

  public boolean isStopped() {
    return myStopped;
  }

  //"##teamcity[importData id='XmlReport' reportType='sometype' file='somedir']"
  // service messsage activates watching "somedir" directory for reports of sometype type
  //"##teamcity[importData id='XmlReport' reportType='sometype' file='somedir' verbose='true']"
  // does the same and sets output verbose
  public void processData(@NotNull File file, Map<String, String> arguments) throws Exception {
    final String reportType = arguments.get(DATA_PROCESSOR_REPORT_TYPE_ARGUMENT);
    if (!SUPPORTED_REPORT_TYPES.containsKey(reportType)) {
      myLogger.error("Wrong report type specified in service message arguments: " + reportType);
      return;
    }

    if (arguments.containsKey(DATA_PROCESSOR_VERBOSE_ARGUMENT)) {
      myVerboseOutput = Boolean.parseBoolean(arguments.get(DATA_PROCESSOR_VERBOSE_ARGUMENT));
    } else {
      myVerboseOutput = false;
    }
    myLogger.setVerboseOutput(myVerboseOutput);

    final List<File> reportDirs = new ArrayList<File>();
    reportDirs.add(file);

    if (!myTestReportParsingEnabled) {
      myTestReportParsingEnabled = true;
      myReportType = reportType;
      startReportProcessing(reportDirs);
    } else {
      if (!myReportType.equals(reportType)) {
        myLogger.error("Report type '" + reportType + "' specified in service message arguments is illegal");
        return;
      }
      myDirectoryWatcher.addDirectories(reportDirs);
    }
  }

  @NotNull
  public String getType() {
    return DATA_PROCESSOR_ID;
  }

  @NotNull
  public String getId() {
    return DATA_PROCESSOR_ID;
  }
}