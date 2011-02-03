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

  private volatile boolean myFinished;

  @Nullable
  private volatile XMLReader myXMLReader;

  @NotNull
  private final ExecutorService myParseExecutor;

  @NotNull
  private final List<RulesContext> myRulesContexts = new CopyOnWriteArrayList<RulesContext>();

  @NotNull
  private final Map<String, ParserFactory> myParserFactoryMap;

  @Nullable
  private volatile Thread myMonitorRulesThread;

  public XmlReportPlugin(@NotNull Map<String, ParserFactory> parserFactoryMap,
                         @NotNull EventDispatcher<AgentLifeCycleListener> agentDispatcher,
                         @NotNull InspectionReporter inspectionReporter,
                         @NotNull DuplicatesReporter duplicatesReporter) {
    myParserFactoryMap = parserFactoryMap;

    agentDispatcher.addListener(this);

    myInspectionReporter = inspectionReporter;
    myDuplicatesReporter = duplicatesReporter;

    myParseExecutor = createExecutor();
  }

  @Override
  public void buildStarted(@NotNull AgentRunningBuild runningBuild) {
    myBuild = runningBuild;
  }

  @Override
  public void beforeRunnerStart(@NotNull BuildRunnerContext runner) {
    myStartTime = new Date().getTime();
    myFinished = false;

    final Map<String, String> runnerParameters = runner.getRunnerParameters();

    if (XmlReportPluginUtil.isParsingEnabled(runnerParameters)) {
      final Rules rules = new Rules(getRules(runnerParameters), getBuild().getCheckoutDirectory());
      final RulesData rulesData = new RulesData(rules, runnerParameters);

      startProcessing(rulesData);
    }
  }

  public synchronized void processRules(@NotNull File rulesFile,
                                        @NotNull Map<String, String> params) {
    if (myFinished) return;

    final File checkoutDir = getBuild().getCheckoutDirectory();

    final Rules rules = new Rules(getRules(rulesFile, checkoutDir), checkoutDir);
    final RulesData rulesData = new RulesData(rules, params);

    startProcessing(rulesData);
  }

  @Override
  public synchronized void runnerFinished(@NotNull BuildRunnerContext runner, @NotNull BuildFinishedStatus status) {
    myFinished = true;

    if (myRulesContexts.isEmpty()) return;

    try {
      if (myMonitorRulesThread != null) myMonitorRulesThread.join();

      for (RulesContext rulesContext : myRulesContexts) {

        rulesContext.getMonitorRulesCommand().run();

        for (Future future : rulesContext.getParseTasks()) {
          future.get();
        }

        logStatistics(rulesContext);
      }
    } catch (Exception e) {
      LoggingUtils.logError("Exception occurred while finishing rules monitoring", e, getBuild().getBuildLogger());
    }

    myRulesContexts.clear();
    myMonitorRulesThread = null;
  }

  @Override
  public void buildFinished(@NotNull AgentRunningBuild build, @NotNull BuildFinishedStatus buildStatus) {
    myBuild = null;
  }

  @Override
  public void agentShutdown() {
    shutdownExecutor(myParseExecutor);
  }

  private synchronized void startProcessing(@NotNull final RulesData rulesData) {
    createXMLReader();
    startMonitoring();

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
    myRulesContexts.add(rulesContext);
  }

  private void startMonitoring() {
    if (myMonitorRulesThread != null) return;

    myMonitorRulesThread = new Thread(new Runnable() {
      public void run() {
        while (!myFinished) {
          for (RulesContext rulesContext : myRulesContexts) {
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
    myMonitorRulesThread.start();
  }

  private void submitParsing(@NotNull File file, @NotNull final RulesContext rulesContext, @NotNull ParserFactory parserFactory) {
    final ParseReportCommand parseReportCommand = new ParseReportCommand(file, rulesContext.getRulesData().getParseReportParameters(),
      rulesContext.getRulesState(), rulesContext.getFailedToParse(), parserFactory);

    final Future future = myParseExecutor.submit(new Runnable() {
      public void run() {
        parseReportCommand.run();
      }
    });

    rulesContext.addParseTask(future);
  }

  private void createXMLReader() {
    if (myXMLReader != null) return;

    try {
      myXMLReader = ParserUtils.createXmlReader(false);
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

  private static ExecutorService createExecutor() {
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
            LoggingUtils.message(totalFileCount + " report" + (totalFileCount == 1 ? "" : "s") + " found", logger);

            for (File file : processedFiles.keySet()) {
              if (rulesContext.getRulesData().isVerbose()) {
                logger.message(file + " found");
              }
              result.accumulate(processedFiles.get(file));
            }
          }

          for (File file : failedToParse.keySet()) {
            LoggingUtils.logFailedToParse(file, rulesContext.getRulesData().getType(), null, logger);
            result.accumulate(failedToParse.get(file));
          }

          result.logAsTotalResult(rulesContext.getRulesData().getParseReportParameters());
        }
      }, logger);
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
          return getBuild().getCheckoutDirectory();
        }

        @NotNull
        public File getTempDir() {
          return getBuild().getBuildTempDirectory();
        }
      };
    }
  }
}
