/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.antJUnit;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.tests.TestsParsingResult;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;


public class AntJUnitReportParserTest extends BaseParserTestCase {
  private static final String REPORT_DIR = "junit";

  private static final String SINGLE_CASE_FAILURE = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test ##TIMESTAMP##\n" +
    "TEST FAILED: TestCase.test\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "      at TestCase.test(Unknown Source)\n" +
    "TEST FINISHED: TestCase.test ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  private static final String SINGLE_CASE_FAILURE_EXTRA_SUITE = SINGLE_CASE_FAILURE +
    "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  private static final String TWO_CASES = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FAILED: TestCase.test1\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "      at TestCase.test(Unknown Source)\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
    "TEST FAILED: TestCase.test2\n" +
    "java.lang.NullPointerException: Error message from test\n" +
    "java.lang.NullPointerException:\n" +
    "      Error message from test\n" +
    "      at TestCase.test(Unknown Source)\n" +
    "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  private static final String ONE_LINE_SYSOUT = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "MESSAGE: [System out]\n" +
    "from test1\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  private static final String ONE_LINE_SYSERR = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "WARNING: [System error]\n" +
    "from test1\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";
  private static final String FIVE_LINE_SYSOUT = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "MESSAGE: [System out]\n" +
    "from test1 line1\n" +
    "from test1 line2\n" +
    "from test1 line3\n" +
    "from test1 line4\n" +
    "from test1 line5\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";
  private static final String FIVE_LINE_SYSERR = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "WARNING: [System error]\n" +
    "from test1 line1\n" +
    "from test1 line2\n" +
    "from test1 line3\n" +
    "from test1 line4\n" +
    "from test1 line5\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";
  private static final String TWO_CASES_FAILED = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
    "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST FAILED: TestCase.test1\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "junit.framework.AssertionFailedError: Assertion message form test\n" +
    "      at TestCase.test(Unknown Source)\n" +
    "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
    "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
    "TEST FAILED: TestCase.test2\n" +
    "java.lang.NullPointerException: Error message from test\n" +
    "java.lang.NullPointerException:\n" +
    "      Error message from test\n" +
    "      at TestCase.test(Unknown Source)\n" +
    "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  @NotNull
  protected AntJUnitReportParser getParser() {
    return new AntJUnitReportParser(getXMLReader(), getLogger());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return REPORT_DIR;
  }

  @Test
  public void testEmptyReport() throws Exception {
    final TestsParsingResult result = (TestsParsingResult) parse("empty.xml");

    final int suitesLogged = result.getSuites();
    Assert.assertTrue("Empty reportData contains 0 suites, but " + suitesLogged + " suites logged", suitesLogged == 0);

    final int testsLogged = result.getTests();
    Assert.assertTrue("Empty reportData contains 0 tests, but " + testsLogged + " tests logged", testsLogged == 0);
  }

  @Test
  public void testNoCases() throws Exception {
    parse("noCase.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    parse("singleCaseSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
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
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteBetweenAttrs() throws Exception {
    parse("singleCaseBreakTestSuiteBetweenAttrs.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteAfterAttrs() throws Exception {
    parse("singleCaseBreakTestSuiteAfterAttrs.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseIn2PartsBreakAfterTestSuite() throws Exception {
    parse("singleCaseBreakAfterTestSuite.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsBreakAfterAttrs() throws Exception {
    parse("singleCaseBreakAfterAttrs.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsBreakClosing() throws Exception {
    parse("singleCaseBreakClosing.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test1CaseIn2PartsFrom1TrySuiteFrom2() throws Exception {
    parse("singleCaseBreakAfter.xml", parse("singleCaseFailure.xml"));
    assertResultEquals(
      SINGLE_CASE_FAILURE_EXTRA_SUITE);
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    parse("twoCasesSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    parse("twoCasesFirstSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    parse("twoCasesSecondSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesFailed() throws Exception {
    parse("twoCasesFailed.xml");
    assertResultEquals(
      TWO_CASES);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakBetweenAttrs() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakBetweenAttrs.xml"));
    assertResultEquals(
      TWO_CASES_FAILED);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakFailureST() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakFailureST.xml"));
    assertResultEquals(
      TWO_CASES_FAILED);
  }

  @Test
  public void test2CasesIn2PartsFirstBreakAfterFailure() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesFirstBreakAfterFailure.xml"));
    assertResultEquals(
      TWO_CASES_FAILED);
  }

  @Test
  public void test2CasesIn2PartsBreakAfterFirst() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakAfterFirst.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakBetweenAttrs() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakBetweenAttrs.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureMessage() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorMessage.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakFailureST() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorST.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakErrorClosing() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakErrorClosing.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsSecondBreakClosing() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesSecondBreakClosing.xml"));
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test1\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n" +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST FAILED: TestCase.test2\n" +
        "java.lang.NullPointerException: Error message from test\n" +
        "java.lang.NullPointerException:\n" +
        "      Error message from test\n" +
        "      at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsBothFrom1TrySuiteFrom2() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakAfterSecond.xml"));
    assertResultEquals(
      TWO_CASES +
        "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesIn2PartsBreakHeading() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakHeading.xml"));
    assertResultEquals(
      TWO_CASES);
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuite() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakTestSuite.xml"));
    assertResultEquals(
      TWO_CASES);
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuiteInAttr() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakTestSuiteInAttr.xml"));
    assertResultEquals(
      TWO_CASES);
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
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "MESSAGE: [System out]\n" +
        "out from test1 line1\n" +
        "out from test1 line2\n" +
        "out from test1 line3\n" +
        "out from test1 line4\n" +
        "out from test1 line5\n" +
        "WARNING: [System error]\n" +
        "err from test1 line1\n" +
        "err from test1 line2\n" +
        "err from test1 line3\n" +
        "err from test1 line4\n" +
        "err from test1 line5\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void testLogCaseSystemOut() throws Exception {
    parse("caseWithSystemOut.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STDOUT: TestCase.test1\n" +
        "from test1\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void testLogCaseSystemErr() throws Exception {
    parse("caseWithSystemErr.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STDERR: TestCase.test1\n" +
        "from test1\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void testLog2CasesSystemOut() throws Exception {
    parse("twoCasesWithSystemOut.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STDOUT: TestCase.test1\n" +
        "from test1\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST STDOUT: TestCase.test2\n" +
        "from test2\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
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
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: TestCase.test1 ##TIMESTAMP##\n" +
        "TEST STARTED: TestCase.test2 ##TIMESTAMP##\n" +
        "TEST IGNORED: TestCase.test2\n" +
        "TEST FINISHED: TestCase.test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  //TW-9343 strange class name
  @Test
  public void testSuiteNameEqualsTestName() throws Exception {
    parse("TEST-ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest.xml");
    assertResultEquals(
      "SUITE STARTED: ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest ##TIMESTAMP##\n" +
        "TEST STARTED: ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest ##TIMESTAMP##\n" +
        "TEST IGNORED: ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
        "TEST FINISHED: ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest ##TIMESTAMP##\n" +
        "SUITE FINISHED: ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest ##TIMESTAMP##\n");
  }
}
