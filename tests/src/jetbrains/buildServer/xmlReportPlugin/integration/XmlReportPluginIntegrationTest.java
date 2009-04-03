/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.integration;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.xmlReportPlugin.XmlReportDataProcessor;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil;
import static jetbrains.buildServer.xmlReportPlugin.integration.ReportFactory.*;
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
public class XmlReportPluginIntegrationTest {
  private static final String REPORTS_DIR = "reportsDir";
  private static final String ANT_JUNIT_REPORT_TYPE = "junit";
  private static final String NUNIT_REPORT_TYPE = "nunit";
  private static final String EMPTY_REPORT_TYPE = "";
  private static final String CHECKOUT_DIR = "workingDirForTesting";

  private XmlReportPlugin myPlugin;
  private AgentRunningBuild myRunningBuild;
  private Map<String, String> myRunnerParams;
  private File myCheckoutDir;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
  private BaseServerLoggerFacadeForTesting myTestLogger;
  private List<MethodInvokation> myLogSequence;
  private List<UnexpectedInvokationException> myFailures;

  private Mockery myContext;
  private InspectionReporter myInspectionReporter;

  private InspectionReporter createInspectionReporter() {
    return myContext.mock(InspectionReporter.class);
  }

  private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File checkoutDirFile, final BaseServerLoggerFacade logger) {
    final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
    myContext.checking(new Expectations() {
      {
        allowing(runningBuild).getBuildLogger();
        will(returnValue(logger));
        allowing(runningBuild).getRunnerParameters();
        will(returnValue(runParams));
        allowing(runningBuild).getBuildTempDirectory();
        will(returnValue(checkoutDirFile));
        ignoring(runningBuild);
      }
    });
    return runningBuild;
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery();

    myInspectionReporter = createInspectionReporter();
    myContext.checking(new Expectations() {
      {
        ignoring(myInspectionReporter);
      }
    });

    myLogSequence = new ArrayList<MethodInvokation>();
    myFailures = new ArrayList<UnexpectedInvokationException>();
    myTestLogger = new BaseServerLoggerFacadeForTesting(myFailures);

    myRunnerParams = new HashMap<String, String>();
    myCheckoutDir = new File(CHECKOUT_DIR);
    removeDir(myCheckoutDir);
    myCheckoutDir.mkdir();
    myRunnerParams.put(XmlReportPlugin.CHECKOUT_DIR, myCheckoutDir.getAbsolutePath());
    myRunningBuild = createAgentRunningBuild(myRunnerParams, myCheckoutDir, myTestLogger);
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myPlugin = new XmlReportPlugin(myEventDispatcher, myInspectionReporter);
    ReportFactory.setCheckoutDir(CHECKOUT_DIR);
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

  private static File getFileInCheckoutDir(String name) {
    return new File("workingDirForTesting/" + name);
  }

  private void isSilentWhenDisabled(BuildFinishedStatus status) {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, EMPTY_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "reports");
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);

    final String path = getFileInCheckoutDir("reports").getAbsolutePath();

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    List<Object> params3 = new ArrayList<Object>();
    params3.add("no files found");
    myLogSequence.add(new MethodInvokation("warning", params3));

    List<Object> params4 = new ArrayList<Object>();
    params4.add(path + " couldn't find any matching files");
    myLogSequence.add(new MethodInvokation("warning", params4));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

