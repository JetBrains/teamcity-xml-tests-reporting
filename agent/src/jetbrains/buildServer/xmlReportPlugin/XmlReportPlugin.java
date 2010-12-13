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

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporterListener;
import jetbrains.buildServer.util.EventDispatcher;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginConstants.*;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.getXmlReportPaths;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.isParsingEnabled;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements InspectionReporterListener {
  public static final Logger LOG = Logger.getLogger(XmlReportPlugin.class);

  private final InspectionReporter myInspectionReporter;
  private final DuplicatesReporter myDuplicatesReporter;

  @Nullable private volatile BuildRunnerContext myRunner;
  @Nullable private volatile Date myStartTime;
  @Nullable private volatile ReportProcessingContext myContext;

  private volatile boolean myStopped;

  private volatile File myCheckoutDir;

  public XmlReportPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull final InspectionReporter inspectionReporter,
                         @NotNull final DuplicatesReporter duplicatesReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
    myInspectionReporter.addListener(this);
    myDuplicatesReporter = duplicatesReporter;
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    myCheckoutDir = runningBuild.getCheckoutDirectory();
  }

  public File getCheckoutDir() {
    return myCheckoutDir;
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    myStopped = false;

    myRunner = runner;
    final Date startTime = new Date();
    myStartTime = startTime;

    if (!isParsingEnabled(runner.getRunnerParameters())) return;

    final ReportProcessingContext context = createContext(runner, startTime, null, null);

    context.myDirectoryWatcher.start();
    context.myReportProcessor.start();

    myContext = context;
  }

  private ReportProcessingContext createContext(@NotNull BuildRunnerContext runner,
                                                @NotNull final Date startTime,
                                                @Nullable final Set<File> paths,
                                                @Nullable final Map<String, String> additionalParams
  ) {
    Map<String, String> parametersMap = new HashMap<String, String>(runner.getRunnerParameters());

    parametersMap.put(BUILD_START, "" + startTime.getTime());
    parametersMap.put(TMP_DIR, runner.getBuild().getBuildTempDirectory().getAbsolutePath());
    parametersMap.put(TREAT_DLL_AS_SUITE, runner.getBuildParameters().getSystemProperties().get(TREAT_DLL_AS_SUITE));
    parametersMap.put(CHECK_REPORT_GROWS, runner.getBuildParameters().getSystemProperties().get(CHECK_REPORT_GROWS));
    parametersMap.put(CHECK_REPORT_COMPLETE, runner.getBuildParameters().getSystemProperties().get(CHECK_REPORT_COMPLETE));
    parametersMap.put(LOG_AS_INTERNAL, runner.getBuildParameters().getSystemProperties().get(LOG_AS_INTERNAL));

    if(additionalParams != null) parametersMap.putAll(additionalParams);

    final Set<File> reportPaths = paths != null ? paths : getReportPathsFromDirProperty(getXmlReportPaths(parametersMap));

    final LinkedBlockingQueue<ReportData> reportsQueue = new LinkedBlockingQueue<ReportData>();
    final XmlReportPluginParameters parameters = new XmlReportPluginParametersImpl(runner.getBuild().getBuildLogger(), myInspectionReporter, myDuplicatesReporter);

    final XmlReportDirectoryWatcher directoryWatcher = new XmlReportDirectoryWatcher(parameters, reportsQueue);
    final XmlReportProcessor reportProcessor = new XmlReportProcessor(parameters, reportsQueue, directoryWatcher);

    parameters.updateParameters(reportPaths, parametersMap);

    return new ReportProcessingContext(parameters, directoryWatcher, reportProcessor);
  }

  private static final Collection<String> SILENT_PATHS = Arrays.asList("");

  private static Set<File> getReportPathsFromDirProperty(String pathsStr) {
    final Set<File> dirs = new HashSet<File>();
    if (pathsStr != null && pathsStr.length() > 0) {
      for (String path : pathsStr.split(SPLIT_REGEX)) {
        dirs.add(new File(path));
      }
    } else {
      throw new RuntimeException("Report paths are empty");
    }
    dirs.removeAll(SILENT_PATHS);
    return dirs;
  }

  public synchronized void processReports(@NotNull Map<String, String> params, @NotNull Set<File> reportPaths) {
    final BuildRunnerContext runner = myRunner;
    final Date startTime = myStartTime;

    assert runner != null;
    assert startTime != null;

    ReportProcessingContext context = myContext;

    if(context == null) {
      context = createContext(runner, startTime, reportPaths, params);
      context.myDirectoryWatcher.start();
      context.myReportProcessor.start();
      myContext = context;
    } else {
      updateContext(context, params, reportPaths);
    }
  }

  private void updateContext(final ReportProcessingContext context,
                             final Map<String, String> params,
                             final Set<File> reportPaths) {
    context.myParameters.updateParameters(reportPaths, params);
  }

  @Override
  public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
    finishWork();
  }

  private synchronized void finishWork() {
    if (myStopped) return;

    myStopped = true;

    final ReportProcessingContext context = myContext;
    if (context == null)
      return; // beforeRunnerStart was not called, i.e. build has failed before runner started

    context.myReportProcessor.signalStop();
    context.myDirectoryWatcher.signalStop();

    final BuildRunnerContext runner = myRunner;
    try {
      context.myReportProcessor.join();
    } catch (InterruptedException e) {
      if(runner != null)
        runner.getBuild().getBuildLogger().exception(e);

      LOG.warn(e.toString(), e);
    }
    if(runner != null)
      context.myDirectoryWatcher.logTotals(runner.getBuild().getBuildLogger());

    myRunner = null;
    myStartTime = null;
    myContext = null;
  }

  public void beforeInspectionsSent(@NotNull AgentRunningBuild build) {
    finishWork();
  }

  private static class ReportProcessingContext {
    @NotNull private final XmlReportPluginParameters myParameters;
    @NotNull private final XmlReportDirectoryWatcher myDirectoryWatcher;
    @NotNull private final XmlReportProcessor myReportProcessor;

    private ReportProcessingContext(@NotNull final XmlReportPluginParameters parameters,
                                    @NotNull final XmlReportDirectoryWatcher directoryWatcher,
                                    @NotNull final XmlReportProcessor reportProcessor) {
      myParameters = parameters;
      myDirectoryWatcher = directoryWatcher;
      myReportProcessor = reportProcessor;
    }
  }
}