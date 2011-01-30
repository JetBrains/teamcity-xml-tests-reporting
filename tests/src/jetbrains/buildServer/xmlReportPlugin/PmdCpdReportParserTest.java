package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.xmlReportPlugin.pmdCpd.PmdCpdReportParser;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * User: vbedrosova
 * Date: 07.09.2010
 * Time: 15:08:19
 */
public class PmdCpdReportParserTest extends BaseParserTestCase {
  @NotNull
  @Override
  protected Parser getParser() {
    return new PmdCpdReportParser(getXMLReader(), getDuplicatesReporter(), getBaseDir());
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

  private void runTest(final String reportName) throws Exception {
    parse(reportName);
    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }
}
