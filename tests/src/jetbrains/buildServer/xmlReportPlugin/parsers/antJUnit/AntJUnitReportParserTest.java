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

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class AntJUnitReportParserTest extends BaseParserTestCase {
  private static final String REPORT_DIR = "junit";

  private static final String SINGLE_CASE_FAILURE = "TestSuite:TestCase\n" +
                                                    "  Test:TestCase.test\n" +
                                                    "    Fail:junit.framework.AssertionFailedError: Assertion message from test Message: junit.framework.AssertionFailedError: Assertion message from test\n" +
                                                    "      at TestCase.test(Unknown Source)\n" +
                                                    "  EndTest:47\n" +
                                                    "------------------------\n" +
                                                    "EndSuite\n";

  private static final String SINGLE_CASE_FAILURE_EXTRA_SUITE = "TestSuite:TestCase\n" +
                                                                "EndSuite\n" + SINGLE_CASE_FAILURE;

  private static final String TWO_CASES_FAILURE = "TestSuite:TestCase\n" +
                                          "  Test:TestCase.test1\n" +
                                          "    Fail:junit.framework.AssertionFailedError: Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
                                          "      at TestCase.test(Unknown Source)\n" +
                                          "  EndTest:31\n" +
                                          "------------------------\n" +
                                          "  Test:TestCase.test2\n" +
                                          "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
                                          "      Error message from test\n" +
                                          "      at TestCase.test(Unknown Source)\n" +
                                          "  EndTest:31\n" +
                                          "------------------------\n" +
                                          "EndSuite\n";

  private static final String FIVE_LINE_SYSOUT = "TestSuite:TestCase\n" +
                                                 "  Test:TestCase.test1\n" +
                                                 "  EndTest:0\n" +
                                                 "------------------------\n" +
                                                 "-->Info: System out from suite TestCase: from test1 line1\n" +
                                                 "from test1 line2\n" +
                                                 "from test1 line3\n" +
                                                 "from test1 line4\n" +
                                                 "from test1 line5\n" +
                                                 "EndSuite\n";
  private static final String FIVE_LINE_SYSERR = "TestSuite:TestCase\n" +
                                                 "  Test:TestCase.test1\n" +
                                                 "  EndTest:0\n" +
                                                 "------------------------\n" +
                                                 "-->Warning: System error from suite TestCase: from test1 line1\n" +
                                                 "from test1 line2\n" +
                                                 "from test1 line3\n" +
                                                 "from test1 line4\n" +
                                                 "from test1 line5\n" +
                                                 "EndSuite\n";
  private static final String TWO_CASES_FAILURE_EXTRA_SUITE = "TestSuite:TestCase\n" +
                                                 "EndSuite\n" + TWO_CASES_FAILURE;
  private static final String TWO_CASES_FAILURE_IN_SEPARATE_SUITE = "TestSuite:TestCase\n" +
  "  Test:TestCase.test1\n" +
  "    Fail:junit.framework.AssertionFailedError: Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
  "      at TestCase.test(Unknown Source)\n" +
  "  EndTest:31\n" +
  "------------------------\n" +
  "EndSuite\n" +
  "TestSuite:TestCase\n" +
  "  Test:TestCase.test2\n" +
  "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
  "      Error message from test\n" +
  "      at TestCase.test(Unknown Source)\n" +
  "  EndTest:31\n" +
  "------------------------\n" +
  "EndSuite\n";

  private static final String ONE_LINE_SYSOUT = "TestSuite:TestCase\n" +
  "  Test:TestCase.test1\n" +
  "  EndTest:0\n" +
  "------------------------\n" +
  "-->Info: System out from suite TestCase: from test1\n" +
  "EndSuite\n";
  private static final String ONE_LINE_SYSERR = "TestSuite:TestCase\n" +
  "  Test:TestCase.test1\n" +
  "  EndTest:0\n" +
  "------------------------\n" +
  "-->Warning: System error from suite TestCase: from test1\n" +
  "EndSuite\n";

  @Override
  @NotNull
  protected AntJUnitReportParser getParser() {
    return new AntJUnitReportParser(getTestReporter(), new SecondDurationParser(), false);
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return REPORT_DIR;
  }

  @Test
  public void testEmptyReport() throws Exception {
    final TestParsingResult result = (TestParsingResult) parse("empty.xml");

    final int suitesLogged = result.getSuites();
    assertEquals(suitesLogged, 0, "Empty reportData contains 0 suites, but " + suitesLogged + " suites logged");

    final int testsLogged = result.getTests();
    assertEquals(testsLogged, 0, "Empty reportData contains 0 tests, but " + testsLogged + " tests logged");
  }

  @Test
  public void testNoCases() throws Exception {
    parse("noCase.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "EndSuite\n");
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    parse("singleCaseSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test1CaseFailure() throws Exception {
    parse("singleCaseFailure.xml");
    assertResultEquals(
      SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseError() throws Exception {
    parse("singleCaseError.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
      "      Error message from test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteBetweenAttrs() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakTestSuiteBetweenAttrs.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteAfterAttrs() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakTestSuiteAfterAttrs.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseIn2PartsBreakAfterTestSuite() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakAfterTestSuite.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsBreakAfterAttrs() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakAfterAttrs.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsBreakClosing() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakClosing.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsFrom1TrySuiteFrom2() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakAfter.xml"));
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    Fail:junit.framework.AssertionFailedError: Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:47\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "TestSuite:TestCase\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    parse("twoCasesSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    parse("twoCasesFirstSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    Fail:junit.framework.AssertionFailedError: Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    parse("twoCasesSecondSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    Fail:junit.framework.AssertionFailedError: Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesFailed() throws Exception {
    parse("twoCasesFailed.xml");
    assertResultEquals(
      TWO_CASES_FAILURE);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakBetweenAttrs() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakBetweenAttrs.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakFailureST() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakFailureST.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakAfterFailure() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakAfterFailure.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test2CasesIn2PartsBreakAfterFirst() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakAfterFirst.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsSecondBreakBetweenAttrs() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakBetweenAttrs.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureMessage() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorMessage.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureST() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorST.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsSecondBreakErrorClosing() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorClosing.xml"));
    assertResultEquals(TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsSecondBreakClosing() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakClosing.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE_IN_SEPARATE_SUITE);
  }

  @Test
  public void test2CasesIn2PartsBothFrom1TrySuiteFrom2() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakAfterSecond.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE +
        "TestSuite:TestCase\n" +
        "EndSuite\n");
  }

  @Test
  public void test2CasesIn2PartsBreakHeading() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakHeading.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE);
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuite() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakTestSuite.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE);
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuiteInAttr() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakTestSuiteInAttr.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE);
  }

  @Test
  public void test9CasesIn3Parts() throws Exception {
    parse("nineCases.xml", parse("nineCasesBreakAfterSixth.xml", parse("nineCasesBreakAfterThird.xml")));
    assertResultEquals(
      getExpectedResult("nineCases.gold"));
  }

  @Test
  public void testPrintSystemOut() throws Exception {
    parse("printSystemOut.xml");
    assertResultEquals(
      ONE_LINE_SYSOUT);
  }

  @Test
  public void testPrintlnSystemOut() throws Exception {
    parse("printlnSystemOut.xml");
    assertResultEquals(
      ONE_LINE_SYSOUT);
  }

  @Test
  public void testPrintSystemErr() throws Exception {
    parse("printSystemErr.xml");
    assertResultEquals(
      ONE_LINE_SYSERR);
  }

  @Test
  public void testPrintlnSystemErr() throws Exception {
    parse("printlnSystemErr.xml");
    assertResultEquals(
      ONE_LINE_SYSERR);
  }

  @Test
  public void testPrintFiveLineSystemOut() throws Exception {
    parse("printFiveLineSystemOut.xml");
    assertResultEquals(
      FIVE_LINE_SYSOUT);
  }

  @Test
  public void testPrintlnFiveLineSystemOut() throws Exception {
    parse("printlnFiveLineSystemOut.xml");
    assertResultEquals(
      FIVE_LINE_SYSOUT);
  }

  @Test
  public void testPrintFiveLineSystemErr() throws Exception {
    parse("printFiveLineSystemErr.xml");
    assertResultEquals(
      FIVE_LINE_SYSERR);
  }

  @Test
  public void testPrintlnFiveLineSystemErr() throws Exception {
    parse("printlnFiveLineSystemErr.xml");
    assertResultEquals(
      FIVE_LINE_SYSERR);
  }

  @Test
  public void testFiveLineSystemOutAndErr() throws Exception {
    parse("fiveLineSystemOutAndErr.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "-->Info: System out from suite TestCase: out from test1 line1\n" +
      "out from test1 line2\n" +
      "out from test1 line3\n" +
      "out from test1 line4\n" +
      "out from test1 line5\n" +
      "-->Warning: System error from suite TestCase: err from test1 line1\n" +
      "err from test1 line2\n" +
      "err from test1 line3\n" +
      "err from test1 line4\n" +
      "err from test1 line5\n" +
      "EndSuite\n");
  }

  @Test
  public void testLogCaseSystemOut() throws Exception {
    parse("caseWithSystemOut.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    StdOutput:from test1\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void testLogCaseSystemErr() throws Exception {
    parse("caseWithSystemErr.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    ErrOutput:from test1\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void testLog2CasesSystemOut() throws Exception {
    parse("twoCasesWithSystemOut.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    StdOutput:from test1\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    StdOutput:from test2\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  // TW-7649: junit tests results being ignored
  @Test
  public void testNoSuites() throws Exception {
    parse("noSuite.xml");
    assertResultEquals(
      getExpectedResult("noSuites.gold"));
  }

  // TW-9343
  @Test
  public void test2CasesFirstSuccessSecondSkipped() throws Exception {
    parse("twoCasesFirstSuccessSecondSkipped.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    Ignored:\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  //TW-9343 strange class name
  @Test
  public void testSuiteNameEqualsTestName() throws Exception {
    parse("TEST-ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest.xml");
    assertResultEquals(
      "TestSuite:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "  Test:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "    Ignored:\n" +
      "  EndTest:10\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  //status attribute
  @Test
  public void test2CasesFirstSuccessSecondSkipped_gtest() throws Exception {
    parse("twoCasesFirstSuccessSecondSkipped_gtest.xml");
    assertResultEquals(
      "TestSuite:MathTest\n" +
      "  Test:Addition\n" +
      "    Fail:Value of: add(1, 1)\n" +
      " Actual: 3\n" +
      "Expected: 2 Message: \n" +
      "  EndTest:7000\n" +
      "------------------------\n" +
      "  Test:Subtraction\n" +
      "  EndTest:5000\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "TestSuite:LogicTest\n" +
      "  Test:NonContradiction\n" +
      "    Ignored:\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  // TW-15430
  @Test
  public void testNestedSuites() throws Exception {
    parse("nestedSuites.xml");
    assertResultEquals("TestSuite:\n" +
                       "TestSuite:Unit Tests\n" +
                       "TestSuite:BootstrapTest\n" +
                       "  Test:test__construct\n" +
                       "  EndTest:51\n" +
                       "------------------------\n" +
                       "  Test:testGetMemcacheOptions\n" +
                       "  EndTest:47\n" +
                       "------------------------\n" +
                       "  Test:testInitApplicationRegistry\n" +
                       "  EndTest:49\n" +
                       "------------------------\n" +
                       "EndSuite\n" +
                       "EndSuite\n" +
                       "TestSuite:Functional Tests\n" +
                       "TestSuite:SeleniumTest\n" +
                       "  Test:testTitle\n" +
                       "    Fail:PHPUnit_Framework_Exception Message: SeleniumTest::testTitle\n" +
                       "PHPUnit_Framework_Exception: Could not connect to the Selenium RC server.\n" +
                       "  EndTest:209\n" +
                       "------------------------\n" +
                       "  Test:testTitle\n" +
                       "    Fail:PHPUnit_Framework_Exception Message: SeleniumTest::testTitle\n" +
                       "PHPUnit_Framework_Exception: Could not connect to the Selenium RC server.\n" +
                       "  EndTest:2\n" +
                       "------------------------\n" +
                       "EndSuite\n" +
                       "EndSuite\n" +
                       "EndSuite\n");
  }

  // http://devnet.jetbrains.net/message/5456287
  @Test
  public void testLogInternalSystemError() throws Exception {
    parse(new AntJUnitReportParser(getTestReporter(), new SecondDurationParser(), true), "printSystemErr.xml");
    assertResultEquals("TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "-->Info: System error from suite TestCase: from test1\n" +
      "EndSuite\n");
  }

  @Test
  public void testNUnitReport_TW_33521() throws Exception {
    parse(new AntJUnitReportParser(getTestReporter(), new SecondDurationParser(), true), "nunit.xml");
    assertResultEquals("-->Error: File ##BASE_DIR##/nunit.xml doesn't match the expected format: \"testsuites\" or \"testsuite\" root element expected\n" +
                       "Please check Ant JUnit Task binaries for the supported DTD\n");
  }
  @Test
  public void emptyStatus() throws Exception {
    parse("emptyStatus.xml");
    assertResultEquals("TestSuite:\n" +
                       "  Test:Test.test0\n" +
                       "  EndTest:14374\n" +
                       "------------------------\n" +
                       "  Test:Test.test1\n" +
                       "  EndTest:3566\n" +
                       "------------------------\n" +
                       "  Test:Test.test2\n" +
                       "  EndTest:2020\n" +
                       "------------------------\n" +
                       "  Test:Test.test3\n" +
                       "  EndTest:2023\n" +
                       "------------------------\n" +
                       "  Test:Test.test4\n" +
                       "  EndTest:65629\n" +
                       "------------------------\n" +
                       "EndSuite\n");
  }
}
