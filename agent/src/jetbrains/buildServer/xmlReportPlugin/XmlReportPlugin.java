/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.util.DiagnosticUtil;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.executors.ExecutorsFactory;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.TeamCityDuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.TeamCityInspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TeamCityTestReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.AntPathMatcher;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements RulesProcessor {
  @NotNull
  private final jetbrains.buildServer.agent.inspections.InspectionReporter myInspectionReporter;
  @NotNull
  private final jetbrains.buildServer.agent.duplicates.DuplicatesReporter myDuplicatesReporter;

  @Nullable
  private AgentRunningBuild myBuild;

  @NotNull
  private final ExecutorService myParseExecutor;

  @NotNull
  private final Map<String, ParserFactory> myParserFactoryMap;

  @NotNull private final BuildAgentConfiguration myConfiguration;

  @Nullable
  private ProcessingContext myBuildProcessingContext;

  @Nullable
  private ProcessingContext myStepProcessingContext;

  public XmlReportPlugin(@NotNull Map<String, ParserFactory> parserFactoryMap,
                         @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull jetbrains.buildServer.agent.inspections.InspectionReporter inspectionReporter,
                         @NotNull jetbrains.buildServer.agent.duplicates.DuplicatesReporter duplicatesReporter,
                         @NotNull BuildAgentConfiguration configuration) {
    myParserFactoryMap = parserFactoryMap;
    myConfiguration = configuration;

    agentDispatcher.addListener(this);

    myInspectionReporter = inspectionReporter;
    myDuplicatesReporter = duplicatesReporter;

    myParseExecutor = createExecutor();
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    myBuild = runningBuild;
    initBuildProcessingContext(runningBuild);
  }

  private void initBuildProcessingContext(final @NotNull AgentRunningBuild runningBuild) {
    myBuildProcessingContext = new ProcessingContext(new ArrayList<RulesContext>());

    final Collection<AgentBuildFeature> features = getBuild().getBuildFeaturesOfType("xml-report-plugin");
    if (features.isEmpty()) return;

    for (AgentBuildFeature feature : features) {
      final Map<String, String> params = feature.getParameters();
      params.putAll(runningBuild.getSharedConfigParameters());
      getBuildProcessingContext().rulesContexts.add(createRulesContext(new RulesData(getRules(params), params, getBuildProcessingContext().startTime)));
    }
  }

  @Override
  public synchronized void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    startProcessing(getBuildProcessingContext());
    myStepProcessingContext = new ProcessingContext(new CopyOnWriteArrayList<RulesContext>());
  }

  public synchronized void processRules(@NotNull File rulesFile,
                                        @NotNull Map<String, String> params) {
    if (getNotNullStepProcessingContext().finished) return;

     // here we check if this path is already monitored for reports of this type
     // we also don't support processing two inspections type during one build
    final String newType = getReportType(params);

    for (RulesContext context : getNotNullStepProcessingContext().rulesContexts) {
      final String existingType = context.getRulesData().getType();

      if (existingType.equals(newType)) {
        final Collection<File> paths = context.getRulesData().getRules().getPaths();
        if (paths.size() == 1) {
          if (paths.contains(rulesFile)) {
            LoggingUtils.LOG.info("Skip monitoring " + rulesFile + " (already monitoring)");
            return;
          }
        }
      }
      if (XmlReportPluginUtil.isInspectionType(existingType) && XmlReportPluginUtil.isInspectionType(newType)) {
        LoggingUtils
          .warn(String.format("Two different inspections can not be processed during one build, skip %s reports", getReportTypeName(
            newType)), getBuild().getBuildLogger());
        return;
      }
    }

    final RulesData rulesData = new RulesData(getRules(rulesFile, params), params, getNotNullStepProcessingContext().startTime);

    getNotNullStepProcessingContext().rulesContexts.add(createRulesContext(rulesData));

    startProcessing(getNotNullStepProcessingContext());
  }

  @Override
  public synchronized void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
    if (getStepProcessingContext() == null) return; // if beforeRunnerStart was not called

    finishProcessing(getNotNullStepProcessingContext(), true);

    finishProcessing(getBuildProcessingContext(), false);
    startProcessing(getBuildProcessingContext());

    myStepProcessingContext = null;
  }

  @Override
  public void beforeBuildFinish(@NotNull final AgentRunningBuild build, @NotNull final BuildFinishedStatus buildStatus) {
    finishProcessing(getBuildProcessingContext(), true);
    myBuild = null;
    myBuildProcessingContext = null;
  }

  @Override
  public void agentShutdown() {
    shutdownExecutor(myParseExecutor);
  }

  private RulesContext createRulesContext(@NotNull final RulesData rulesData) {
    final RulesState fileStateHolder = new RulesState();
    final ParserFactory parserFactory = getParserFactory(rulesData.getType());

    final RulesContext rulesContext = new RulesContext(rulesData, fileStateHolder);

    final MonitorRulesCommand monitorRulesCommand = new MonitorRulesCommand(rulesData.getMonitorRulesParameters(), rulesContext.getRulesState(),
      new MonitorRulesCommand.MonitorRulesListener() {
        public void modificationDetected(@NotNull File file) {
          submitParsing(file, rulesContext, parserFactory);
        }
      });

    rulesContext.setMonitorRulesCommand(monitorRulesCommand);

    return rulesContext;
  }

  private void startProcessing(@NotNull final ProcessingContext processingContext) {
    if (isStarted(processingContext)) return;
    if (!rulesExist(processingContext)) return;

    processingContext.finished = false;
    processingContext.monitorThread = new Thread(new Runnable() {
      public void run() {
        while (!processingContext.finished) {
          processAllRules(processingContext);

          try {
            Thread.sleep(500L);
          } catch (InterruptedException e) {
            getBuild().getBuildLogger().exception(e);
          }
        }
      }
    });
    processingContext.monitorThread.start();
  }

  private boolean rulesExist(final @NotNull ProcessingContext processingContext) {
    return processingContext.rulesContexts.size() > 0;
  }

  private boolean isStarted(final @NotNull ProcessingContext processingContext) {
    return processingContext.monitorThread != null;
  }

  private void processAllRules(final @NotNull ProcessingContext processingContext) {
    for (RulesContext rulesContext : processingContext.rulesContexts) {
      rulesContext.getMonitorRulesCommand().run();
    }
  }

  private void finishProcessing(@NotNull final ProcessingContext processingContext, boolean logStatistics) {
    if (!isStarted(processingContext) && !rulesExist(processingContext)) return;
    if (!isStarted(processingContext)) {
      // process all rules even if we do not have build steps
      processAllRules(processingContext);
    }

    processingContext.finished = true;
    try {
      if (isStarted(processingContext)) {
        processingContext.monitorThread.join();
      }

      processingContext.monitorThread = null;

      for (RulesContext rulesContext : processingContext.rulesContexts) {
        for (Future future : rulesContext.getParseTasks()) {
          future.get();
        }

        rulesContext.clearParseTasks();

        rulesContext.getMonitorRulesCommand().run();

        for (Future future : rulesContext.getParseTasks()) {
          future.get();
        }

        if (logStatistics) logStatistics(rulesContext);
      }
    } catch (Exception e) {
      LoggingUtils.logError("Exception occurred while finishing rules monitoring", e, getBuild().getBuildLogger(), false);
    }
  }

  private void submitParsing(@NotNull File file, @NotNull final RulesContext rulesContext, @NotNull ParserFactory parserFactory) {
    final ParseReportCommand parseReportCommand = new ParseReportCommand(file, rulesContext.getRulesData().getParseReportParameters(), rulesContext.getRulesState(), parserFactory);

    synchronized (myParseExecutor) {
      rulesContext.addParseTask(myParseExecutor.submit(parseReportCommand));
    }
  }

  private void shutdownExecutor(@NotNull ExecutorService executor) {
    executor.shutdown();
    try {
      executor.awaitTermination(5, TimeUnit.SECONDS);
      if (!executor.isTerminated()) {
        LoggingUtils.LOG.warn("Waiting for one of xml-report-plugin executors to complete");
      }

      executor.shutdownNow();

      executor.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LoggingUtils.LOG.warn(e.toString());
      LoggingUtils.LOG.debug(e.getMessage(), e);
    }

    if (!executor.isTerminated()) {
      final File dump = DiagnosticUtil.threadDumpToDirectory(myConfiguration.getAgentLogsDirectory(), new DiagnosticUtil.ThreadDumpData()
        .withSummary("Stopped waiting for one xml-report-plugin executors to complete, it is still running"));
      LoggingUtils.LOG.warn("Stopped waiting for one xml-report-plugin executors to complete, it is still running. Thread dump is  saved to " + dump.getAbsolutePath());
    }
  }

  private static ExecutorService createExecutor() {
    return ExecutorsFactory.newFixedDaemonExecutor("xml-report-plugin", 1);
  }

  private Rules getRules(@NotNull Map<String, String> parameters) {
    return getRules(getXmlReportPaths(parameters), parameters);
  }

  private Rules getRules(@Nullable File rulesFile, @NotNull Map<String, String> parameters) {
    final String rulesStr = rulesFile == null ? getXmlReportPaths(parameters) : rulesFile.getAbsolutePath();
    return getRules(rulesStr, parameters);
  }

  @NotNull
  private Rules getRules(@NotNull String rulesStr, @NotNull Map<String, String> parameters) {
    final List<String> rules = Arrays.asList(rulesStr.split(XmlReportPluginConstants.SPLIT_REGEX));
    final File baseDir = getBuild().getCheckoutDirectory();

    if (rules.size() == 1) {
      final String rule = rules.get(0);
      if (isFilePath(rule)) {
        return new FileRules(new File(resolveRule(rule, baseDir)));
      }
    }

    return isOptimizedFilesCollectionEnabled(parameters) ? new OptimizingIncludeExcludeRules(baseDir, rules) : new FullSearchIncludeExcludeRules(baseDir, rules);
  }

  @NotNull
  private String resolveRule(@NotNull String rule, @NotNull File baseDir) {
    if (rule.startsWith("+:") || rule.startsWith("-:")) {
      rule = rule.substring(2);
    }
    return FileUtil.normalizeAbsolutePath(FileUtil.resolvePath(baseDir, rule).getAbsolutePath());
  }

  private boolean isFilePath(@NotNull String rule) {
    return !new AntPathMatcher().isPattern(rule);
  }

  private void logStatistics(@NotNull final RulesContext rulesContext) {
    final BuildProgressLogger logger = getBuild().getBuildLogger();

    final Map<File, ParsingResult> succeeded = rulesContext.getRulesState().getProcessedFiles();
    final Map<File, ParsingResult> failedToParse = rulesContext.getRulesState().getFailedToProcessFiles();
    final List<File> outOfDate = rulesContext.getRulesState().getOutOfDateFiles();

    final int processedFileCount = succeeded.size() + failedToParse.size();

    final LogAction summaryLogAction = processedFileCount == 0 ? rulesContext.getRulesData().getWhenNoDataPublished() : LogAction.INFO;
    if (summaryLogAction == LogAction.DO_NOTHING) return;

    LoggingUtils.logInTarget(LoggingUtils.getTypeDisplayName(rulesContext.getRulesData().getType()) + " report watcher",
      new Runnable() {
        public void run() {
          final int totalFileCount = processedFileCount + outOfDate.size();
          summaryLogAction.doLogAction(
            totalFileCount == 0 ?
            "No reports found for paths:" :
            totalFileCount + " report" + getEnding(totalFileCount) + " found for paths:", logger);

          final Collection<String> rules = rulesContext.getRulesData().getRules().getBody();

          if (rules.size() == 0) {
            LoggingUtils.warn("<no paths>", logger);
          } else {
            for (String r : rules) {
              summaryLogAction.doLogAction(r, logger);
            }
          }

          final ParsingResult result = myParserFactoryMap.get(rulesContext.getRulesData().getType()).createEmptyResult();

          if (!failedToParse.isEmpty()) {
            LoggingUtils.logInTarget("Parsing errors",
                                     new Runnable() {
                                       public void run() {
                                         LoggingUtils
                                           .error("Failed to parse " + failedToParse.size() + " report" + getEnding(failedToParse.size()), logger);

                                         int i = 0;
                                         for (Map.Entry<File, ParsingResult> e : failedToParse.entrySet()) {
                                           final Throwable p = getProblem(e.getValue());
                                           String m = getPathInCheckoutDir(e.getKey());

                                           if (p == null) m = m + ": Report is incomplete or has unexpected structure";

                                           if (rulesContext.getRulesData().isVerbose() || failedToParse.size() == 1) {
                                             LoggingUtils.logError(m, p, logger, true);
                                           } else if (i < 10) {
                                             LoggingUtils.LOG.warn(m);
                                           } else {
                                             LoggingUtils.LOG.debug(m, p);
                                           }
                                           result.accumulate(e.getValue());
                                           ++i;
                                         }
                                       }
                                     }, logger);

            if (rulesContext.getRulesData().failBuildIfParsingFailed()) {
              logger.logBuildProblem(createBuildProblem(rulesContext.getRulesData().getType(), failedToParse.keySet()));
            }
          }

          if (!succeeded.isEmpty()) {
            LoggingUtils.logInTarget("Successfully parsed",
                                     new Runnable() {
                                       public void run() {
                                         LoggingUtils
                                           .message(succeeded.size() + " report" + getEnding(succeeded.size()), logger);

                                         for (Map.Entry<File, ParsingResult> e : succeeded.entrySet()) {
                                           final String m = getPathInCheckoutDir(e.getKey());

                                           if (rulesContext.getRulesData().isVerbose() || succeeded.size() == 1) {
                                             LoggingUtils.message(m, logger);
                                           } else {
                                             LoggingUtils.LOG.debug(m);
                                           }
                                           result.accumulate(e.getValue());
                                         }
                                       }
                                     }, logger);
          }

          if (!outOfDate.isEmpty()) {
            LoggingUtils.logInTarget("Skipped as out-of-date", new Runnable() {
              @Override
              public void run() {
                LoggingUtils.verbose("Processing start time is: [" + rulesContext.getRulesData().getMonitorRulesParameters().getStartTime() + "]", logger);
                summaryLogAction.doLogAction(outOfDate.size() + " report" + getEnding(outOfDate.size()), logger);

                int i = 0;
                for (File f : outOfDate) {
                  final String m = getPathInCheckoutDir(f);
                  final String details = m + " has last modified timestamp [" + f.lastModified() + "]";

                  if (rulesContext.getRulesData().isVerbose() || outOfDate.size() == 1 || processedFileCount == 0) {
                    summaryLogAction.doLogAction(m, logger);
                    LoggingUtils.verbose(details, logger);
                  } else {
                    if (i < 10) {
                      LoggingUtils.LOG.info(m);
                    } else {
                      LoggingUtils.LOG.debug(m);
                    }
                    LoggingUtils.LOG.debug(details);
                  }
                  ++i;
                }

              }
            }, logger);
          }
          result.logAsTotalResult(rulesContext.getRulesData().getParseReportParameters());
        }
      }, logger);
  }

  private String getPathInCheckoutDir(@NotNull File file) {
    String relativePath = null;
    if (FileUtil.isAncestor(getBuild().getCheckoutDirectory(), file, false)) {
      relativePath = FileUtil.getRelativePath(getBuild().getCheckoutDirectory(), file);
    }
    return relativePath == null ? file.getAbsolutePath() : relativePath;
  }

  @Nullable Throwable getProblem(@NotNull ParsingResult parsingResult) {
    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored"})
    final Throwable problem = parsingResult.getProblem();
    if (problem == null) return null;
    assert problem instanceof ParsingException;
    return problem.getCause();
  }

  @NotNull
  private static String getEnding(int count) {
    return count == 1 ? "" : "s";
  }

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  private AgentRunningBuild getBuild() {
    if (myBuild == null) {
      throw new IllegalStateException("Build is null");
    }
    return myBuild;
  }

  @NotNull
  private ParserFactory getParserFactory(@NotNull String type) {
    if (!myParserFactoryMap.containsKey(type))
      throw new IllegalArgumentException("No factory for " + type);
    return myParserFactoryMap.get(type);
  }

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  private ProcessingContext getBuildProcessingContext() {
    if (myBuildProcessingContext == null) {
      throw new IllegalStateException("Build processing context is null");
    }
    return myBuildProcessingContext;
  }

  @Nullable
  private synchronized ProcessingContext getStepProcessingContext() {
    return myStepProcessingContext;
  }

  @NotNull
  private synchronized ProcessingContext getNotNullStepProcessingContext() {
    final ProcessingContext context = getStepProcessingContext();
    if (context == null) {
      throw new IllegalStateException("Step processing context is null");
    }
    return context;
  }

  public class RulesData {
    @NotNull
    private final Rules myRules;

    @NotNull
    private final Map<String, String> myParameters;

    private final long myStartTime;

    public RulesData(@NotNull Rules rules,
                     @NotNull Map<String, String> parameters,
                     long startTime) {
      myRules = rules;
      myParameters = parameters;
      myStartTime = startTime;
    }

    @NotNull
    public Rules getRules() {
      return myRules;
    }

    @NotNull
    public String getType() {
      return XmlReportPluginUtil.getReportType(myParameters);
    }

    public boolean isVerbose() {
      return isOutputVerbose(myParameters);
    }

    @NotNull
    public LogAction getWhenNoDataPublished() {
      return LogAction.getAction(whenNoDataPublished(myParameters));
    }

    public boolean failBuildIfParsingFailed() {
      return isFailBuildIfParsingFailed(myParameters);
    }

    @NotNull
    public MonitorRulesCommand.MonitorRulesParameters getMonitorRulesParameters() {
      return new MonitorRulesCommand.MonitorRulesParameters() {
        @NotNull
        public Rules getRules() {
          return myRules;
        }

        @NotNull
        public String getType() {
          return getReportType(myParameters);
        }

        public boolean isParseOutOfDate() {
          return isParseOutOfDateReports(myParameters);
        }

        public long getStartTime() {
          return myStartTime;
        }

        @NotNull
        public BuildProgressLogger getThreadLogger() {
          return getBuild().getBuildLogger().getThreadLogger();
        }
      };
    }

    @NotNull
    public ParseParameters getParseReportParameters() {
      return new ParseParameters() {
        public boolean isVerbose() {
          return isOutputVerbose(myParameters);
        }

        @NotNull
        public BuildProgressLogger getThreadLogger() {
          return getBuild().getBuildLogger().getThreadLogger();
        }

        @NotNull
        private BuildProgressLogger getInternalizingThreadLogger() {
          return isLogAsInternal() ?
            ((MessageTweakingSupport) getThreadLogger()).getTweakedLogger(MessageInternalizer.MESSAGE_INTERNALIZER)
            : getThreadLogger();
        }

        private boolean isLogAsInternal() {
          return isLogIsInternal(myParameters);
        }

        @NotNull
        public InspectionReporter getInspectionReporter() {
          return new TeamCityInspectionReporter(myInspectionReporter, getBuild().getBuildLogger(), getBuild().getCheckoutDirectory());
        }

        @NotNull
        public DuplicationReporter getDuplicationReporter() {
          return new TeamCityDuplicationReporter(myDuplicatesReporter);
        }

        @NotNull
        public TestReporter getTestReporter() {
          return new TeamCityTestReporter(getInternalizingThreadLogger());
        }

        @NotNull
        public Map<String, String> getParameters() {
          return Collections.unmodifiableMap(myParameters);
        }

        @NotNull
        public String getType() {
          return getReportType(myParameters);
        }

        @NotNull
        public File getCheckoutDir() {
          return getBuild().getCheckoutDirectory();
        }
      };
    }
  }

  @NotNull
  private BuildProblemData createBuildProblem(@NotNull String type, @NotNull Collection<File> failedToParse) {
    return BuildProblemData.createBuildProblem(String.valueOf(failedToParse.hashCode()), getBuildProblemType(type), "Failed to parse xml " + StringUtil.pluralize("report", failedToParse.size()));
  }

  @NotNull
  private String getBuildProblemType(@NotNull String type) {
    return XmlReportPluginConstants.BUILD_PROBLEM_TYPE + StringUtil.capitalize(type) + "ParsingFailure";
  }

  private final class ProcessingContext {
    private final long startTime;
    private volatile boolean finished;
    @Nullable
    private volatile Thread monitorThread;
    @NotNull
    private final List<RulesContext> rulesContexts;

    private ProcessingContext(@NotNull List<RulesContext> rulesContexts) {
      this.rulesContexts = rulesContexts;

      startTime = new Date().getTime()/1000*1000;

      finished = false;
    }
  }
}
