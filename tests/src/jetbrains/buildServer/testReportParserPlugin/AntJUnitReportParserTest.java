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

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.testReportParserPlugin.antJUnit.AntJUnitReportParser;
import junit.framework.Assert;
import junit.framework.TestCase;
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
public class AntJUnitReportParserTest extends TestCase {
  private static final String REPORT_DIR = "Tests/testData/junit/";
  private static final String SUITE_NAME = "TestCase";
  private static final String CASE_CLASSNAME = "TestCase";
  private static final String CASE_NAME = CASE_CLASSNAME + ".test";

  private static final String FAILURE_MESSAGE = "junit.framework.AssertionFailedError: Assertion message form test";
  private static final String ERROR_MESSAGE = "java.lang.NullPointerException: Error message from test";

  private static final String SYSO_MESSAGE1 = "from test1";
  private static final String SYSO_MESSAGE2 = "from test2";

  private TestReportParser myParser;
  private BaseServerLoggerFacade myLogger;

  private Mockery myContext;
  private Sequence mySequence;

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private File report(String name) {
    return new File(REPORT_DIR + name);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    myLogger = createBaseServerLoggerFacade();
    myParser = new AntJUnitReportParser(new TestReportLogger(myLogger, true));
    mySequence = myContext.sequence("Log Sequence");
  }

//    @Test(expected = IllegalArgumentException.class)
//    public void testFirstNullArgument() {
//        myParser = new AntJUnitReportParser(null);
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testNullReport() {
//        myParser.parse(null, 0);
//    }

  @Test
  public void testUnexistingReport() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).exception(with(any(FileNotFoundException.class)));
      }
    });

    myParser.parse(new File("unexisting"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testEm4ptyReport() {
    long testsLogged = myParser.parse(report("empty.xml"), 0);
    Assert.assertTrue("Empty report contains 0 tests, but " + testsLogged + " tests logged", testsLogged == 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testWrongFormatReport() {
    myParser.parse(report("wrongFormat"), 0);
  }

  @Test
  public void testNoCases() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("noCase.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testSingleCaseSuccess() {
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
    myParser.parse(report("singleCaseSuccess.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseFailure() {
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
    myParser.parse(report("singleCaseFailure.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseError() {
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
    myParser.parse(report("singleCaseError.xml"), 0);
    myContext.assertIsSatisfied();
  }

  private void singleCaseIn2PartsCaseAndSuiteFrom2Try(String unfinishedReportName) {
    long testsLogged = myParser.parse(report(unfinishedReportName), 0);
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
    myParser.parse(report("singleCaseFailure.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteBetweenAttrs() {
    singleCaseIn2PartsCaseAndSuiteFrom2Try("singleCaseBreakTestSuiteBetweenAttrs.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteAfterAttrs() {
    singleCaseIn2PartsCaseAndSuiteFrom2Try("singleCaseBreakTestSuiteAfterAttrs.xml");
  }

  private void singleCaseIn2PartsFrom2TrySuiteFrom1(String unfinishedReportName) {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    long testsLogged = myParser.parse(report(unfinishedReportName), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
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
    myParser.parse(report("singleCaseFailure.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIn2PartsBreakAfterTestSuite() {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakAfterTestSuite.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakAfterAttrs() {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakAfterAttrs.xml");
  }

  @Test
  public void test1CaseIn2PartsBreakClosing() {
    singleCaseIn2PartsFrom2TrySuiteFrom1("singleCaseBreakClosing.xml");
  }

  @Test
  public void test1CaseIn2PartsFrom1TrySuiteFrom2() {
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
      }
    });
    long testsLogged = myParser.parse(report("singleCaseBreakAfter.xml"), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseFailure.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSuccess() {
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
    myParser.parse(report("twoCasesSuccess.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFirstSuccess() {
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
    myParser.parse(report("twoCasesFirstSuccess.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSecondSuccess() {
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
    myParser.parse(report("twoCasesSecondSuccess.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFailed() {
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
    myParser.parse(report("twoCasesFailed.xml"), 0);
    myContext.assertIsSatisfied();
  }

  private void twoCasesIn2PartsBothFrom2TrySuiteFrom1(String unfinishedReportName) {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    long testsLogged = myParser.parse(report(unfinishedReportName), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
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
    myParser.parse(report("twoCasesFailed.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsFirstBreakBetweenAttrs() {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakBetweenAttrs.xml");
  }

  @Test
  public void test2CasesIn2PartsFirstBreakFailureST() {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakFailureST.xml");
  }

  @Test
  public void test2CasesIn2PartsFirstBreakAfterFailure() {
    twoCasesIn2PartsBothFrom2TrySuiteFrom1("twoCasesFirstBreakAfterFailure.xml");
  }

  private void twoCasesIn2PartsSecondFrom2Try(String unfinishedReportName) {
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
      }
    });
    long testsLogged = myParser.parse(report(unfinishedReportName), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
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
    myParser.parse(report("twoCasesFailed.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsBreakAfterFirst() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesBreakAfterFirst.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakBetweenAttrs() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakBetweenAttrs.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureMessage() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorMessage.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureST() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorST.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakErrorClosing() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakErrorClosing.xml");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakClosing() {
    twoCasesIn2PartsSecondFrom2Try("twoCasesSecondBreakClosing.xml");
  }

  @Test
  public void test2CasesIn2PartsBothFrom1TrySuiteFrom2() {
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
      }
    });
    long testsLogged = myParser.parse(report("twoCasesBreakAfterSecond.xml"), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("twoCasesFailed.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  private void twoCasesIn2PartsBothAndSuiteFrom2Try(String unfinishedReportName) {
    long testsLogged = myParser.parse(report(unfinishedReportName), 0);
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
    myParser.parse(report("twoCasesFailed.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesIn2PartsBreakHeading() {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakHeading.xml");
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuite() {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakTestSuite.xml");
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuiteInAttr() {
    twoCasesIn2PartsBothAndSuiteFrom2Try("twoCasesBreakTestSuiteInAttr.xml");
  }

  @Test
  public void test9CasesIn3Parts() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);

        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    long testsLogged = myParser.parse(report("nineCasesBreakAfterThird.xml"), 0);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFailed(with(any(String.class)), with(FAILURE_MESSAGE), with(any(String.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    testsLogged = myParser.parse(report("nineCasesBreakAfterSixth.xml"), testsLogged);
    myContext.assertIsSatisfied();
    myContext.checking(new Expectations() {
      {
        exactly(3).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(3).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);

        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("nineCases.xml"), testsLogged);
    myContext.assertIsSatisfied();
  }

  private void oneLineSystemOut(String reportName) {
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
    myParser.parse(report(reportName), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintSystemOut() {
    oneLineSystemOut("printSystemOut.xml");
  }

  @Test
  public void testPrintlnSystemOut() {
    oneLineSystemOut("printlnSystemOut.xml");
  }

  private void oneLineSystemErr(String reportName) {
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
    myParser.parse(report(reportName), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintSystemErr() {
    oneLineSystemErr("printSystemErr.xml");
  }

  @Test
  public void testPrintlnSystemErr() {
    oneLineSystemErr("printlnSystemErr.xml");
  }

  private void fiveLineSystemOut(String reportName) {
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
    myParser.parse(report(reportName), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintFiveLineSystemOut() {
    fiveLineSystemOut("printFiveLineSystemOut.xml");
  }

  @Test
  public void testPrintlnFiveLineSystemOut() {
    fiveLineSystemOut("printlnFiveLineSystemOut.xml");
  }

  private void fiveLineSystemErr(String reportName) {
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
    myParser.parse(report(reportName), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testPrintFiveLineSystemErr() {
    fiveLineSystemErr("printFiveLineSystemErr.xml");
  }

  @Test
  public void testPrintlnFiveLineSystemErr() {
    fiveLineSystemErr("printlnFiveLineSystemErr.xml");
  }

  @Test
  public void fiveLineSystemOutAndErr() {
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
    myParser.parse(report("fiveLineSystemOutAndErr.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLogCaseSystemOut() {
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
    myParser.parse(report("caseWithSystemOut.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLogCaseSystemErr() {
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
    myParser.parse(report("caseWithSystemErr.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testLog2CasesSystemOut() {
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
    myParser.parse(report("twoCasesWithSystemOut.xml"), 0);
    myContext.assertIsSatisfied();
  }
}