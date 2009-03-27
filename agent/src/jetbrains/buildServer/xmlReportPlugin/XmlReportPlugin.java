/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;


public class XmlReportPlugin extends AgentLifeCycleAdapter {
  @NonNls
  private static final Collection<String> SILENT_PATHS = Arrays.asList("");
  public static final Logger LOGGER = Loggers.AGENT;

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
    myParameters.put(CHECKOUT_DIR, build.getCheckoutDirectory().getAbsolutePath());
    myParameters.put(TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
  }

  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    obtainLogger(build);
    if (!isParsingEnabled(myParameters)) {
      return;
    }
    final String pathsStr = getXmlReportPaths(myParameters);
    final List<File> reportPaths = getReportPathsFromDirProperty(pathsStr, myParameters.get(CHECKOUT_DIR));
    final String type = getReportType(myParameters);
    logWatchingPaths(reportPaths, type);
    //TODO: can avoid this if by adding paths presence in the web IU
    if (reportPaths.size() == 0) {
      enableXmlReportParsing(myParameters, ""); //can avoid this by adding paths presence in the web IU
    } else { //can avoid this by adding paths presence in the web IU
      startProcessing(reportPaths, type);
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

  public void processReports(Map<String, String> params, List<File> reportPaths) {
    final boolean wasParsingEnabled = isParsingEnabled(myParameters);
    final String type = getReportType(params);
    myParameters.putAll(params);
    logWatchingPaths(reportPaths, type);
    if (!wasParsingEnabled) {
      startProcessing(reportPaths, type);
    } else {
      myDirectoryWatcher.addParams(reportPaths, type);
    }
  }

  private void startProcessing(List<File> reportDirs, String type) {
    final LinkedBlockingQueue<Pair<String, File>> reportsQueue = new LinkedBlockingQueue<Pair<String, File>>();

    myDirectoryWatcher = new XmlReportDirectoryWatcher(this, reportDirs, type, reportsQueue);
    myReportProcessor = new XmlReportProcessor(this, reportsQueue, myDirectoryWatcher);

    myDirectoryWatcher.start();
    myReportProcessor.start();
  }

  private void logWatchingPaths(List<File> paths, String type) {
    if (!SUPPORTED_REPORT_TYPES.containsKey(type)) {
      error("Illegal report type: " + type);
      return;
    }
    final String target = SUPPORTED_REPORT_TYPES.get(type) + " report watcher";
    myLogger.targetStarted(target);
    String message = "Watching paths: ";
    if (paths.size() == 0) {
      message += "<no paths>";
      error(message);
    } else {
      message(message);
      for (File f : paths) {
        message(f.getAbsolutePath());
      }
    }
    myLogger.targetFinished(target);
  }

  private static List<File> getReportPathsFromDirProperty(String pathsStr, String checkoutDir) {
    final List<File> dirs = new ArrayList<File>();
    if (pathsStr != null) {
      final String[] paths = pathsStr.split(" *[,\n\r] *");
      for (int i = 0; i < paths.length; ++i) {
        dirs.add(FileUtil.resolvePath(new File(checkoutDir), paths[i]));
      }
    }
    dirs.removeAll(SILENT_PATHS);
    return dirs;
  }

  private void error(String message) {
    myLogger.error(message);
    LOGGER.debug(message);
  }

  private void message(String message) {
    myLogger.message(message);
    LOGGER.debug(message);
  }

  public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
    myStopped = true;
    if (isParsingEnabled(myParameters)) {
      synchronized (myReportProcessor) {
        try {
          myReportProcessor.join();
        } catch (InterruptedException e) {
        }
      }
      myDirectoryWatcher.logTotals();
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

  public String getCheckoutDir() {
    return myParameters.get(CHECKOUT_DIR);
  }

  public boolean parseOutOfDate() {
    return Boolean.parseBoolean(myParameters.get(PARSE_OUT_OF_DATE));
  }

  public boolean isVerbose() {
    return Boolean.parseBoolean(myParameters.get(VERBOSE_OUTPUT));
  }

  public Map<String, String> getParameters() {
    return myParameters;
  }
}