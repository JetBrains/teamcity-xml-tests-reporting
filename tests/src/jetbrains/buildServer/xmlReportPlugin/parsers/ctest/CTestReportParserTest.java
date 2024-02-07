

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * @author Vladislav.Rassokhin
 */
@Test
public class CTestReportParserTest extends BaseParserTestCase {

  public static final String REPORT_DIR = "ctest";

  @NotNull
  @Override
  protected Parser getParser() {
    return new CTestReportParser(getTestReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return REPORT_DIR;
  }

  private void runTest(final String dirName) throws Exception {
    runTest(dirName, "Test.xml");
  }

  private void runTest(final String dirName, final String fileName) throws Exception {
    String fullFileName = dirName + "/" + fileName;
    parse(fullFileName);
    assertResultEquals(getExpectedResult(fullFileName + ".gold"));
  }

  public void testSample1() throws Exception {
    runTest("sample1");
  }

  public void testFailedOne1() throws Exception {
    runTest("failedOne1");
  }

  public void testFailedOne2() throws Exception {
    runTest("failedOne2");
  }

  public void testEmpty() throws Exception {
    runTest("empty");
  }

  public void testOutputParsing() throws Exception {
    runTest("outputParsing");
  }

  public void test2Suites() throws Exception {
    String s = "2suites/";
    parse(s + "A/Test.xml");
    parse(s + "B/Test.xml");
    assertResultEquals(getExpectedResult(s + "result" + ".gold"));
  }

  public void testIncorrectFormat() throws Exception {
    runTest("incorrect-format", "junit.xml");
  }

  public void testNoNameCheck() throws Exception {
    runTest("no-name-check", "name.xml");
  }
}