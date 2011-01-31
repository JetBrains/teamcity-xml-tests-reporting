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

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.NamedThreadFactory;
import jetbrains.buildServer.util.ThreadUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;


public class XmlReportPlugin extends AgentLifeCycleAdapter implements RulesProcessor {
  @NotNull
  private static final NamedThreadFactory THREAD_FACTORY = new NamedThreadFactory("xml-report-plugin");

  @NotNull
  private final InspectionReporter myInspectionReporter;
  @NotNull
  private final DuplicatesReporter myDuplicatesReporter;

  @Nullable
  private AgentRunningBuild myBuild;

  private long myStartTime;

  @Nullable
  private XMLReader myXMLReader;

  @NotNull
  private final ScheduledExecutorService myMonitorExecutor;

  @NotNull
  private final ScheduledExecutorService myParseExecutor;

  @NotNull
  private final List<RulesContext> myRulesContexts = new ArrayList<RulesContext>();

  @NotNull
  private final Map<String, ParserFactory> myParserFactoryMap;

  public XmlReportPlugin(@NotNull Map<String, ParserFactory> parserFactoryMap,
                         @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull InspectionReporter inspectionReporter,
                         @NotNull DuplicatesReporter duplicatesReporter) {
    myParserFactoryMap = parserFactoryMap;

    agentDispatcher.addListener(this);

    myInspectionReporter = inspectionReporter;
    myDuplicatesReporter = duplicatesReporter;

    myMonitorExecutor = createExecutor();
    myParseExecutor = createExecutor();
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    myBuild = runningBuild;
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    myStartTime = new Date().getTime();

    final AgentRunningBuild build = myBuild;
    assert build != null;
    final File checkoutDir = build.getCheckoutDirectory();

    final Map<String, String> runnerParameters = runner.getRunnerParameters();

    if (XmlReportPluginUtil.isParsingEnabled(runnerParameters)) {
      final Rules rules = new Rules(getRules(runnerParameters), checkoutDir);
      final RulesData rulesData = new RulesData(rules, runnerParameters);

      scheduleProcessing(rulesData);
    }
  }

  public synchronized void processRules(@NotNull File rulesFile,
                                        @NotNull Map<String, String> params) {
    final AgentRunningBuild build = myBuild;
    assert build != null;
    final File checkoutDir = build.getCheckoutDirectory();

    final Rules rules = new Rules(getRules(rulesFile, checkoutDir), checkoutDir);
    final RulesData rulesData = new RulesData(rules, params);

    scheduleProcessing(rulesData);
  }

  @Override
  public void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
    if (myRulesContexts.isEmpty()) return;

    for (final RulesContext rulesContext : myRulesContexts) {

      try {
        rulesContext.getMonitorTask().cancel(false);

        myMonitorExecutor.submit(new Runnable() {
          public void run() {
            waitParseTasksFinish(rulesContext);
            rulesContext.getMonitorRulesCommand().run();
          }
        }).get();

      } catch (Exception e) {
        LoggingUtils.logError("Exception occurred while finishing paths watching",
          e, myBuild.getBuildLogger());
      }

      waitParseTasksFinish(rulesContext);

      logStatistics(rulesContext);
    }

