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
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class XmlReportPlugin extends AgentLifeCycleAdapter {
  private XmlReportDirectoryWatcher myDirectoryWatcher;
  private XmlReportProcessor myReportProcessor;
  private BaseServerLoggerFacade myLogger;
  private InspectionReporter myInspectionReporter;

  private Map<String, String> myParameters;

  private volatile boolean myStopped;

  public XmlReportPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull final InspectionReporter inspectionReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
  }

  public void buildStarted(@NotNull AgentRunningBuild build) {
    myStopped = false;
    myParameters = new HashMap<String, String>(build.getRunnerParameters());
    myParameters.put(BUILD_START, "" + new Date().getTime());
    myParameters.put(WORKING_DIR, build.getWorkingDirectory().getAbsolutePath());
    myParameters.put(TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
  }

  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    obtainLogger(build);
    if (!isParsingEnabled(myParameters)) {
      return;
    }

    final String dirProperty = getXmlReportDirs(myParameters);
    final List<File> reportDirs = getReportDirsFromDirProperty(dirProperty, myParameters.get(WORKING_DIR));

    if (reportDirs.size() == 0) {
      myLogger.warning("No report directories specified");
      enableXmlReportParsing(myParameters, ""); //can avoid this by adding paths presence in the web IU
    } else { //can avoid this by adding paths presence in the web IU
      startProcessing(reportDirs, getReportType(myParameters));
    }
  }

  private void obtainLogger(AgentRunningBuild agentRunningBuild) {
    final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
    if (logger instanceof BaseServerLoggerFacade) {
      myLogger = (BaseServerLoggerFacade) logger;
    } else {
      // not expected
    }
  }

  public void processReports(Map<String, String> params, List<File> reportDirs) {
//    myLogger.setVerboseOutput(Boolean.parseBoolean(params.get(VERBOSE_OUTPUT)));
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

    myDirectoryWatcher = new XmlReportDirectoryWatcher(this, reportDirs, type, reportsQueue);
    myReportProcessor = new XmlReportProcessor(this, reportsQueue, myDirectoryWatcher);

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
//            myLogger.debugToAgentLog("Plugin thread interrupted");
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

  public BaseServerLoggerFacade getLogger() {
    return myLogger;
  }

  public InspectionReporter getInspectionReporter() {
    return myInspectionReporter;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public long getBuildStartTime() {
    return Long.parseLong(myParameters.get(BUILD_START));
  }

  public String getCurrentReportType() {
    return getReportType(myParameters);
  }

  public String getTmpDir() {
    return myParameters.get(TMP_DIR);
  }

  public String getWorkingDir() {
    return myParameters.get(WORKING_DIR);
  }

  public boolean parseOutOfDate() {
    return Boolean.parseBoolean(myParameters.get(PARSE_OUT_OF_DATE));
  }

  public Map<String, String> getParameters() {
    return myParameters;
  }
}