

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class NUnitReportParserTest extends BaseParserTestCase {
  private static final String REPORT_DIR = "nunit";

  @NotNull
  @Override
  protected Parser getParser() {
    return new NUnitReportParser(getTestReporter());
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
    Assert.assertEquals(suitesLogged, 0, "Empty reportData contains 0 suites, but " + suitesLogged + " suites logged");

    final int testsLogged = result.getTests();
    Assert.assertEquals(testsLogged, 0 , "Empty reportData contains 0 tests, but " + testsLogged + " tests logged");
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    parse("singleCaseSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

 @Test
  public void test1CaseFailure() throws Exception {
   parse("singleCaseFailure.xml");
   assertResultEquals(
     "TestSuite:TestCase\n" +
     "  Test:test\n" +
     "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
     "            at TestCase.test(Unknown Source)\n" +
     "  EndTest:16\n" +
     "------------------------\n" +
     "EndSuite\n");
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    parse("twoCasesSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test1\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "  Test:test2\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    parse("twoCasesFirstSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test1\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "  Test:test2\n" +
      "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "            at TestCase.test(Unknown Source)\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    parse("twoCasesSecondSuccess.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test1\n" +
      "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "            at TestCase.test(Unknown Source)\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "  Test:test2\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test2CasesFailed() throws Exception {
    parse("twoCasesFailed.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test1\n" +
      "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "            at TestCase.test(Unknown Source)\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "  Test:test2\n" +
      "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "            at TestCase.test(Unknown Source)\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test1CaseIgnored() throws Exception {
    parse("singleCaseIgnored.xml");
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test\n" +
      "    Ignored:\n" +
      "  EndTest:0\n" +
      "------------------------\n" +
      "EndSuite\n");
  }

  @Test
  public void test1CaseIn2PartsBreakTestSuiteBetweenAttrs() throws Exception {
    parse("singleCaseFailure.xml", parse("singleCaseBreakTestSuiteBetweenAttrs.xml"));
    assertResultEquals(
      "TestSuite:TestCase\n" +
      "  Test:test\n" +
      "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
      "            at TestCase.test(Unknown Source)\n" +
      "  EndTest:16\n" +
      "------------------------\n" +
      "EndSuite\n");
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
     "TestSuite:TestCase\n" +
     "  Test:test\n" +
     "    Fail:Assertion message form test Message: junit.framework.AssertionFailedError: Assertion message form test\n" +
     "            at TestCase.test(Unknown Source)\n" +
     "            at TestCase1.test(Unknown Source)\n" +
     "            at TestCase2.test(Unknown Source)\n" +
     "  EndTest:16\n" +
     "------------------------\n" +
     "EndSuite\n");
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
    parse("TestResults_TW8120.xml", parse("TestResults_TW8120_part.xml", parse("TestResults_TW8120_part.xml")));
    assertResultEquals(
      getExpectedResult("reportWithPrematureEndOfFileFrom3Tries.gold"));
  }

  @Test
  public void testSuiteNameWithFullPathToAssemblyDll() throws Exception {
    parse("fullPathToDllInSuiteName.xml");
    assertResultEquals(
      "TestSuite:Test.dll\n" +
      "  Test:Namespace.Package.Class.testMethod\n" +
      "  EndTest:11041\n" +
      "------------------------\n" +
      "EndSuite\n");
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

  // TW-11744
  @Test
  public void test_nunit_2_5_x_statuses() throws Exception {
    parse("nunit-2.5/statuses.xml");
    assertResultEquals(
      getExpectedResult("nunit_2_5_x_statuses.gold"));
  }

  // TW-17110
  @Test
  public void test_abnormal_termination() throws Exception {
    parse("abnormalTermination.xml");
    assertResultEquals("TestSuite:xxx.dll\n" +
                       "  Test:AbnormalTermination\n" +
                       "    Fail:Abnormal termination, exit code -2146233082 (0x80131506) Message: Stacktrace not available\n" +
                       "  EndTest:274524000\n" +
                       "------------------------\n" +
                       "EndSuite\n");
  }

  @Test
  public void test_TW_11859() throws Exception {
    parse("TestResults_TW11859.xml");
    assertResultEquals(getExpectedResult("TW11859.gold"));
  }

  @Test
  public void testUnsupportedFormat() throws Exception {
    parse("junit.xml");
    assertResultEquals("-->Error: ##BASE_DIR##/junit.xml: must contain \"test-results\", \"test-run\" or \"stack-trace\" root element\nPlease check the NUnit sources for the supported XML Schema\n");
  }

  @Test
  public void test_TW_46544() throws Exception {
    parse("TestResults_TW46544.xml");
    assertResultEquals(getExpectedResult("TW46544.gold"));
  }
}