    myRulesContexts.clear();
  }

  @Override
  public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    myBuild = null;
  }

  @Override
  public void agentShutdown() {
    shutdownExecutor(myMonitorExecutor);
    shutdownExecutor(myParseExecutor);
  }

  private synchronized void scheduleProcessing(@NotNull final RulesData rulesData) {
    if (myXMLReader == null) {
      myXMLReader = createXMLReader();
    }

    final RulesFileStateHolder rulesFilesState = new RulesFileStateHolder();
    final Map<File, ParsingResult> failedToParse = new HashMap<File, ParsingResult>();
    final ParserFactory parserFactory = getParserFactory(rulesData.getType());

    final RulesContext rulesContext = new RulesContext(rulesData, rulesFilesState, failedToParse);
    myRulesContexts.add(rulesContext);

    final MonitorRulesCommand monitorRulesCommand = new MonitorRulesCommand(rulesData.getMonitorRulesParameters(), rulesFilesState,
      new MonitorRulesCommand.MonitorRulesListener() {
        public void modificationDetected(@NotNull File file) {
          synchronized (XmlReportPlugin.this) {
            final Future future
              = myParseExecutor.submit(
              new ParseReportCommand(file, rulesData.getParseReportParameters(),
                rulesFilesState, failedToParse, parserFactory));

            rulesContext.addParseTask(future);
          }
        }
      });

    rulesContext.setMonitorRulesCommand(monitorRulesCommand);

    final ScheduledFuture future = myMonitorExecutor.scheduleWithFixedDelay(new Runnable() {
      public void run() {
        monitorRulesCommand.run();
      }
    }, 0L, 200L, TimeUnit.MILLISECONDS);

    rulesContext.setMonitorTask(future);
  }

  private void waitParseTasksFinish(@NotNull RulesContext rulesContext) {
    final AgentRunningBuild build = myBuild;
    assert build != null;
    for (Future future : rulesContext.getParseTasks()) {
      try {
        future.get();
      } catch (Exception e) {
        LoggingUtils.logError("Exception occurred while finishing reports processing", e, build.getBuildLogger());
      }
    }
    rulesContext.clearParseTasks();
  }

  @NotNull
  private XMLReader createXMLReader() {
    try {
      return ParserUtils.createXmlReader(false);
    } catch (SAXException e) {
      throw new RuntimeException("Unable to parse xml, failed to load parser");
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

  private static ScheduledExecutorService createExecutor() {
    return Executors.newSingleThreadScheduledExecutor(THREAD_FACTORY);
  }

  private static Collection<String> getRules(@NotNull Map<String, String> parameters) {
    final String rulesStr = XmlReportPluginUtil.getXmlReportPaths(parameters);
    if (rulesStr == null || rulesStr.length() == 0) {
      throw new RuntimeException("Rules are empty");
    }
    return Arrays.asList(rulesStr.split(XmlReportPluginConstants.SPLIT_REGEX));
  }

  private static Collection<String> getRules(@NotNull File rulesFile, @NotNull File baseDir) {
    final List<String> rules = new ArrayList<String>();
    final String reportPathStr = FileUtil.getRelativePath(baseDir, rulesFile);
    rules.add(reportPathStr == null ? rulesFile.getPath() : reportPathStr);
    return rules;
  }

  public void logStatistics(@NotNull final RulesContext rulesContext) {
    final AgentRunningBuild build = myBuild;
    assert build != null;
    final BuildProgressLogger logger = build.getBuildLogger();

    final ParsingResult result = myParserFactoryMap.get(rulesContext.getRulesData().getType()).createEmptyResult();

    final Map<File, ParsingResult> failedToParse = rulesContext.getFailedToParse();

    for (File file : failedToParse.keySet()) {
      LoggingUtils.logFailedToParse(file, rulesContext.getRulesData().getType(), null, logger);
      result.accumulate(failedToParse.get(file));
    }

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

          final Map<File, ParsingResult> processedFiles = rulesContext.getRulesFilesState().getFiles();

          final int totalFileCount = processedFiles.size() + failedToParse.size();

          if (totalFileCount == 0) {
            rulesContext.getRulesData().getWhenNoDataPublished().doLogAction("No reports found", logger, LoggingUtils.LOG);
          } else {
            LoggingUtils.message(totalFileCount + " report" + (totalFileCount == 1 ? "" : "s") + " found", logger);

            for (File file : processedFiles.keySet()) {
              if (rulesContext.getRulesData().isVerbose()) {
                logger.message(file + " found");
              }
              result.accumulate(processedFiles.get(file));
            }
          }
        }
      }, logger);

      myParserFactoryMap.get(rulesContext.getRulesData().getType()).createResultsProcessor().processTotalResult(result, rulesContext.getRulesData().getParseReportParameters());
  }

  @NotNull
  private ParserFactory getParserFactory(@NotNull String type) {
    if (!myParserFactoryMap.containsKey(type))
      throw new IllegalArgumentException("No factory for " + type);
    return myParserFactoryMap.get(type);
  }

  public class RulesData {
    @NotNull
    private final Rules myRules;

    @NotNull
    private final Map<String, String> myParameters;

    public RulesData(@NotNull Rules rules,
                     @NotNull Map<String, String> parameters) {
      myRules = rules;
      myParameters = parameters;
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
    private MonitorRulesCommand.MonitorRulesParameters getMonitorRulesParameters() {
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
          final AgentRunningBuild build = myBuild;
          assert build != null;
          return build.getBuildLogger().getThreadLogger();
        }
      };
    }

    @NotNull
    private ParseParameters getParseReportParameters() {
      return new ParseParameters() {
        public boolean isVerbose() {
          return XmlReportPluginUtil.isOutputVerbose(myParameters);
        }

        @NotNull
        public BuildProgressLogger getThreadLogger() {
          final AgentRunningBuild build = myBuild;
          assert build != null;
          return build.getBuildLogger().getThreadLogger();
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
          return myInspectionReporter;
        }

        @NotNull
        public DuplicatesReporter getDuplicatesReporter() {
          return myDuplicatesReporter;
        }

        @NotNull
        public Map<String, String> getParameters() {
          return Collections.unmodifiableMap(myParameters);
        }

        @NotNull
        public XMLReader getXmlReader() {
          final XMLReader xmlReader = myXMLReader;
          assert xmlReader != null;
          return xmlReader;
        }

        @NotNull
        public String getType() {
          return XmlReportPluginUtil.getReportType(myParameters);
        }

        @NotNull
        public File getCheckoutDir() {
          final AgentRunningBuild build = myBuild;
          assert build != null;
          return build.getCheckoutDirectory();
        }

        @NotNull
        public File getTempDir() {
          final AgentRunningBuild build = myBuild;
          assert build != null;
          return build.getBuildTempDirectory();
        }
      };
    }
  }
}

