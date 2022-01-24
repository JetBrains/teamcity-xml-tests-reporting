/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.xmlReportPlugin.ParserFactory.ParsingStage.BEFORE_FINISH;

/**
 * User: vbedrosova
 * Date: 20.01.11
 * Time: 12:59
 */
public class RulesContext {
  @NotNull
  private final XmlReportPlugin.RulesData myRulesData;

  @NotNull
  private final RulesState myRulesState;

  @NotNull
  private final Map<ParserFactory.ParsingStage, List<ExecuteTask>> myExecutedTasks = new EnumMap<ParserFactory.ParsingStage, List<ExecuteTask>>(ParserFactory.ParsingStage.class);

  @NotNull
  private MonitorRulesCommand myMonitorRulesCommand;

  public RulesContext(@NotNull XmlReportPlugin.RulesData rulesData,
                      @NotNull RulesState rulesState) {
    myRulesData = rulesData;
    myRulesState = rulesState;
    for (ParserFactory.ParsingStage stage : ParserFactory.ParsingStage.values()) {
      myExecutedTasks.put(stage, new ArrayList<ExecuteTask>());
    }
  }

  public void addParseTask(@NotNull final ExecutorService executor, @NotNull final ParseReportCommand command) {
    final ExecuteTask task = new CommandTask(executor, command);
    final ParserFactory.ParsingStage stage = command.getParsingStage();
    switch (stage) {
      case RUNTIME:
        task.start();
        break;
      case BEFORE_FINISH:
        break;
    }
    myExecutedTasks.get(stage).add(task);
  }

  public void addParseFactory(@NotNull final ParserFactory factory) {
    myExecutedTasks.get(factory.getParsingStage()).add(new FactoryTask(factory, getRulesData(), getRulesState()));
  }

  public void finish() throws ExecutionException, InterruptedException {
    for (ExecuteTask task : myExecutedTasks.get(BEFORE_FINISH)) {
      task.start();
    }
    for (List<ExecuteTask> tasks : myExecutedTasks.values()) {
      for (ExecuteTask task : tasks) {
        task.join();
      }
    }
  }

  public void waitRuntimeParsing() throws ExecutionException, InterruptedException {
    for (ExecuteTask task : myExecutedTasks.get(ParserFactory.ParsingStage.RUNTIME)) {
      task.join();
    }
  }

  public void clearRuntimeParseTasks() {
    myExecutedTasks.get(ParserFactory.ParsingStage.RUNTIME).clear();
  }

  @Nullable
  public MonitorRulesCommand getMonitorRulesCommand() {
    return myMonitorRulesCommand;
  }

  public void setMonitorRulesCommand(@NotNull MonitorRulesCommand monitorRulesCommand) {
    myMonitorRulesCommand = monitorRulesCommand;
  }

  @NotNull
  public XmlReportPlugin.RulesData getRulesData() {
    return myRulesData;
  }

  @NotNull
  public RulesState getRulesState() {
    return myRulesState;
  }

  private interface ExecuteTask {
    void start();

    void join() throws ExecutionException, InterruptedException;
  }

  private static class FactoryTask implements ExecuteTask {

    @NotNull
    private final ParserFactory myFactory;
    @NotNull
    private final XmlReportPlugin.RulesData myRulesData;
    @NotNull
    private final RulesState myRulesState;

    public FactoryTask(@NotNull final ParserFactory factory, @NotNull final XmlReportPlugin.RulesData rulesData, @NotNull final RulesState rulesState) {
      myFactory = factory;
      myRulesData = rulesData;
      myRulesState = rulesState;
    }

    @Override
    public void start() {
      for (File file : myRulesData.getMonitorRulesParameters().getRules().collectFiles()) {
        final ParseReportCommand command = new ParseReportCommand(file, myRulesData.getParseReportParameters(), myRulesState, myFactory);
        command.run();
      }
    }

    @Override
    public void join() {
    }
  }

  private static class CommandTask implements ExecuteTask {
    @NotNull private final ParseReportCommand myCommand;
    @NotNull private final ExecutorService myExecutor;
    @Nullable private Future<?> myParseTask;

    public CommandTask(@NotNull final ExecutorService executor, @NotNull final ParseReportCommand command) {
      myCommand = command;
      myExecutor = executor;
    }

    @Override
    public void start() {
      synchronized (myExecutor) {
        myParseTask = myExecutor.submit(myCommand);
      }
    }

    @Override
    public void join() throws ExecutionException, InterruptedException {
      if (myParseTask != null) myParseTask.get();
    }
  }
}
