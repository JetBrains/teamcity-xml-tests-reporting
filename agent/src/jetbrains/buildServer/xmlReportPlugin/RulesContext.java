/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import org.jetbrains.annotations.NotNull;

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
  private final List<Future> myParseTasks = new ArrayList<Future>();

  @NotNull
  private MonitorRulesCommand myMonitorRulesCommand;

  public RulesContext(@NotNull XmlReportPlugin.RulesData rulesData,
                      @NotNull RulesState rulesState) {
    myRulesData = rulesData;
    myRulesState = rulesState;
  }

  public void addParseTask(@NotNull Future parseTask) {
    myParseTasks.add(parseTask);
  }

  @NotNull
  public List<Future> getParseTasks() {
    return Collections.unmodifiableList(myParseTasks);
  }

  public void clearParseTasks() {
    myParseTasks.clear();
  }

  public void setMonitorRulesCommand(@NotNull MonitorRulesCommand monitorRulesCommand) {
    myMonitorRulesCommand = monitorRulesCommand;
  }

  @NotNull
  public MonitorRulesCommand getMonitorRulesCommand() {
    return myMonitorRulesCommand;
  }

  @NotNull
  public XmlReportPlugin.RulesData getRulesData() {
    return myRulesData;
  }

  @NotNull
  public RulesState getRulesState() {
    return myRulesState;
  }
}
