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

  private final InspectionReporter myInspectionReporter;

  @Nullable private volatile AgentRunningBuild myBuild;
  @Nullable private volatile Date myStartTime;
  @Nullable private volatile ReportProcessingContext myContext;

  private volatile boolean myStopped;

  public XmlReportPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull final InspectionReporter inspectionReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
    myInspectionReporter.addListener(this);
  }

  @Override
  public void beforeRunnerStart(@NotNull AgentRunningBuild build) {
    myStopped = false;

    assert myBuild == null;

    myBuild = build;
    final Date startTime = new Date();
    myStartTime = startTime;

    if (!isParsingEnabled(build.getRunnerParameters()))
      return;

    final Set<File> reportPaths = getReportPathsFromDirProperty(getXmlReportPaths(build.getRunnerParameters()),
                                                                build.getCheckoutDirectory());
    final ReportProcessingContext context = createContext(build, startTime, reportPaths, null);

    context.myDirectoryWatcher.start();
    context.myReportProcessor.start();

    myContext = context;
  }

  private ReportProcessingContext createContext(@NotNull final AgentRunningBuild build,
                                                @NotNull final Date startTime,
                                                @NotNull final Set<File> reportPaths,
                                                @Nullable final Map<String, String> additionalParams
  ) {
    Map<String, String> parametersMap = new HashMap<String, String>(build.getRunnerParameters());

    parametersMap.put(BUILD_START, "" + startTime.getTime());
    parametersMap.put(TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
    parametersMap.put(TREAT_DLL_AS_SUITE, build.getBuildParameters().getSystemProperties().get(TREAT_DLL_AS_SUITE));

    if(additionalParams != null)
      parametersMap.putAll(additionalParams);

    final String type = getReportType(parametersMap);
    //TODO: can avoid this if by adding paths presence in the web IU
    if (reportPaths.size() == 0) {
      enableXmlReportParsing(parametersMap, ""); //can avoid this by adding paths presence in the web IU
    }

    final LinkedBlockingQueue<ReportData> reportsQueue = new LinkedBlockingQueue<ReportData>();

    final Parameters parameters = new Parameters(parametersMap);

    XmlReportDirectoryWatcher directoryWatcher = new XmlReportDirectoryWatcher(parameters, reportPaths, type, reportsQueue);
    XmlReportProcessor reportProcessor = new XmlReportProcessor(parameters, reportsQueue, directoryWatcher);

    return new ReportProcessingContext(parametersMap, directoryWatcher, reportProcessor);
  }

  public synchronized void processReports(Map<String, String> params, Set<File> reportPaths) {
    AgentRunningBuild build = myBuild;
    Date startTime = myStartTime;

    assert build != null;
    assert startTime != null;

    final String type = getReportType(params);

    ReportProcessingContext context = myContext;

    if(context == null) {
      context = createContext(build, startTime, reportPaths, params);
      context.myDirectoryWatcher.start();
      context.myReportProcessor.start();
      myContext = context;
    } else {
      context.myParameters.putAll(params);
      context.myDirectoryWatcher.addPaths(reportPaths, type);
    }
  }

  private class Parameters implements XmlReportDirectoryWatcher.Parameters, XmlReportProcessor.Parameters {
    private final Map<String, String> myParameters;

    private Parameters(@NotNull final Map<String, String> parameters) {
      myParameters = parameters;
    }

    @NotNull
    public InspectionReporter getInspectionReporter() {
      return myInspectionReporter;
    }

    @NotNull
    public String getCheckoutDir() {
      return myParameters.get(CHECKOUT_DIR);
    }

    @Nullable
    public String getFindBugsHome() {
      return getFindBugsHomePath(myParameters);
    }

    public boolean isVerbose() {
      return Boolean.parseBoolean(myParameters.get(VERBOSE_OUTPUT));
    }

    @NotNull
    public BuildProgressLogger getLogger() {
      final AgentRunningBuild build = myBuild;
      assert build != null;
      
      return build.getBuildLogger();
    }

    @NotNull
    public Map<String, String> getRunnerParameters() {
      return myParameters;
    }

    @NotNull
    public String getTmpDir() {
      return myParameters.get(TMP_DIR);
    }

    public boolean parseOutOfDate() {
      return Boolean.parseBoolean(myParameters.get(PARSE_OUT_OF_DATE));
    }

    public long getBuildStartTime() {
      return Long.parseLong(myParameters.get(BUILD_START));
    }
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

    ReportProcessingContext context = myContext;
    if (context == null)
      return; // beforeRunnerStart was not called, i.e. build has failed before runner started

    context.myReportProcessor.signalStop();
    context.myDirectoryWatcher.signalStop();

    try {
      context.myReportProcessor.join();
    } catch (InterruptedException e) {
      final AgentRunningBuild build = myBuild;
      if(build != null)
        build.getBuildLogger().exception(e);

      LOG.warn(e.toString(), e);
    }
    context.myDirectoryWatcher.logTotals();
  }

  @Override
  public void buildFinished(@NotNull final BuildFinishedStatus buildStatus) {
    myBuild = null;
    myStartTime = null;
    myContext = null;
  }

  public boolean isStopped() {
    return myStopped;
  }

  public void beforeInspectionsSent(@NotNull AgentRunningBuild build) {
    finishWork();
  }

  private static class ReportProcessingContext {
    @NotNull private final Map<String, String> myParameters;
    @NotNull private final XmlReportDirectoryWatcher myDirectoryWatcher;
    @NotNull private final XmlReportProcessor myReportProcessor;

    private ReportProcessingContext(@NotNull final Map<String, String> parameters,
                                    @NotNull final XmlReportDirectoryWatcher directoryWatcher,
                                    @NotNull final XmlReportProcessor reportProcessor) {
      myParameters = parameters;
      myDirectoryWatcher = directoryWatcher;
      myReportProcessor = reportProcessor;
    }
  }
}