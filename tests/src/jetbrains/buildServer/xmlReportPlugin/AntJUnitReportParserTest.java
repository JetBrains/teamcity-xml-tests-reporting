/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
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
import java.io.FileNotFoundException;
import java.util.Date;


@RunWith(JMock.class)
public class AntJUnitReportParserTest extends BaseTestCase {
  private static final String REPORT_DIR = "junit";
  private static final String SUITE_NAME = "TestCase";
  private static final String CASE_CLASSNAME = "TestCase";
  private static final String CASE_NAME = CASE_CLASSNAME + ".test";

  private static final String FAILURE_MESSAGE = "junit.framework.AssertionFailedError: Assertion message form test";
  private static final String ERROR_MESSAGE = "java.lang.NullPointerException: Error message from test";

  private static final String SYSO_MESSAGE1 = "from test1";
  private static final String SYSO_MESSAGE2 = "from test2";

  private static final String REPORT_1 = "report.xml";
  private static final String REPORT_2 = "report1.xml";
  private static final String REPORT_3 = "report2.xml";
  private static final String REPORT_4 = "report3.xml";

  private XmlReportParser myParser;
  private BaseServerLoggerFacade myLogger;

  private Mockery myContext;
  private Sequence mySequence;

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private ReportData reportData(@NotNull final String fileName) throws FileNotFoundException {
    return new ReportData(TestUtil.getTestDataFile(fileName, REPORT_DIR), "junit");
  }

  private File file(@NotNull final String fileName) throws FileNotFoundException {
    return TestUtil.getTestDataFile(fileName, REPORT_DIR);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    myLogger = createBaseServerLoggerFacade();
    myParser = new AntJUnitReportParser(myLogger);
    mySequence = myContext.sequence("Log Sequence");
  }

