

package jetbrains.buildServer.xmlReportPlugin.parsers.checkstyle;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * User: vbedrosova
 * Date: 25.12.2009
 * Time: 15:01:28
 */

@Test
public class CheckstyleReportParserTest extends BaseParserTestCase {
  private static final String TYPE = "checkstyle";

  @NotNull
  @Override
  protected Parser getParser() {
    return new CheckstyleReportParser(getInspectionReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return TYPE;
  }

  private void runTest(final String fileName) throws Exception {
    parse(fileName);
    assertResultEquals(getExpectedResult(fileName + ".gold"));
  }

  @Test
  public void testNoInspections() throws Exception {
    runTest("noInspections.xml");
  }

  @Test
  public void testOneErrorOneWarningOneInfo() throws Exception {
    runTest("oneErrorOneWarningOneInfo.xml");
  }

  @Test
  public void testException() throws Exception {
    runTest("exception.xml");
  }

  @Test
  public void testBig() throws Exception {
    runTest("big.xml");
  }
}