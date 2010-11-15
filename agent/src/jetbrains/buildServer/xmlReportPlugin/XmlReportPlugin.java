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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements InspectionReporterListener {
  @NonNls
  private static final Collection<String> SILENT_PATHS = Arrays.asList("");
  public static final Logger LOG = Logger.getLogger(XmlReportPlugin.class);

  public static final String CHECKOUT_DIR = "teamcity.build.checkoutDir";

  public static final String TREAT_DLL_AS_SUITE = "xmlReportParsing.nunit.treatDllAsRootSuite";
  public static final String PATHS_TO_EXCLUDE = "xmlReportParsing.exclude";
  public static final String CHECK_REPORT_GROWS = "xmlReportParsing.check.report.grows";
  public static final String CHECK_REPORT_COMPLETE = "xmlReportParsing.check.report.complete";

  private static final String SPLIT_REGEX = " *[,\n\r] *";

  private final InspectionReporter myInspectionReporter;
  private final DuplicatesReporter myDuplicatesReporter;

  @Nullable private volatile AgentRunningBuild myBuild;
  @Nullable private volatile BuildRunnerContext myRunner;
  @Nullable private volatile Date myStartTime;
  @Nullable private volatile ReportProcessingContext myContext;

  private volatile boolean myStopped;

  public XmlReportPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull final InspectionReporter inspectionReporter,
                         @NotNull final DuplicatesReporter duplicatesReporter) {
    agentDispatcher.addListener(this);
    myInspectionReporter = inspectionReporter;
    myInspectionReporter.addListener(this);
    myDuplicatesReporter = duplicatesReporter;
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    myStopped = false;

    myBuild = runner.getBuild();
    myRunner = runner;
    final Date startTime = new Date();
    myStartTime = startTime;

    if (!isParsingEnabled(runner.getRunnerParameters()))
      return;

    final Set<File> reportPaths = getReportPathsFromDirProperty(getXmlReportPaths(runner.getRunnerParameters()),
                                                                runner.getBuild().getCheckoutDirectory());
    final ReportProcessingContext context = createContext(runner.getBuild(), runner, startTime, reportPaths, null);

    context.myDirectoryWatcher.start();
    context.myReportProcessor.start();

    myContext = context;
  }

  private ReportProcessingContext createContext(@NotNull final AgentRunningBuild build,
                                                @NotNull BuildRunnerContext runner,
                                                @NotNull final Date startTime,
                                                @NotNull final Set<File> reportPaths,
                                                @Nullable final Map<String, String> additionalParams
  ) {
    Map<String, String> parametersMap = new HashMap<String, String>(runner.getRunnerParameters());

    parametersMap.put(BUILD_START, "" + startTime.getTime());
    parametersMap.put(TMP_DIR, build.getBuildTempDirectory().getAbsolutePath());
    parametersMap.put(TREAT_DLL_AS_SUITE, runner.getBuildParameters().getSystemProperties().get(TREAT_DLL_AS_SUITE));
    parametersMap.put(PATHS_TO_EXCLUDE, runner.getBuildParameters().getSystemProperties().get(PATHS_TO_EXCLUDE));
    parametersMap.put(CHECK_REPORT_GROWS, runner.getBuildParameters().getSystemProperties().get(CHECK_REPORT_GROWS));
    parametersMap.put(CHECK_REPORT_COMPLETE, runner.getBuildParameters().getSystemProperties().get(CHECK_REPORT_COMPLETE));
    parametersMap.put(LOG_AS_INTERNAL, runner.getBuildParameters().getSystemProperties().get(LOG_AS_INTERNAL));

    if(additionalParams != null)
      parametersMap.putAll(additionalParams);

    final String type = getReportType(parametersMap);
    final LinkedBlockingQueue<ReportData> reportsQueue = new LinkedBlockingQueue<ReportData>();

    final Parameters parameters = new Parameters(reportPaths, parametersMap);

    XmlReportDirectoryWatcher directoryWatcher = new XmlReportDirectoryWatcher(parameters, reportPaths, type, reportsQueue);
    XmlReportProcessor reportProcessor = new XmlReportProcessor(parameters, reportsQueue, directoryWatcher);

    return new ReportProcessingContext(parameters, directoryWatcher, reportProcessor);
  }

  public synchronized void processReports(@NotNull Map<String, String> params, @NotNull Set<File> reportPaths) {
    AgentRunningBuild build = myBuild;
    BuildRunnerContext runner = myRunner;
    Date startTime = myStartTime;

    assert build != null;
    assert runner != null;
    assert startTime != null;

    final String type = getReportType(params);

    ReportProcessingContext context = myContext;

    if(context == null) {
      context = createContext(build, runner, startTime, reportPaths, params);
      context.myDirectoryWatcher.start();
      context.myReportProcessor.start();
      myContext = context;
    } else {
      updateContext(context, params, reportPaths, type);
    }
  }

  private void updateContext(final ReportProcessingContext context,
                             final Map<String, String> params,
                             final Set<File> reportPaths,
                             final String type) {
    context.myParameters.update(reportPaths, params);
    context.myDirectoryWatcher.addPaths(reportPaths, type);
  }

  private static class PathParameters {
    private final boolean myParseOutOfDate;
    private final String myWhenNoDataPublished;
    private final boolean myLogAsInternal;

    private PathParameters(final boolean parseOutOfDate,
                           final String whenNoDataPublished,
                           final boolean logAsInternal) {
      myParseOutOfDate = parseOutOfDate;
      myWhenNoDataPublished = whenNoDataPublished;
      myLogAsInternal = logAsInternal;
    }
  }

  private class Parameters implements XmlReportDirectoryWatcher.Parameters, XmlReportProcessor.Parameters {
    private final Map<String, String> myParameters;
    private final Map<File, PathParameters> myPathParameters; // TODO why not to join this with myPaths storage? What's the point of having this separate?

    private Parameters(@NotNull Set<File> paths, @NotNull final Map<String, String> parameters) {
      myParameters = parameters;
      myPathParameters = new HashMap<File, PathParameters>();

      updateParameters(paths, parameters);
    }

    private void updateParameters(final Set<File> paths, final Map<String, String> parameters) {
      for (final File path : paths) {
        myPathParameters.put(path, new PathParameters(Boolean.parseBoolean(parameters.get(PARSE_OUT_OF_DATE)),
                                                      parameters.get(WHEN_NO_DATA_PUBLISHED),
                                                      !"false".equalsIgnoreCase(parameters.get(LOG_AS_INTERNAL))));
      }
    }

    private void update(@NotNull Set<File> paths, @NotNull final Map<String, String> parameters) {
      updateParameters(paths, parameters);
    }

    @NotNull
    public InspectionReporter getInspectionReporter() {
      return myInspectionReporter;
    }

    @NotNull
    public DuplicatesReporter getDuplicatesReporter() {
      return myDuplicatesReporter;
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

    public boolean parseOutOfDate(@NotNull final File path) {
      return myPathParameters.get(path).myParseOutOfDate;
    }

    public long getBuildStartTime() {
      return Long.parseLong(myParameters.get(BUILD_START));
    }

    @NotNull
    public List<String> getPathsToExclude() {
      if (StringUtil.isEmpty(myParameters.get(PATHS_TO_EXCLUDE))) {
        return Collections.emptyList();       
      }

      final File checkoutDir = new File(getCheckoutDir());
      final List<String> paths = new ArrayList<String>();
      for (final String s : myParameters.get(PATHS_TO_EXCLUDE).split(SPLIT_REGEX)) {
        paths.add(FileUtil.resolvePath(checkoutDir, s).getAbsolutePath());
      }
      return paths;
    }

    @NotNull
    public String getWhenNoDataPublished(@NotNull File path) {
      return myPathParameters.get(path).myWhenNoDataPublished;
    }

    public boolean getLogAsInternal(@NotNull final File path) {
      return myPathParameters.get(path).myLogAsInternal;
    }
  }

  private static Set<File> getReportPathsFromDirProperty(String pathsStr, File checkoutDir) {
    final Set<File> dirs = new HashSet<File>();
    if (pathsStr != null) {
      final String[] paths = pathsStr.split(SPLIT_REGEX);
      for (String path : paths) {
        dirs.add(FileUtil.resolvePath(checkoutDir, path));
      }
    }
    dirs.removeAll(SILENT_PATHS);
    return dirs;
  }


  @Override
  public synchronized void runnerFinished(
    @NotNull BuildRunnerContext runner,
    @NotNull BuildFinishedStatus status) {
    if (!myStopped) {
      finishWork();
    }
  }

  private synchronized void finishWork() {
    myStopped = true;

    ReportProcessingContext context = myContext;
    if (context == null)
      return; // beforeRunnerStart was not called, i.e. build has failed before runner started

    context.myReportProcessor.signalStop();
    context.myDirectoryWatcher.signalStop();

    final AgentRunningBuild build = myBuild;
    try {
      context.myReportProcessor.join();
    } catch (InterruptedException e) {
      if(build != null)
        build.getBuildLogger().exception(e);

      LOG.warn(e.toString(), e);
    }
    if(build != null)
      context.myDirectoryWatcher.logTotals(build.getBuildLogger());

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
    @NotNull private final Parameters myParameters;
    @NotNull private final XmlReportDirectoryWatcher myDirectoryWatcher;
    @NotNull private final XmlReportProcessor myReportProcessor;

    private ReportProcessingContext(@NotNull final Parameters parameters,
                                    @NotNull final XmlReportDirectoryWatcher directoryWatcher,
                                    @NotNull final XmlReportProcessor reportProcessor) {
      myParameters = parameters;
      myDirectoryWatcher = directoryWatcher;
      myReportProcessor = reportProcessor;
    }
  }
}