  @Test
  public void testUnexistingReport() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).exception(with(any(FileNotFoundException.class)));
      }
    });

    myParser.parse(new ReportData(new File("unexisting"), "junit"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testEmptyReport() throws Exception {
    final ReportData data = reportData("empty.xml");
    myParser.parse(data);
    final int testsLogged = data.getProcessedEvents();
    Assert.assertTrue("Empty reportData contains 0 tests, but " + testsLogged + " tests logged", testsLogged == 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testWrongFormatReport() throws Exception {
    myParser.parse(reportData("wrongFormat"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testNoCases() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("noCase.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("singleCaseSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseFailure() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("singleCaseFailure.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseError() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("singleCaseError.xml"));
    myContext.assertIsSatisfied();
  }

  private void singleCaseIn2PartsCaseAndSuiteFrom2Try(String unfinishedReportName) throws Exception {
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file(unfinishedReportName), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("singleCaseFailure.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteBetweenAttrs() throws Exception {
    singleCaseIn2PartsCaseAndSuiteFrom2Try("singleCaseBreakTestSuiteBetweenAttrs.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteAfterAttrs() throws Exception {
    singleCaseIn2PartsCaseAndSuiteFrom2Try("singleCaseBreakTestSuiteAfterAttrs.xml");
  }

  private void singleCaseIn2PartsFrom2TrySuiteFrom1(String unfinishedReportName) throws Exception {
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file(unfinishedReportName), data.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("singleCaseFailure.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIn2PartsBreakAfterTestSuite() throws Exception {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakAfterTestSuite.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakAfterAttrs() throws Exception {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakAfterAttrs.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakClosing() throws Exception {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakClosing.xml");
  }

  @Test
  public void test1CaseIn2PartsFrom1TrySuiteFrom2() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file("singleCaseBreakAfter.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("singleCaseFailure.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesFirstSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesSecondSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFailed() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesFailed.xml"));
    myContext.assertIsSatisfied();
  }

  private void twoCasesIn2PartsBothFrom2TrySuiteFrom1(String unfinishedReportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file(unfinishedReportName), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("twoCasesFailed.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsFirstBreakBetweenAttrs() throws Exception {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakBetweenAttrs.xml");
  }

  @Test
  public void test2CasesIn2PartsFirstBreakFailureST() throws Exception {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakFailureST.xml");
  }

  @Test
  public void test2CasesIn2PartsFirstBreakAfterFailure() throws Exception {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakAfterFailure.xml");
  }

  private void twoCasesIn2PartsSecondFrom2Try(String unfinishedReportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file(unfinishedReportName), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("twoCasesFailed.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsBreakAfterFirst() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesBreakAfterFirst.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakBetweenAttrs() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakBetweenAttrs.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureMessage() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorMessage.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureST() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorST.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakErrorClosing() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorClosing.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakClosing() throws Exception {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakClosing.xml");
  }

  @Test
  public void test2CasesIn2PartsBothFrom1TrySuiteFrom2() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file("twoCasesBreakAfterSecond.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("twoCasesFailed.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  private void twoCasesIn2PartsBothAndSuiteFrom2Try(String unfinishedReportName) throws Exception {
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file(unfinishedReportName), data.getFile());
    myParser.parse(data);
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(FAILURE_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(ERROR_MESSAGE), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("twoCasesFailed.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsBreakHeading() throws Exception {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakHeading.xml");
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuite() throws Exception {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakTestSuite.xml");
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuiteInAttr() throws Exception {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakTestSuiteInAttr.xml");
  }

  @Test
  public void test9CasesIn3Parts() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData(REPORT_1);
    FileUtil.copy(file("nineCasesBreakAfterThird.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFailed(with(any(String.class)), with(FAILURE_MESSAGE), with(any(String.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("nineCasesBreakAfterSixth.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    FileUtil.copy(file("nineCases.xml"), data.getFile());
    myParser.parse(data);
    myContext.assertIsSatisfied();
  }

  private void oneLineSystemOut(String reportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).message(with("[System out]\nfrom test1"));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData(reportName));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintSystemOut() throws Exception {
    oneLineSystemOut("printSystemOut.xml");
  }

  @Test
  public void testPrintlnSystemOut() throws Exception {
    oneLineSystemOut("printlnSystemOut.xml");
  }

  private void oneLineSystemErr(String reportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).warning(with("[System error]\nfrom test1"));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData(reportName));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintSystemErr() throws Exception {
    oneLineSystemErr("printSystemErr.xml");
  }

  @Test
  public void testPrintlnSystemErr() throws Exception {
    oneLineSystemErr("printlnSystemErr.xml");
  }

  private void fiveLineSystemOut(String reportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).message(with("[System out]\n" +
          "from test1 line1\nfrom test1 line2\nfrom test1 line3\nfrom test1 line4\nfrom test1 line5"));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData(reportName));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintFiveLineSystemOut() throws Exception {
    fiveLineSystemOut("printFiveLineSystemOut.xml");
  }

  @Test
  public void testPrintlnFiveLineSystemOut() throws Exception {
    fiveLineSystemOut("printlnFiveLineSystemOut.xml");
  }

  private void fiveLineSystemErr(String reportName) throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).warning(with("[System error]\n" +
          "from test1 line1\nfrom test1 line2\nfrom test1 line3\nfrom test1 line4\nfrom test1 line5"));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData(reportName));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintFiveLineSystemErr() throws Exception {
    fiveLineSystemErr("printFiveLineSystemErr.xml");
  }

  @Test
  public void testPrintlnFiveLineSystemErr() throws Exception {
    fiveLineSystemErr("printlnFiveLineSystemErr.xml");
  }

  @Test
  public void fiveLineSystemOutAndErr() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).message(with("[System out]\n" +
          "out from test1 line1\nout from test1 line2\nout from test1 line3\nout from test1 line4\nout from test1 line5"));
        oneOf(myLogger).warning(with("[System error]\n" +
          "err from test1 line1\nerr from test1 line2\nerr from test1 line3\nerr from test1 line4\nerr from test1 line5"));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("fiveLineSystemOutAndErr.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLogCaseSystemOut() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStdOut(with(CASE_NAME + "1"), with(SYSO_MESSAGE1));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("caseWithSystemOut.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLogCaseSystemErr() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStdErr(with(CASE_NAME + "1"), with(SYSO_MESSAGE1));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("caseWithSystemErr.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLog2CasesSystemOut() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStdOut(with(CASE_NAME + "1"), with(SYSO_MESSAGE1));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStdOut(with(CASE_NAME + "2"), with(SYSO_MESSAGE2));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesWithSystemOut.xml"));
    myContext.assertIsSatisfied();
  }

  // TW-7649: junit tests results being ignored
  @Test
  public void testNoSuites() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("api.brightcove.catalog.AudioFacadeReadServletTest"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).message(with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("api.brightcove.catalog.AudioFacadeReadServletTest"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    final ReportData data = reportData("noSuite.xml");
    myParser.parse(data);
    assertEquals(-1, data.getProcessedEvents());
    myContext.assertIsSatisfied();
  }

  @Test
  public void testManyCasesInManyFilesFromManyTries() throws Exception {
    final ReportData data1 = reportData(REPORT_1);
    FileUtil.copy(file("TestClass0_0_500.xml"), data1.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(500).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(500).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(500).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data1);
    myContext.assertIsSatisfied();

    final ReportData data2 = reportData(REPORT_2);
    FileUtil.copy(file("TestClass1_0.xml"), data2.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass1_0"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(1000).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(1000).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(1000).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass1_0"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data2);
    myContext.assertIsSatisfied();

    final ReportData data3 = reportData(REPORT_3);
    FileUtil.copy(file("TestClass0_1_700.xml"), data3.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass0_1"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(700).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(700).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(700).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass0_1"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data3);
    myContext.assertIsSatisfied();

    FileUtil.copy(file("TestClass0_0_800.xml"), data1.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(300).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(300).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(300).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data1);
    myContext.assertIsSatisfied();

    final ReportData data4 = reportData(REPORT_4);
    FileUtil.copy(file("TestClass1_1.xml"), data4.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass1_1"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(1000).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(1000).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(1000).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass1_1"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data4);
    myContext.assertIsSatisfied();

    FileUtil.copy(file("TestClass0_1.xml"), data3.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass0_1"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(300).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(300).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(300).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass0_1"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data3);
    myContext.assertIsSatisfied();

    FileUtil.copy(file("TestClass0_0.xml"), data1.getFile());
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(200).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(200).of(myLogger).logTestFailed(with(any(String.class)), with(any(String.class)), with(any(String.class)));
        exactly(200).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("org.jetbrains.testProject.TestClass0_0"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(data1);
    myContext.assertIsSatisfied();
  }

  //TW-9343
  @Test
  public void test2CasesFirstSuccessSecondSkipped() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with(CASE_NAME + "2"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("twoCasesFirstSuccessSecondSkipped.xml"));
    myContext.assertIsSatisfied();
  }

  //TW-9343 strange class name
  @Test
  public void testSuiteNameEqualsTestName() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(reportData("TEST-ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest.xml"));
    myContext.assertIsSatisfied();
  }
}