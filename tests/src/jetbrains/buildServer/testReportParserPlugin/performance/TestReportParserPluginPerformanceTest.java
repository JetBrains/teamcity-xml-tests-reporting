/*
 * Copyright 2008 JetBrains s.r.o.
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

package jetbrains.buildServer.testReportParserPlugin.performance;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin;
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil;
import jetbrains.buildServer.util.EventDispatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginPerformanceTest {
  private static final String WORKING_DIR = "workingDir";

  private TestReportParserPlugin myPlugin;
  private AgentRunningBuild myRunningBuild;
  private Map<String, String> myRunParams;
  private File myWorkingDir;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
  private BaseServerLoggerFacadeForTesting myTestLogger;
  private List<MethodInvokation> myLogSequence;
  private List<UnexpectedInvokationException> myFailure;

  private Mockery myContext;

  private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File workingDirFile, final BaseServerLoggerFacade logger) {
    final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
    myContext.checking(new Expectations() {
      {
        allowing(runningBuild).getBuildLogger();
        will(returnValue(logger));
        allowing(runningBuild).getRunnerParameters();
        will(returnValue(runParams));
        allowing(runningBuild).getWorkingDirectory();
        will(returnValue(workingDirFile));
        ignoring(runningBuild);
      }
    });
    return runningBuild;
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery();

    myLogSequence = new ArrayList<MethodInvokation>();
    myFailure = new ArrayList<UnexpectedInvokationException>();
    myTestLogger = new BaseServerLoggerFacadeForTesting(myFailure);

    myRunParams = new HashMap<String, String>();
    myWorkingDir = new File(WORKING_DIR);
    myRunningBuild = createAgentRunningBuild(myRunParams, myWorkingDir, myTestLogger);
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myPlugin = new TestReportParserPlugin(myEventDispatcher);
  }

  @Test
  public void testIsSilentWhenDisabled() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, false);
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();

    if (myFailure.size() > 0) {
      throw myFailure.get(0);
    }
  }

  @Test
  public void testNotSilentWhenEnabled() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, true);

    List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailure.size() > 0) {
      throw myFailure.get(0);
    }
  }
}