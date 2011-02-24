/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.*;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.messages.serviceMessages.MapSerializerUtil;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.NamedThreadFactory;
import jetbrains.buildServer.util.ThreadUtil;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.TeamCityDuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.TeamCityInspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TeamCityTestReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements RulesProcessor {
  @NotNull
  private static final NamedThreadFactory THREAD_FACTORY = new NamedThreadFactory("xml-report-plugin");

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

  @Nullable
  private ProcessingContext myBuildProcessingContext;
  @Nullable
  private ProcessingContext myStepProcessingContext;

  public XmlReportPlugin(@NotNull Map<String, ParserFactory> parserFactoryMap,
                         @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull jetbrains.buildServer.agent.inspections.InspectionReporter inspectionReporter,
                         @NotNull jetbrains.buildServer.agent.duplicates.DuplicatesReporter duplicatesReporter) {
    myParserFactoryMap = parserFactoryMap;

    agentDispatcher.addListener(this);

    myInspectionReporter = inspectionReporter;
    myDuplicatesReporter = duplicatesReporter;

    myParseExecutor = createExecutor();
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    myBuild = runningBuild;
    myBuildProcessingContext = new ProcessingContext(new ArrayList<RulesContext>());

    final Map<String, String> params = runningBuild.getSharedConfigParameters();

    if (!params.containsKey(XmlReportPluginConstants.PARSING_ENABLED)) return;

    for (String key : params.keySet()) {
      if (key.startsWith(XmlReportPluginConstants.FEATURE_PARAMS)) {
        try {
          final Map<String, String> featureParams = MapSerializerUtil.stringToProperties(params.get(key), MapSerializerUtil.STD_ESCAPER, false);
          featureParams.putAll(params);

          final RulesData rulesData
            = new RulesData(getRules(featureParams), featureParams, getBuildProcessingContext().startTime);

          getBuildProcessingContext().rulesContexts.add(createRulesContext(rulesData));
        } catch (ParseException e) {
          runningBuild.getBuildLogger().exception(e);
        }
      }
    }
    startProcessing(getBuildProcessingContext());
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    myStepProcessingContext = new ProcessingContext(new CopyOnWriteArrayList<RulesContext>());
  }

  public synchronized void processRules(@NotNull File rulesFile,
                                        @NotNull Map<String, String> params) {
    if (getStepProcessingContext().finished) return;

    final RulesData rulesData = new RulesData(getRules(rulesFile), params, getStepProcessingContext().startTime);

    getStepProcessingContext().rulesContexts.add(createRulesContext(rulesData));

    startProcessing(getStepProcessingContext());
  }

  @Override
  public synchronized void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
    getStepProcessingContext().finished = true;

    if (getStepProcessingContext().rulesContexts.isEmpty()) return;

    finishProcessing(getStepProcessingContext());
    myStepProcessingContext = null;
  }

  @Override
  public void beforeBuildFinish(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    getBuildProcessingContext().finished = true;

    if (getBuildProcessingContext().rulesContexts.isEmpty()) return;

    finishProcessing(getBuildProcessingContext());
    myBuildProcessingContext = null;
  }

  @Override
  public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    myBuild = null;
  }

  @Override
  public void agentShutdown() {
    shutdownExecutor(myParseExecutor);
  }

  private RulesContext createRulesContext(@NotNull final RulesData rulesData) {
    final RulesState fileStateHolder = new RulesState();
    final Map<File, ParsingResult> failedToParse = new HashMap<File, ParsingResult>();
    final ParserFactory parserFactory = getParserFactory(rulesData.getType());

    final RulesContext rulesContext = new RulesContext(rulesData, fileStateHolder, failedToParse);

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
    if (processingContext.monitorThread != null) return;

    processingContext.monitorThread = new Thread(new Runnable() {
      public void run() {
        while (!processingContext.finished) {
          for (RulesContext rulesContext : processingContext.rulesContexts) {
            rulesContext.getMonitorRulesCommand().run();
          }

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

  private void finishProcessing(@NotNull final ProcessingContext processingContext) {
    try {
      if (processingContext.monitorThread != null) processingContext.monitorThread.join();

      for (RulesContext rulesContext : processingContext.rulesContexts) {
        for (Future future : rulesContext.getParseTasks()) {
          future.get();
        }

        rulesContext.clearParseTasks();

        rulesContext.getMonitorRulesCommand().run();

        for (Future future : rulesContext.getParseTasks()) {
          future.get();
        }

        logStatistics(rulesContext);
      }
    } catch (Exception e) {
      LoggingUtils.logError("Exception occurred while finishing rules monitoring", e, getBuild().getBuildLogger());
    }
  }

  private void submitParsing(@NotNull File file, @NotNull final RulesContext rulesContext, @NotNull ParserFactory parserFactory) {
    final ParseReportCommand parseReportCommand = new ParseReportCommand(file, rulesContext.getRulesData().getParseReportParameters(),
      rulesContext.getRulesState(), rulesContext.getFailedToParse(), parserFactory);

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
      LoggingUtils.LOG.warn(e);
    }

    if (!executor.isTerminated()) {
      LoggingUtils.LOG.warn("Stopped waiting for one xml-report-plugin executors to complete, it is still running");
      ThreadUtil.threadDump(THREAD_FACTORY);
    }
  }

  private static ExecutorService createExecutor() {
    return Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
  }

  private Rules getRules(@NotNull Map<String, String> parameters) {
    final String rulesStr = XmlReportPluginUtil.getXmlReportPaths(parameters);
    if (rulesStr == null || rulesStr.length() == 0) {
      throw new RuntimeException("Rules are empty");
    }
    final List<String> rules = new ArrayList<String>();
    for (String rule : rulesStr.split(XmlReportPluginConstants.SPLIT_REGEX)) {
      rules.add(FileUtil.resolvePath(getBuild().getCheckoutDirectory(), rule).getPath());
    }
    return new Rules(rules);
  }

  private Rules getRules(@NotNull File rulesFile) {
    final List<String> rules = new ArrayList<String>();
    rules.add(FileUtil.resolvePath(getBuild().getCheckoutDirectory(), rulesFile.getPath()).getPath());
    return new Rules(rules);
  }

  private void logStatistics(@NotNull final RulesContext rulesContext) {
    final BuildProgressLogger logger = getBuild().getBuildLogger();

    LoggingUtils.logInTarget(LoggingUtils.getTypeDisplayName(rulesContext.getRulesData().getType()) + " report watcher",
      new Runnable() {
        public void run() {
          String message = "Stop watching paths:";

          final List<String> rules = rulesContext.getRulesData().getRules().getBody();

          if (rules.size() == 0) {
            message += " <no paths>";
            LoggingUtils.warn(message, logger);
          } else {
            LoggingUtils.message(message, logger);
            for (String r : rules) {
              LoggingUtils.message(r, logger);
            }
          }

          final ParsingResult result = myParserFactoryMap.get(rulesContext.getRulesData().getType()).createEmptyResult();

          final Map<File, ParsingResult> processedFiles = rulesContext.getRulesState().getFiles();
          final Map<File, ParsingResult> failedToParse = rulesContext.getFailedToParse();

          final int totalFileCount = processedFiles.size() + failedToParse.size();

          if (totalFileCount == 0) {
            rulesContext.getRulesData().getWhenNoDataPublished().doLogAction("No reports found", logger, LoggingUtils.LOG);
          } else {
            LoggingUtils.message(totalFileCount + " report" + getEnding(totalFileCount) + " found", logger);

            for (File file : processedFiles.keySet()) {
              if (rulesContext.getRulesData().isVerbose()) {
                logger.message(file + " found");
              }
              result.accumulate(processedFiles.get(file));
            }
          }

          if (!failedToParse.isEmpty()) {
            LoggingUtils.error("Failed to parse " + failedToParse.size() + " report" + getEnding(failedToParse.size()), logger);

            int i = 0;
            for (File file : failedToParse.keySet()) {
              if (rulesContext.getRulesData().isVerbose()) {
                LoggingUtils.logError("Failed to parse " + file, null, logger);
              } else if (i++ < 10) {
                LoggingUtils.LOG.warn("Failed to parse " + file);
              }
              result.accumulate(failedToParse.get(file));
            }
          }

          result.logAsTotalResult(rulesContext.getRulesData().getParseReportParameters());
        }
      }, logger);
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

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  private ProcessingContext getStepProcessingContext() {
    if (myStepProcessingContext == null) {
      throw new IllegalStateException("Step processing context is null");
    }
    return myStepProcessingContext;
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
      return XmlReportPluginUtil.isOutputVerbose(myParameters);
    }

    @NotNull
    public LogAction getWhenNoDataPublished() {
      return LogAction.getAction(XmlReportPluginUtil.whenNoDataPublished(myParameters));
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
          return XmlReportPluginUtil.getReportType(myParameters);
        }

        public boolean isParseOutOfDate() {
          return XmlReportPluginUtil.isParseOutOfDateReports(myParameters);
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
          return XmlReportPluginUtil.isOutputVerbose(myParameters);
        }

        @NotNull
        public BuildProgressLogger getThreadLogger() {
          return getBuild().getBuildLogger().getThreadLogger();
        }

        @NotNull
        public BuildProgressLogger getInternalizingThreadLogger() {
          return isLogAsInternal() ?
            ((MessageTweakingSupport) getThreadLogger()).getTweakedLogger(MessageInternalizer.MESSAGE_INTERNALIZER)
            : getThreadLogger();
        }

        private boolean isLogAsInternal() {
          return XmlReportPluginUtil.isLogIsInternal(myParameters);
        }

        @NotNull
        public InspectionReporter getInspectionReporter() {
          return new TeamCityInspectionReporter(myInspectionReporter, getBuild().getBuildLogger(), getCheckoutDir());
        }

        @NotNull
        public DuplicationReporter getDuplicationReporter() {
          return new TeamCityDuplicationReporter(myDuplicatesReporter, getCheckoutDir());
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
          return XmlReportPluginUtil.getReportType(myParameters);
        }

        @NotNull
        public File getCheckoutDir() {
          return getBuild().getCheckoutDirectory();
        }
      };
    }
  }

  private static final class ProcessingContext {
    private final long startTime;
    private volatile boolean finished;
    @Nullable
    private volatile Thread monitorThread;
    @NotNull
    private final List<RulesContext> rulesContexts;

    private ProcessingContext(@NotNull List<RulesContext> rulesContexts) {
      this.rulesContexts = rulesContexts;
      startTime = new Date().getTime();
      finished = false;
    }
  }
}
