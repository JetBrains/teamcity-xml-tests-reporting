package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.xmlReportPlugin.pmdCpd.PmdCpdReportParser;
import org.junit.Test;

import java.util.Map;

/**
 * User: vbedrosova
 * Date: 07.09.2010
 * Time: 15:08:19
 */
public class PmdCpdReportParserTest extends ParserTestCase {
  @Override
  protected String getType() {
    return PmdCpdReportParser.TYPE;
  }

  @Override
  protected XmlReportParser getParser(StringBuilder results) {
    final DuplicatesReporter reporter = TestUtil.createDuplicatesReporter(results);
    final BuildProgressLogger logger = new BuildLoggerForTesting(results);

    return new PmdCpdReportParser(logger, reporter);
  }

  @Override
  protected void prepareParams(Map<String, String> paramsMap) {
    XmlReportPluginUtil.enableXmlReportParsing(paramsMap, getType());
  }

  @Test
  public void testSimple() throws Exception {
    runTest("result.xml");
  }
}
