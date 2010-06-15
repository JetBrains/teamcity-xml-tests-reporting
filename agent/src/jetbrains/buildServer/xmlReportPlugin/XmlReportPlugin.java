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
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporterListener;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements InspectionReporterListener {
  @NonNls
  private static final Collection<String> SILENT_PATHS = Arrays.asList("");
  public static final Logger LOG = Logger.getLogger(XmlReportPlugin.class);

  public static final String CHECKOUT_DIR = "teamcity.build.checkoutDir";

  public static final String TREAT_DLL_AS_SUITE = "xmlReportParsing.nunit.treatDllAsRootSuite";

  private XmlReportDirectoryWatcher myDirectoryWatcher;
  private XmlReportProcessor myReportProcessor;
  private BaseServerLoggerFacade myLogger;
  private final InspectionReporter myInspectionReporter;

  private volatile Map<String, String> myParameters;

  private volatile boolean myStopped;

  public XmlReportPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull final InspectionReporter inspectionReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
    myInspectionReporter.addListener(this);
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild build) {
    myStopped = false;
    myParameters = new HashMap<String, String>(build.getRunnerParameters());
    myParameters.put(BUILD_START, "" + new Date().getTime());
    myParameters.put(TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
    myParameters.put(TREAT_DLL_AS_SUITE, build.getBuildParameters().getSystemProperties().get(TREAT_DLL_AS_SUITE));
  }

  @Override
  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    obtainLogger(build);
    if (!isParsingEnabled(myParameters)) {
      return;
    }
    final Set<File> reportPaths = getReportPathsFromDirProperty(getXmlReportPaths(myParameters), build.getCheckoutDirectory());
    final String type = getReportType(myParameters);
    //TODO: can avoid this if by adding paths presence in the web IU
    if (reportPaths.size() == 0) {
      enableXmlReportParsing(myParameters, ""); //can avoid this by adding paths presence in the web IU
    }
    startProcessing(reportPaths, type);
  }

  private void obtainLogger(AgentRunningBuild agentRunningBuild) {
    final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
    if (logger instanceof BaseServerLoggerFacade) {
      myLogger = (BaseServerLoggerFacade) logger;
    } else {
      // not expected
    }
  }

  public synchronized void processReports(Map<String, String> params, Set<File> reportPaths) {
    final boolean wasParsingEnabled = isParsingEnabled(myParameters);
    final String type = getReportType(params);
    myParameters.putAll(params);
    if (!wasParsingEnabled) {
      startProcessing(reportPaths, type);
    } else {
      myDirectoryWatcher.addPaths(reportPaths, type);
    }
  }

  private class Parameters implements XmlReportDirectoryWatcher.Parameters, XmlReportProcessor.Parameters {
    private final String myCheckoutDir;
    private final String myFindBugsHome;
    private final boolean myVerbose;
    private final Map<String, String> myRunnerParameters;
    private final String myTmpDir;
    private final boolean myParseOutOfDate;
    private final long myBuildStartTime;

    private Parameters(@NotNull final String checkoutDir,
                       @Nullable final String findBugsHome,
                       final boolean verbose,
                       @NotNull final Map<String, String> runnerParameters,
                       @NotNull final String tmpDir, final boolean parseOutOfDate, final long buildStartTime) {
      myCheckoutDir = checkoutDir;
      myFindBugsHome = findBugsHome;
      myVerbose = verbose;
      myRunnerParameters = runnerParameters;
      myTmpDir = tmpDir;
      myParseOutOfDate = parseOutOfDate;
      myBuildStartTime = buildStartTime;
    }

    @NotNull
    public InspectionReporter getInspectionReporter() {
      return myInspectionReporter;
    }

    @NotNull
    public String getCheckoutDir() {
      return myCheckoutDir;
    }

    @Nullable
    public String getFindBugsHome() {
      return myFindBugsHome;
    }

    public boolean isVerbose() {
      return myVerbose;
    }

    @NotNull
    public BaseServerLoggerFacade getLogger() {
      return myLogger;
    }

    @NotNull
    public Map<String, String> getRunnerParameters() {
      return myRunnerParameters;
    }

    @NotNull
    public String getTmpDir() {
      return myTmpDir;
    }

    public boolean parseOutOfDate() {
      return myParseOutOfDate;
    }

    public long getBuildStartTime() {
      return myBuildStartTime;
    }
  }

  private void startProcessing(Set<File> reportDirs, String type) {
    final LinkedBlockingQueue<ReportData> reportsQueue = new LinkedBlockingQueue<ReportData>();

    final Parameters parameters =
      new Parameters(myParameters.get(CHECKOUT_DIR),
                     getFindBugsHomePath(myParameters),
                     Boolean.parseBoolean(myParameters.get(VERBOSE_OUTPUT)),
                     myParameters, myParameters.get(TMP_DIR),
                     Boolean.parseBoolean(myParameters.get(PARSE_OUT_OF_DATE)),
                     Long.parseLong(myParameters.get(BUILD_START)));

    myDirectoryWatcher = new XmlReportDirectoryWatcher(parameters, reportDirs, type, reportsQueue);
    myReportProcessor = new XmlReportProcessor(parameters, reportsQueue, myDirectoryWatcher);

    myDirectoryWatcher.start();
    myReportProcessor.start();
  }

  private static Set<File> getReportPathsFromDirProperty(String pathsStr, File checkoutDir) {
    final Set<File> dirs = new HashSet<File>();
    if (pathsStr != null) {
      final String[] paths = pathsStr.split(" *[,\n\r] *");
      for (String path : paths) {
        dirs.add(FileUtil.resolvePath(checkoutDir, path));
      }
    }
    dirs.removeAll(SILENT_PATHS);
    return dirs;
  }

  @Override
  public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
    if (!myStopped) {
      finishWork();
    }
  }

  private void finishWork() {
    myStopped = true;
    if (myReportProcessor == null)
      return; // beforeRunnerStart was not called, i.e. build has failed before runner started

    myReportProcessor.signalStop();
    myDirectoryWatcher.signalStop();

    if (isParsingEnabled(myParameters)) {
      try {
        myReportProcessor.join();
      } catch (InterruptedException e) {
        getLogger().exception(e);
        LOG.warn(e.toString(), e);
      }
      myDirectoryWatcher.logTotals();
    }
  }

  @Override
  public void buildFinished(@NotNull final BuildFinishedStatus buildStatus) {
    myDirectoryWatcher = null;
    myReportProcessor = null;
    myLogger = null;
    myParameters = null;
  }

  public BaseServerLoggerFacade getLogger() {
    return myLogger;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public void beforeInspectionsSent(@NotNull AgentRunningBuild build) {
    finishWork();
  }
}