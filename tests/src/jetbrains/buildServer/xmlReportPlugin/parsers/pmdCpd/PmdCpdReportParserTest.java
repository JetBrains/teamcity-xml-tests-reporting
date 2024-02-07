

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * User: vbedrosova
 * Date: 07.09.2010
 * Time: 15:08:19
 */
@Test
public class PmdCpdReportParserTest extends BaseParserTestCase {
  @NotNull
  @Override
  protected Parser getParser() {
    return new PmdCpdReportParser(getDuplicationReporter(), getBaseDir());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return "pmdCpd";
  }

  @Test
  public void testSimple() throws Exception {
    runTest("result.xml");
  }

  @Test
  @TestFor(issues = "TW-43120")
  public void testSimpleJavaEncoding() throws Exception {
    runTest("result2.xml");
  }

  private void runTest(final String reportName) throws Exception {
    parse(reportName);
    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }
}