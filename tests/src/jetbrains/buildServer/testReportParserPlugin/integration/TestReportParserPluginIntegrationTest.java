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

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin;
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil;
import static jetbrains.buildServer.testReportParserPlugin.TestUtil.*;
import static jetbrains.buildServer.testReportParserPlugin.TestUtil.WORKING_DIR;
import static jetbrains.buildServer.testReportParserPlugin.integration.ReportFactory.*;
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
public class TestReportParserPluginIntegrationTest {
  private static final String REPORTS_DIR = "reportsDir";

  private TestReportParserPlugin myPlugin;
  private AgentRunningBuild myRunningBuild;
  private Map<String, String> myRunnerParams;
  private File myWorkingDir;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
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
        allowing(runningBuild).getBuildTempDirectory();
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
    myFailures = new ArrayList<UnexpectedInvokationException>();
    myTestLogger = new BaseServerLoggerFacadeForTesting(myFailures);

    myRunnerParams = new HashMap<String, String>();
    myWorkingDir = new File(WORKING_DIR);
    removeDir(myWorkingDir);
    myWorkingDir.mkdir();
    myRunningBuild = createAgentRunningBuild(myRunnerParams, myWorkingDir, myTestLogger);
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myPlugin = new TestReportParserPlugin(myEventDispatcher);
    ReportFactory.setWorkingDir(WORKING_DIR);
  }

  private void removeDir(File dir) {
    File[] subDirs = dir.listFiles();
    if ((subDirs == null) || (subDirs.length == 0)) {
      dir.delete();
      return;
    }
    for (int i = 0; i < subDirs.length; ++i) {
      removeDir(subDirs[i]);
    }
  }

  private void isSilentWhenDisabled(BuildFinishedStatus status) {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, EMPTY_REPORT_TYPE);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);
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
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    myRunnerParams.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_DIRS, "reports");
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

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
  public void testWarningWhenDirectoryWasNotActuallyDirectory() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    myRunnerParams.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_DIRS, "reports");
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    ReportFactory.createFile("reports");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  private void warningWhenNoReportsFoundInDirectory(String reportType) {
    createDir(REPORTS_DIR);
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, reportType);
    myRunnerParams.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_DIRS, REPORTS_DIR);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("warning", params));
    myTestLogger.setExpectedSequence(myLogSequence);
  }

  private void warningWhenNoReportsFoundInDirectoryOnlyWrong(String reportType) {
    createDir(REPORTS_DIR);
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, reportType);
    myRunnerParams.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_DIRS, REPORTS_DIR);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("message", params));
    myLogSequence.add(new MethodInvokation("warning", params));
    myLogSequence.add(new MethodInvokation("message", params));
    myTestLogger.setExpectedSequence(myLogSequence);
  }

  @Test
  public void testAntJUnitWarningWhenNoReportsFoundInDirectory() {
    warningWhenNoReportsFoundInDirectory(ANT_JUNIT_REPORT_TYPE);

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
  public void testAntJUnitWarningWhenNoReportsFoundInDirectoryOnlyWrongFile() {
    warningWhenNoReportsFoundInDirectoryOnlyWrong(ANT_JUNIT_REPORT_TYPE);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile(REPORTS_DIR + "\\somefile");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testNUnitWarningWhenNoReportsFoundInDirectory() {
    warningWhenNoReportsFoundInDirectory(NUNIT_REPORT_TYPE);

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
  public void testNUnitWarningWhenNoReportsFoundInDirectoryOnlyWrongFile() {
    warningWhenNoReportsFoundInDirectoryOnlyWrong(NUNIT_REPORT_TYPE);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile(REPORTS_DIR + "\\somefile");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testAntJUnitWarningWhenUnfinishedReportFoundInDirectory() {
    createDir(REPORTS_DIR);
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    myRunnerParams.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_DIRS, REPORTS_DIR);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("message", params));
    myLogSequence.add(new MethodInvokation("logSuiteStarted", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFailed", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", params));
    myLogSequence.add(new MethodInvokation("warning", params));
    myLogSequence.add(new MethodInvokation("message", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createUnfinishedReport(REPORTS_DIR + "\\report", ANT_JUNIT_REPORT_TYPE);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testNotSilentWhenEnabled() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
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

  //test for Gradle bug after fixing
  @Test
  public void testLogSuiteWhenAppearsIn2Files() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);
    TestReportParserPluginUtil.setTestReportDirs(myRunnerParams, "");

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("logSuiteStarted", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", params));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile("suite2", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  //test for Gradle bug after fixing
  @Test
  public void testLogSuiteWhenAppearsIn2FilesOthersMustBeLogged() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);
    TestReportParserPluginUtil.setTestReportDirs(myRunnerParams, "");

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);

    final List<Object> param = new ArrayList<Object>();
    param.add("TestCase1");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    param.remove("TestCase1");
    param.add("TestCase2");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFailed", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    param.remove("TestCase2");
    param.add("TestCase3");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFailed", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile("suite2", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuites>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase2\" tests=\"2\" time=\"0.062\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test1\" time=\"0.031\"/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test2\" time=\"0.031\">\n" +
      "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
      "      junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "    </failure>\n" +
      "  </testcase>\n" +
      "</testsuite>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase1\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase3\" tests=\"2\" time=\"0.062\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase3\" name=\"test1\" time=\"0.031\"/>\n" +
      "  <testcase classname=\"TestCase3\" name=\"test2\" time=\"0.031\">\n" +
      "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
      "      junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "    </failure>\n" +
      "  </testcase>\n" +
      "</testsuite>" +
      "</testsuites>");
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  //test for Gradle bug after fixing
  @Test
  public void testLogSuiteWhenAppearsIn2FilesOthersMustBeLoggedInTwoTries() {
    TestReportParserPluginUtil.enableTestReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    TestReportParserPluginUtil.setVerboseOutput(myRunnerParams, true);
    TestReportParserPluginUtil.setTestReportDirs(myRunnerParams, "");

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);

    final List<Object> param = new ArrayList<Object>();
    param.add("TestCase1");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    param.remove("TestCase1");
    param.add("TestCase2");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFailed", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    param.remove("TestCase2");
    param.add("TestCase3");

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFailed", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    createFile("suite2", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuites>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase2\" tests=\"2\" time=\"0.062\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test1\" time=\"0.031\"/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test2\" time=\"0.031\">\n" +
      "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
      "      junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "    </failure>\n" +
      "  </testcase>\n" +
      "</testsuite>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase ");

    createFile("suite2", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuites>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase2\" tests=\"2\" time=\"0.062\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test1\" time=\"0.031\"/>\n" +
      "  <testcase classname=\"TestCase2\" name=\"test2\" time=\"0.031\">\n" +
      "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
      "      junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "    </failure>\n" +
      "  </testcase>\n" +
      "</testsuite>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase1\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase3\" tests=\"2\" time=\"0.062\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase3\" name=\"test1\" time=\"0.031\"/>\n" +
      "  <testcase classname=\"TestCase3\" name=\"test2\" time=\"0.031\">\n" +
      "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
      "      junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "    </failure>\n" +
      "  </testcase>\n" +
      "</testsuite>" +
      "</testsuites>");

    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }
}