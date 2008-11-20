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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesRegister;
import jetbrains.buildServer.util.EventDispatcher;
import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginTest {
  private static final String WORKING_DIR = "workingDir";

  private TestReportParserPlugin myPlugin;
  private Map<String, String> myRunParams;
  private File myWorkingDir;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
  private ServiceMessagesRegister myServiceMessagesRegister;

  private Mockery myContext;
  private Sequence mySequence;

  private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File workingDirFile) {
    final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
    myContext.checking(new Expectations() {
      {
        allowing(runningBuild).getRunnerParameters();
        will(returnValue(runParams));
        inSequence(mySequence);
        allowing(runningBuild).getWorkingDirectory();
        will(returnValue(workingDirFile));
        inSequence(mySequence);
      }
    });
    return runningBuild;
  }

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private ServiceMessagesRegister createServiceMessagesRegister() {
    return myContext.mock(ServiceMessagesRegister.class);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    mySequence = myContext.sequence("Log Sequence");
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myServiceMessagesRegister = createServiceMessagesRegister();
    myContext.checking(new Expectations() {
      {
        ignoring(myServiceMessagesRegister);
      }
    });
    myRunParams = new HashMap<String, String>();
    myWorkingDir = new File(WORKING_DIR);
  }

  private void isSilentWhenDisabled(BuildFinishedStatus status) {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, false);

    final AgentRunningBuild runningBuild = createAgentRunningBuild(myRunParams, myWorkingDir);
    myPlugin = new TestReportParserPlugin(myEventDispatcher, myServiceMessagesRegister);

    myEventDispatcher.getMulticaster().buildStarted(runningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(runningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(status);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testIsSilentWhenDisabledNormalFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_SUCCESS);
  }


  @Test
  public void testIsSilentWhenDisabledInterruptedFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.INTERRUPTED);
  }

  @Test
  public void testIsSilentWhenDisabledDoesNotExistFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_FAILED);
  }

  private void warningWhenZeroReportDirsSize() {
    final AgentRunningBuild runningBuild = createAgentRunningBuild(myRunParams, myWorkingDir);
    final BaseServerLoggerFacade logger = createBaseServerLoggerFacade();
    myContext.checking(new Expectations() {
      {
        oneOf(runningBuild).getBuildLogger();
        will(returnValue(logger));
        inSequence(mySequence);
        oneOf(logger).warning(with(any(String.class)));
        inSequence(mySequence);
      }
    });
    myPlugin = new TestReportParserPlugin(myEventDispatcher, myServiceMessagesRegister);

    myEventDispatcher.getMulticaster().buildStarted(runningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(runningBuild);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testWarningWhenReportDirsNull() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, true);

    warningWhenZeroReportDirsSize();
  }

//    @Test
//    public void testWarningWhenReportDirsEmpty() {
//        TestReportParserPluginUtil.enableTestReportParsing(myRunParams, true);
//        TestReportParserPluginUtil.setTestReportDirs(myRunParams, "");
//
//        warningWhenZeroReportDirsSize();
//    }

  @Test
  public void testIsStoppedWhenDisabled() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, false);

    final AgentRunningBuild runningBuild = createAgentRunningBuild(myRunParams, myWorkingDir);
    myPlugin = new TestReportParserPlugin(myEventDispatcher, myServiceMessagesRegister);

    myEventDispatcher.getMulticaster().buildStarted(runningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(runningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();

    Assert.assertTrue("Plugin must be stopped", myPlugin.isStopped());
  }

  private void isStoppedWhenZeroReportDirsSize() {
    final AgentRunningBuild runningBuild = createAgentRunningBuild(myRunParams, myWorkingDir);
    final BaseServerLoggerFacade logger = createBaseServerLoggerFacade();
    myContext.checking(new Expectations() {
      {
        oneOf(runningBuild).getBuildLogger();
        will(returnValue(logger));
        ignoring(runningBuild);
        ignoring(logger);
      }
    });
    myPlugin = new TestReportParserPlugin(myEventDispatcher, myServiceMessagesRegister);

    myEventDispatcher.getMulticaster().buildStarted(runningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(runningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();

    Assert.assertTrue("Plugin must be stopped", myPlugin.isStopped());
  }

  @Test
  public void testIsStoppedWhenReportDirsEmpty() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, true);

    isStoppedWhenZeroReportDirsSize();
  }

  @Test
  public void testIsStoppedWhenReportDirsNull() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunParams, true);

    isStoppedWhenZeroReportDirsSize();
  }

  //TODO: add tests for failure - must finish work!!!
}