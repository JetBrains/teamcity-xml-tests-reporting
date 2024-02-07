

package jetbrains.buildServer.xmlReportPlugin.parsers.jslint;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * User: vbedrosova
 * Date: 06.05.11
 * Time: 20:39
 */
@Test
public class JSLintReportParserTest extends BaseParserTestCase {
  @NotNull
  @Override
  protected Parser getParser() {
    return new JSLintReportParser(getInspectionReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return "jslint";
  }

  public void testMisc() throws Exception {
    parse("misc.xml");
    assertResultEquals(getExpectedResult("misc.xml.gold"));
  }
}