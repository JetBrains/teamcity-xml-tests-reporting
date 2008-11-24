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

package jetbrains.buildServer.testReportParserPlugin.integration;

import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessagesRegister;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginIntegrationTest {
  private static final String WORKING_DIR = "workingDirForTesting";

  private TestReportParserPlugin myPlugin;
  private AgentRunningBuild myRunningBuild;
  private Map<String, String> myRunnerParams;
  private File myWorkingDir;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
  private ServiceMessagesRegister myServiceMessagesRegister;
  private BaseServerLoggerFacadeForTesting myTestLogger;
  private List<MethodInvokation> myLogSequence;
  private List<UnexpectedInvokationException> myFailures;

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

  private ServiceMessagesRegister createServiceMessagesRegister() {
    return myContext.mock(ServiceMessagesRegister.class);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery();

    myLogSequence = new ArrayList<MethodInvokation>();
    myFailures = new ArrayList<UnexpectedInvokationException>();
    myTestLogger = new BaseServerLoggerFacadeForTesting(myFailures);

    myRunnerParams = new HashMap<String, String>();
    myWorkingDir = new File(WORKING_DIR);
    FileUtil.delete(myWorkingDir);
    myWorkingDir.mkdir();
    myRunningBuild = createAgentRunningBuild(myRunnerParams, myWorkingDir, myTestLogger);
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myServiceMessagesRegister = createServiceMessagesRegister();
    myContext.checking(new Expectations() {
      {
        ignoring(myServiceMessagesRegister);
      }
    });
    myPlugin = new TestReportParserPlugin(myEventDispatcher, myServiceMessagesRegister);
  }

  private void isSilentWhenDisabled(BuildFinishedStatus status) {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, false);
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(status);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testIsSilentWhenDisabledFinishedSuccess() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_SUCCESS);
  }

  @Test
  public void testIsSilentWhenDisabledFinishedFailed() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_FAILED);
  }

  @Test
  public void testIsSilentWhenDisabledInterrupted() {
    isSilentWhenDisabled(BuildFinishedStatus.INTERRUPTED);
  }

  @Test
  public void testIsSilentWhenDisabledDoesNotExist() {
    isSilentWhenDisabled(BuildFinishedStatus.DOES_NOT_EXIST);
  }

  @Test
  public void testWarningWhenNoReportDirAppears() {
    myRunnerParams.put(TestReportParserPlugin.TEST_REPORT_DIR_PROPERTY, "reports");
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, true);

    List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  private static void createDir(String name) {
    (new File(WORKING_DIR + "\\" + name)).mkdir();
  }

  private void createFile(String name) {
    final File f = new File(WORKING_DIR + "\\" + name);
    try {
      final FileWriter fw = new FileWriter(f);
      fw.write("File content");
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testWarningWhenDirectoryWasNotActuallyDirectory() {
    myRunnerParams.put(TestReportParserPlugin.TEST_REPORT_DIR_PROPERTY, "reports");
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, true);

    List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile("reports");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

//  @Test
//  public void testWarningWhenNoReportsFoundInDirectoryOnlyWrongFile() {
//    final String dirName = "reportsDir";
//    createDir(dirName);
//    myRunnerParams.put(TestReportParserPlugin.TEST_REPORT_DIR_PROPERTY, "reportsDir");
//    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, true);
//
//    List<Object> params = new ArrayList<Object>();
//    params.add(MethodInvokation.ANY_VALUE);
//    myLogSequence.add(new MethodInvokation("warning", params));
//    myTestLogger.setExpectedSequence(myLogSequence);
//
//    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
//    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
//    createFile(dirName + "\\somefile");
//    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
//    myContext.assertIsSatisfied();
//    myTestLogger.checkIfAllExpectedMethodsWereInvoked();
//
//    if (myFailures.size() > 0) {
//      throw myFailures.get(0);
//    }
//  }

  @Test
  public void testWarningWhenNoReportsFoundInDirectory() {
    final String dirName = "reportsDir";
    createDir(dirName);
    myRunnerParams.put(TestReportParserPlugin.TEST_REPORT_DIR_PROPERTY, "reportsDir");
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, true);

    List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testNotSilentWhenEnabled() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, true);

    List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }
}