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

package jetbrains.buildServer.xmlReportPlugin.parsers.testng;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.tests.MillisecondDurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class TestNGReportParserTest extends BaseParserTestCase {
  private static final String REPORT_DIR = "testng";
  private static final String SINGLE_CASE_FAILURE = "TestSuite:Surefire suite\n" +
                                                    "TestSuite:Surefire test\n" +
                                                    "TestSuite:TestCase\n" +
                                                    "  Test:TestCase.test\n" +
                                                    "    Fail:org.testng.Assert.fail: Assertion message from test Message: org.testng.Assert.fail: Assertion message from test\n" +
                                                    "      at TestCase.test(Unknown Source)\n" +
                                                    "  EndTest:47\n" +
                                                    "------------------------\n" +
                                                    "EndSuite\n" +
                                                    "EndSuite\n" +
                                                    "EndSuite\n";
  private static final String SINGLE_CASE_FAILURE_EXTRA_SUITE = "TestSuite:Surefire suite\n" +
                                                                "TestSuite:Surefire test\n" +
                                                                "EndSuite\n" +
                                                                "EndSuite\n" + SINGLE_CASE_FAILURE;

  private static final String TWO_CASES_FAILURE = "TestSuite:Surefire suite\n" +
                                                  "TestSuite:Surefire test\n" +
                                                  "TestSuite:TestCase\n" +
                                                  "  Test:TestCase.test1\n" +
                                                  "    Fail:org.testng.Assert.fail: Assertion message from test Message: org.testng.Assert.fail: Assertion message from test\n" +
                                                  "      at TestCase.test(Unknown Source)\n" +
                                                  "  EndTest:5\n" +
                                                  "------------------------\n" +
                                                  "  Test:TestCase.test2\n" +
                                                  "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
                                                  "      Error message from test\n" +
                                                  "      at TestCase.test(Unknown Source)\n" +
                                                  "  EndTest:47\n" +
                                                  "------------------------\n" +
                                                  "EndSuite\n" +
                                                  "EndSuite\n" +
                                                  "EndSuite\n";

  private static final String TWO_CASES_FAILURE_EXTRA_SUITE = "TestSuite:Surefire suite\n" +
                                                              "TestSuite:Surefire test\n" +
                                                              "TestSuite:TestCase\n" +
                                                              "EndSuite\n" +
                                                              "EndSuite\n" +
                                                              "EndSuite\n" + TWO_CASES_FAILURE;

  private static final String TWO_CASES_FAILURE_IN_SEPARATE_SUITE = "TestSuite:Surefire suite\n" +
                                                                    "TestSuite:Surefire test\n" +
                                                                    "TestSuite:TestCase\n" +
                                                                    "  Test:TestCase.test1\n" +
                                                                    "    Fail:org.testng.Assert.fail: Assertion message fORm test Message: org.testng.Assert.fail: Assertion message fORm test\n" +
                                                                    "      at TestCase.test(Unknown Source)\n" +
                                                                    "  EndTest:5\n" +
                                                                    "------------------------\n" +
                                                                    "EndSuite\n" +
                                                                    "EndSuite\n" +
                                                                    "EndSuite\n" +
                                                                    "TestSuite:Surefire suite\n" +
                                                                    "TestSuite:Surefire test\n" +
                                                                    "TestSuite:TestCase\n" +
                                                                    "  Test:TestCase.test2\n" +
                                                                    "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
                                                                    "      Error message from test\n" +
                                                                    "      at TestCase.test(Unknown Source)\n" +
                                                                    "  EndTest:47\n" +
                                                                    "------------------------\n" +
                                                                    "EndSuite\n" +
                                                                    "EndSuite\n" +
                                                                    "EndSuite\n";

  @NotNull
  @Override
  protected Parser getParser() {
    return new TestNGReportParser(getTestReporter(), new MillisecondDurationParser(), false);
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return REPORT_DIR;
  }

  @Test
  public void testEmptyReport() throws Exception {
    final TestParsingResult result = (TestParsingResult)parse("empty.xml");

    final int suitesLogged = result.getSuites();
    assertEquals(suitesLogged, 0, "Empty reportData contains 0 suites, but " + suitesLogged + " suites logged");

    final int testsLogged = result.getTests();
    assertEquals(testsLogged, 0, "Empty reportData contains 0 tests, but " + testsLogged + " tests logged");
  }

  @Test
  public void testNoCases() throws Exception {
    parse("noCase.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    parse("singleCaseSuccess.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
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
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    Fail:java.lang.NullPointerException: Error message from test Message: java.lang.NullPointerException:\n" +
      "      Error message from test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:5\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
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
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n" + SINGLE_CASE_FAILURE);
  }

  @Test
  public void test1CaseIn2PartsFrom1TrySuiteFrom2() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakAfter.xml"));
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    Fail:org.testng.Assert.fail: Assertion message fORm test Message: org.testng.Assert.fail: Assertion message fORm test\n" +
      "      at TestCase.test(Unknown Source)\n" +
      "  EndTest:47\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    parse("twoCasesSuccess.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:5\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "  EndTest:2\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    parse("twoCasesFirstSuccess.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:6\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    Fail:java.lang.AssertionError: Assertion message form test Message: java.lang.AssertionError: Assertion message form test\n" +
      "    at org.testng.Assert.fail(Assert.java:94)\n" +
      "    at TestCase.test2(TestCase.java:10)\n" +
      "  EndTest:1\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    parse("twoCasesSecondSuccess.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    Fail:java.lang.AssertionError: Assertion message form test Message: java.lang.AssertionError: Assertion message form test\n" +
      "    at org.testng.Assert.fail(Assert.java:94)\n" +
      "    at TestCase.test2(TestCase.java:10)\n" +
      "  EndTest:6\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "  EndTest:1\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
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
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesIn2PartsBreakTestSuite() throws Exception {
    parse("twoCasesFailed.xml", parse("twoCasesBreakTestSuite.xml"));
    assertResultEquals(
      TWO_CASES_FAILURE);
  }

  @Test
  public void testLogCaseSystemOut() throws Exception {
    parse("caseWithSystemOut.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    StdOutput:from test\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void testPrintlnFiveLineSystemOut() throws Exception {
    parse("caseFiveLineSystemOut.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test\n" +
      "    StdOutput:from test line1\n" +
      "from test line2\n" +
      "from test line3\n" +
      "from test line4\n" +
      "from test line5\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void testLog2CasesSystemOut() throws Exception {
    parse("twoCasesWithSystemOut.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "    StdOutput:from test1\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    StdOutput:from test2\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test3CasesFirstSuccessSecondAndThirdSkipped() throws Exception {
    parse("threeCasesFirstSuccessSecondAndThirdSkipped.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test1\n" +
      "  EndTest:5\n" +
      "------------------------\n" +
      "  Test:TestCase.test2\n" +
      "    Ignored:skip test2\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "  Test:TestCase.test3\n" +
      "    Ignored:\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void testSuiteNameEqualsTestName() throws Exception {
    parse("singleCaseSuiteNameEqualsTestName.xml");
    assertResultEquals(
      "TestSuite:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "TestSuite:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "TestSuite:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "  Test:ru.rambler.xmpp.server.core.cm.JDBCPgPersistenceManagerImplTest\n" +
      "  EndTest:31\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2SuccessWithDifferentParams() throws Exception {
    parse("twoCasesSuccessWithDifferentParams.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test(\"true\", \"testng: 1\")\n" +
      "  EndTest:4\n" +
      "------------------------\n" +
      "  Test:TestCase.test(\"false\", \"testng: 2\")\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2FirstSuccessWithDifferentParams() throws Exception {
    parse("twoCasesFirstSuccessWithDifferentParams.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test(\"true\", \"testng: 1\")\n" +
      "  EndTest:7\n" +
      "------------------------\n" +
      "  Test:TestCase.test(\"false\", \"testng: 2\")\n" +
      "    Fail:java.lang.AssertionError: testng: 2 expected [true] but found [false] Message: java.lang.AssertionError: testng: 2 expected [true] but found [false]\n" +
      "\tat org.testng.Assert.fail(Assert.java:94)\n" +
      "  EndTest:1\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test2FirstSuccessWithMixedDifferentParams() throws Exception {
    parse("twoCasesFirstSuccessWithMixedDifferentParams.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test(\"true\", \"testng: 1\")\n" +
      "  EndTest:7\n" +
      "------------------------\n" +
      "  Test:TestCase.test(\"false\", \"testng: 2\")\n" +
      "    Fail:java.lang.AssertionError: testng: 2 expected [true] but found [false] Message: java.lang.AssertionError: testng: 2 expected [true] but found [false]\n" +
      "\tat org.testng.Assert.fail(Assert.java:94)\n" +
      "  EndTest:1\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test1MixedParams() throws Exception {
    parse("singleCasesMixedParams.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test(\"param: 0\", \"param: 1\", \"param: 2\", \"param: 3\", null, \"param: 5\", \"param: 6\")\n" +
      "  EndTest:7\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }

  @Test
  public void test1MixedParams2() throws Exception {
    parse("singleCasesMixedParams2.xml");
    assertResultEquals(
      "TestSuite:Surefire suite\n" +
      "TestSuite:Surefire test\n" +
      "TestSuite:TestCase\n" +
      "  Test:TestCase.test(\"param: 0\", \"param: 1\", \"param: 2\", \"param: 3\", \"param: 4\", \"param: 5\")\n" +
      "  EndTest:7\n" +
      "------------------------\n" +
      "EndSuite\n" +
      "EndSuite\n" +
      "EndSuite\n");
  }
}
