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

package jetbrains.buildServer.xmlReportPlugin.nUnit;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.TestsParsingResult;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;


public class NUnitReportParserTest extends BaseParserTestCase {
  private static final String REPORT_DIR = "nunit";

  private static final String SINGLE_CASE_IGNORED = "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
    "TEST STARTED: test ##TIMESTAMP##\n" +
    "TEST IGNORED: test\n" +
    "TEST FINISHED: test ##TIMESTAMP##\n" +
    "SUITE FINISHED: TestCase ##TIMESTAMP##\n";

  private File myBaseFolder;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myBaseFolder = FileUtil.createTempDirectory("", "");
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    FileUtil.delete(myBaseFolder);
  }

  @NotNull
  @Override
  protected Parser getParser() {
    return new NUnitReportParser(getXMLReader(), getLogger(), "nunit-to-junit.xsl", myBaseFolder);
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
  public void testSingleCaseSuccess() throws Exception {
    parse("singleCaseSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: test ##TIMESTAMP##\n" +
        "TEST FINISHED: test ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

 @Test
  public void test1CaseFailure() throws Exception {
   parse("singleCaseFailure.xml");
   assertResultEquals(
     "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
       "TEST STARTED: test ##TIMESTAMP##\n" +
       "TEST FAILED: test\n" +
       "Assertion message form test\n" +
       "junit.framework.AssertionFailedError: Assertion message form test\n" +
       "            at TestCase.test(Unknown Source)\n" +
       "TEST FINISHED: test ##TIMESTAMP##\n" +
       "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    parse("twoCasesSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: test1 ##TIMESTAMP##\n" +
        "TEST STARTED: test2 ##TIMESTAMP##\n" +
        "TEST FINISHED: test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    parse("twoCasesFirstSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: test1 ##TIMESTAMP##\n" +
        "TEST FINISHED: test1 ##TIMESTAMP##\n" +
        "TEST STARTED: test2 ##TIMESTAMP##\n" +
        "TEST FAILED: test2\n" +
        "Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "            at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    parse("twoCasesSecondSuccess.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: test1 ##TIMESTAMP##\n" +
        "TEST FAILED: test1\n" +
        "Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "            at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: test1 ##TIMESTAMP##\n" +
        "TEST STARTED: test2 ##TIMESTAMP##\n" +
        "TEST FINISHED: test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test2CasesFailed() throws Exception {
    parse("twoCasesFailed.xml");
    assertResultEquals(
      "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
        "TEST STARTED: test1 ##TIMESTAMP##\n" +
        "TEST FAILED: test1\n" +
        "Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "            at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: test1 ##TIMESTAMP##\n" +
        "TEST STARTED: test2 ##TIMESTAMP##\n" +
        "TEST FAILED: test2\n" +
        "Assertion message form test\n" +
        "junit.framework.AssertionFailedError: Assertion message form test\n" +
        "            at TestCase.test(Unknown Source)\n" +
        "TEST FINISHED: test2 ##TIMESTAMP##\n" +
        "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test1CaseIgnored() throws Exception {
    parse("singleCaseIgnored.xml");
    assertResultEquals(
      SINGLE_CASE_IGNORED);
  }

  @Test
  public void test1CaseIgnoredFailed() throws Exception {
    parse("singleCaseIgnoredFailure.xml");
    assertResultEquals(
      SINGLE_CASE_IGNORED);
  }

  //TW-7573: XML Report plugin not reporting correct results for NUnit results
  @Test
  public void testNegativePassedTestNumberObserved() throws Exception {
    parse("Pragma.OnKey5.Tests.OCL.dll.TestResult.xml");
    assertResultEquals(
      getExpectedResult("negativePassedTestNumberObserved.gold"));
  }

 @Test
  public void test1CaseFailureWithMultiline() throws Exception {
   parse("singleCaseFailureWithMultiline.xml");
   assertResultEquals(
     "SUITE STARTED: TestCase ##TIMESTAMP##\n" +
       "TEST STARTED: test ##TIMESTAMP##\n" +
       "TEST FAILED: test\n" +
       "Assertion message form test\n" +
       "junit.framework.AssertionFailedError: Assertion message form test\n" +
       "            at TestCase.test(Unknown Source)\n" +
       "            at TestCase1.test(Unknown Source)\n" +
       "            at TestCase2.test(Unknown Source)\n" +
       "TEST FINISHED: test ##TIMESTAMP##\n" +
       "SUITE FINISHED: TestCase ##TIMESTAMP##\n");
  }

  @Test
  public void test1CaseFailureInManyLines() throws Exception {
    parse("TestResults.xml");
    assertResultEquals(
      getExpectedResult("singleCaseWithFailureInManyLines.gold"));
  }

  //TW-8140 (TW-8120)
  @Test
  public void testReportWithPrematureEndOfFileFull() throws Exception {
    parse("TestResults_TW8120.xml");
    assertResultEquals(
      getExpectedResult("reportWithPrematureEndOfFileFull.gold"));
  }

  //TW-8140 (TW-8120)
  @Test
  public void testReportWithPrematureEndOfFilePart() throws Exception {
    parse("TestResults_TW8120.xml", parse("TestResults_TW8120_part.xml"));
    assertResultEquals(
      getExpectedResult("reportWithPrematureEndOfFileFull.gold"));
  }

  //TW-8815
//  @Test
//  public void testTwoIdenticalAssembliesWithDifferingTimestamp() throws Exception {
//    myContext.checking(new Expectations() {
//      {
//        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(any(String.class)), with(any(String.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(any(String.class)), with(any(String.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
//        inSequence(mySequence);
//        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
//        inSequence(mySequence);
//      }
//    });
//    myParser.parse(report("twoCasesFailed.xml"));
//    myParser.parse(report("twoCasesFailed_diffTimestamp.xml"));
//    myContext.assertIsSatisfied();
//  }

  //  TW-11744
  @Test
  public void test_nunit_2_5_x_statuses() throws Exception {
    parse("nunit-2.5/statuses.xml");
    assertResultEquals(
      getExpectedResult("nunit_2_5_x_statuses.gold"));
  }
}