//  @Test
//  public void testWarningWhenDirectoryWasNotActuallyDirectory() {
//    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
//    myRunnerParams.put(XmlReportPluginUtil.REPORT_DIRS, "reports");
//    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
//
//    final List<Object> params = new ArrayList<Object>();
//    params.add(getFileInWorkingDir("reports").getAbsolutePath() + " is not actually a directory");
//    myLogSequence.add(new MethodInvokation("warning", params));
//    myTestLogger.setExpectedSequence(myLogSequence);
//
//    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
//    ReportFactory.createFile("reports");
//    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
//    myContext.assertIsSatisfied();
//    myTestLogger.checkIfAllExpectedMethodsWereInvoked();
//
//    if (myFailures.size() > 0) {
//      throw myFailures.get(0);
//    }
//  }

  private void warningWhenNoReportsFoundInDirectory(String reportType) {
    createDir(REPORTS_DIR);
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, reportType);
    myRunnerParams.put(XmlReportPluginUtil.REPORT_DIRS, REPORTS_DIR);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);

    final String path = getFileInCheckoutDir(REPORTS_DIR).getAbsolutePath();
    final String report = path + File.separator + "somefile";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    final List<Object> params3 = new ArrayList<Object>();
    params3.add("no files found");
    myLogSequence.add(new MethodInvokation("warning", params3));

    final List<Object> params4 = new ArrayList<Object>();
    params4.add(getFileInCheckoutDir(REPORTS_DIR).getAbsolutePath() + ": no files found");
    myLogSequence.add(new MethodInvokation("warning", params4));
    myTestLogger.setExpectedSequence(myLogSequence);
  }

  private void warningWhenNoReportsFoundInDirectoryOnlyWrong(String reportType) {
    createDir(REPORTS_DIR);
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, reportType);
    myRunnerParams.put(XmlReportPluginUtil.REPORT_DIRS, REPORTS_DIR);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);

    final String path = getFileInCheckoutDir(REPORTS_DIR).getAbsolutePath();
    final String report = path + File.separator + "somefile";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    final List<Object> params3 = new ArrayList<Object>();
    params3.add(report + ": failed to parse with " +
      XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(reportType) + " parser");
    myLogSequence.add(new MethodInvokation("error", params3));

    final List<Object> params4 = new ArrayList<Object>();
    params4.add("1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params4));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add(path + ": 1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params5));

    List<Object> params6 = new ArrayList<Object>();
    params6.add(report + " found");
    myLogSequence.add(new MethodInvokation("message", params6));

    myTestLogger.setExpectedSequence(myLogSequence);
  }

  @Test
  public void testAntJUnitWarningWhenNoReportsFoundInDirectory() {
    warningWhenNoReportsFoundInDirectory(ANT_JUNIT_REPORT_TYPE);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

//  @Test
//  public void testNUnitWarningWhenNoReportsFoundInDirectoryOnlyWrongFile() {
//    warningWhenNoReportsFoundInDirectoryOnlyWrong(NUNIT_REPORT_TYPE);
//
//    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
//    createFile(REPORTS_DIR + "\\somefile");
//    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
//    myContext.assertIsSatisfied();
//    myTestLogger.checkIfAllExpectedMethodsWereInvoked();
//
//    if (myFailures.size() > 0) {
//      throw myFailures.get(0);
//    }
//  }

  @Test
  public void testAntJUnitWarningWhenUnfinishedReportFoundInDirectory() {
    createDir(REPORTS_DIR);
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    myRunnerParams.put(XmlReportPluginUtil.REPORT_DIRS, REPORTS_DIR);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);

    final String path = getFileInCheckoutDir(REPORTS_DIR).getAbsolutePath();
    final String report = path + File.separator + "report";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> threeAnyParams = new ArrayList<Object>();
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFailed", threeAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", twoAnyParams));

    final List<Object> params3 = new ArrayList<Object>();
    params3.add(report + ": failed to parse with " + XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(ANT_JUNIT_REPORT_TYPE) + " parser");
    myLogSequence.add(new MethodInvokation("error", params3));

    final List<Object> params4 = new ArrayList<Object>();
    params4.add("1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params4));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add(path + ": 1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params5));

    List<Object> params6 = new ArrayList<Object>();
    params6.add(report + " found");
    myLogSequence.add(new MethodInvokation("message", params6));

    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("error", params));
    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  //test for Gradle bug after fixing
  //TW-6467: Tests can be reported twice with xml tests reporter (Gradle case)
  @Test
  public void testLogSuiteWhenAppearsIn2Files() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "");

    final List<Object> params = new ArrayList<Object>();
    params.add(MethodInvokation.ANY_VALUE);
    params.add(MethodInvokation.ANY_VALUE);
    myLogSequence.add(new MethodInvokation("logSuiteStarted", params));
    myLogSequence.add(new MethodInvokation("logTestStarted", params));
    myLogSequence.add(new MethodInvokation("logTestFinished", params));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", params));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
  //TW-6467: Tests can be reported twice with xml tests reporter (Gradle case)
  @Test
  public void testLogSuiteWhenAppearsIn2FilesOthersMustBeLogged() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "");

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> threeAnyParams = new ArrayList<Object>();
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> param1 = new ArrayList<Object>();
    param1.add("TestCase1");
    param1.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param1));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param1));

    final List<Object> param2 = new ArrayList<Object>();
    param2.add("TestCase2");
    param2.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param2));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFailed", threeAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param2));

    final List<Object> param3 = new ArrayList<Object>();
    param3.add("TestCase3");
    param3.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param3));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFailed", threeAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param3));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
  //TW-6467: Tests can be reported twice with xml tests reporter (Gradle case)
  @Test
  public void testLogSuiteWhenAppearsIn2FilesOthersMustBeLoggedInTwoTries() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "");

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> threeAnyParams = new ArrayList<Object>();
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);
    threeAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> param1 = new ArrayList<Object>();
    param1.add("TestCase1");
    param1.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param1));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param1));

    final List<Object> param2 = new ArrayList<Object>();
    param2.add("TestCase2");
    param2.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param2));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFailed", threeAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param2));

    final List<Object> param3 = new ArrayList<Object>();
    param3.add("TestCase3");
    param3.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param3));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFailed", threeAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param3));

    myTestLogger.setExpectedSequence(myLogSequence);
    myTestLogger.addNotControlledMethod("message");
    myTestLogger.addNotControlledMethod("warning");

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
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
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testSkipsOldFiles() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "");
    XmlReportPluginUtil.setParseOutOfDateReports(myRunnerParams, false);

    final String path = getFileInCheckoutDir("").getAbsolutePath();
    final String report = path + File.separator + "suite1";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    List<Object> params3 = new ArrayList<Object>();
    params3.add("Found existing files:");
    myLogSequence.add(new MethodInvokation("warning", params3));

    List<Object> params4 = new ArrayList<Object>();
    params4.add(report);
    myLogSequence.add(new MethodInvokation("warning", params4));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add("no files found");
    myLogSequence.add(new MethodInvokation("warning", params5));

    final List<Object> params6 = new ArrayList<Object>();
    params6.add(path + ": no files found");
    myLogSequence.add(new MethodInvokation("warning", params6));

    myTestLogger.setExpectedSequence(myLogSequence);

    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");

    try {
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);

    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testNotSkipsOldFiles() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, ANT_JUNIT_REPORT_TYPE);
    XmlReportPluginUtil.setVerboseOutput(myRunnerParams, true);
    XmlReportPluginUtil.setXmlReportPaths(myRunnerParams, "");
    XmlReportPluginUtil.setParseOutOfDateReports(myRunnerParams, true);

    final String path = getFileInCheckoutDir("").getAbsolutePath();
    final String report = path + File.separator + "suite1";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    List<Object> params3 = new ArrayList<Object>();
    params3.add("Found existing files:");
    myLogSequence.add(new MethodInvokation("warning", params3));

    List<Object> params4 = new ArrayList<Object>();
    params4.add(report);
    myLogSequence.add(new MethodInvokation("warning", params4));

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> param = new ArrayList<Object>();
    param.add("TestCase1");
    param.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add(report + " report processed: 1 suite(s), 1 test(s)");
    myLogSequence.add(new MethodInvokation("message", params5));

    final List<Object> params6 = new ArrayList<Object>();
    params6.add("1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params6));

    final List<Object> params7 = new ArrayList<Object>();
    params7.add(path + ": 1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params7));

    List<Object> params8 = new ArrayList<Object>();
    params8.add(report + " found");
    myLogSequence.add(new MethodInvokation("message", params8));

    myTestLogger.setExpectedSequence(myLogSequence);

    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);

    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testParsingFromServiceMessage() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, "");

    final String path = getFileInCheckoutDir("").getAbsolutePath();
    final String report = path + File.separator + "suite1";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> param = new ArrayList<Object>();
    param.add("TestCase1");
    param.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param));

    final List<Object> params3 = new ArrayList<Object>();
    params3.add(report + " report processed: 1 suite(s), 1 test(s)");
    myLogSequence.add(new MethodInvokation("message", params3));

    final List<Object> params4 = new ArrayList<Object>();
    params4.add("1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params4));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add(path + ": 1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params5));

    List<Object> params6 = new ArrayList<Object>();
    params6.add(report + " found");
    myLogSequence.add(new MethodInvokation("message", params6));

    myTestLogger.setExpectedSequence(myLogSequence);

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);

    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");

    final XmlReportDataProcessor dataProcessor = new XmlReportDataProcessor.JUnitDataProcessor(myPlugin);
    final Map<String, String> args = new HashMap<String, String>();
    args.put(XmlReportDataProcessor.VERBOSE_ARGUMENT, "true");
    dataProcessor.processData(new File(CHECKOUT_DIR), args);

    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);

    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testParsingFromServiceMessageSkipOld() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, "");

    final String path = getFileInCheckoutDir("").getAbsolutePath();
    final String report = path + File.separator + "suite1";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    List<Object> params3 = new ArrayList<Object>();
    params3.add("Found existing files:");
    myLogSequence.add(new MethodInvokation("warning", params3));

    List<Object> params4 = new ArrayList<Object>();
    params4.add(report);
    myLogSequence.add(new MethodInvokation("warning", params4));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add("no files found");
    myLogSequence.add(new MethodInvokation("warning", params5));

    final List<Object> params6 = new ArrayList<Object>();
    params6.add(path + ": no files found");
    myLogSequence.add(new MethodInvokation("warning", params6));

    myTestLogger.setExpectedSequence(myLogSequence);

    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);

    final XmlReportDataProcessor dataProcessor = new XmlReportDataProcessor.JUnitDataProcessor(myPlugin);
    final Map<String, String> args = new HashMap<String, String>();
    args.put(XmlReportDataProcessor.VERBOSE_ARGUMENT, "true");
    dataProcessor.processData(new File(CHECKOUT_DIR), args);

    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);

    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }

  @Test
  public void testParsingFromServiceMessageNotSkipOld() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunnerParams, "");

    final String path = getFileInCheckoutDir("").getAbsolutePath();
    final String report = path + File.separator + "suite1";

    List<Object> params1 = new ArrayList<Object>();
    params1.add("Watching paths: ");
    myLogSequence.add(new MethodInvokation("message", params1));

    List<Object> params2 = new ArrayList<Object>();
    params2.add(path);
    myLogSequence.add(new MethodInvokation("message", params2));

    List<Object> params3 = new ArrayList<Object>();
    params3.add("Found existing files:");
    myLogSequence.add(new MethodInvokation("warning", params3));

    List<Object> params4 = new ArrayList<Object>();
    params4.add(report);
    myLogSequence.add(new MethodInvokation("warning", params4));

    final List<Object> twoAnyParams = new ArrayList<Object>();
    twoAnyParams.add(MethodInvokation.ANY_VALUE);
    twoAnyParams.add(MethodInvokation.ANY_VALUE);

    final List<Object> param2 = new ArrayList<Object>();
    param2.add("TestCase1");
    param2.add(MethodInvokation.ANY_VALUE);

    myLogSequence.add(new MethodInvokation("logSuiteStarted", param2));
    myLogSequence.add(new MethodInvokation("logTestStarted", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logTestFinished", twoAnyParams));
    myLogSequence.add(new MethodInvokation("logSuiteFinished", param2));

    final List<Object> params5 = new ArrayList<Object>();
    params5.add(report + " report processed: 1 suite(s), 1 test(s)");
    myLogSequence.add(new MethodInvokation("message", params5));

    final List<Object> params6 = new ArrayList<Object>();
    params6.add("1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params6));

    final List<Object> params7 = new ArrayList<Object>();
    params7.add(path + ": 1 file(s) found");
    myLogSequence.add(new MethodInvokation("message", params7));

    List<Object> params8 = new ArrayList<Object>();
    params8.add(report + " found");
    myLogSequence.add(new MethodInvokation("message", params8));

    myTestLogger.setExpectedSequence(myLogSequence);

    createFile("suite1", "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
      "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase1\" tests=\"1\" time=\"0.031\"\n" +
      "           timestamp=\"2008-10-30T17:11:25\">\n" +
      "  <properties/>\n" +
      "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
      "</testsuite>");
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);

    final XmlReportDataProcessor dataProcessor = new XmlReportDataProcessor.JUnitDataProcessor(myPlugin);
    final Map<String, String> args = new HashMap<String, String>();
    args.put(XmlReportDataProcessor.VERBOSE_ARGUMENT, "true");
    args.put(XmlReportDataProcessor.PARSE_OUT_OF_DATE_ARGUMENT, "true");
    dataProcessor.processData(new File(CHECKOUT_DIR), args);

    myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED_SUCCESS);
    myEventDispatcher.getMulticaster().buildFinished(BuildFinishedStatus.FINISHED_SUCCESS);

    myContext.assertIsSatisfied();
    myTestLogger.checkIfAllExpectedMethodsWereInvoked();

    if (myFailures.size() > 0) {
      throw myFailures.get(0);
    }
  }
}