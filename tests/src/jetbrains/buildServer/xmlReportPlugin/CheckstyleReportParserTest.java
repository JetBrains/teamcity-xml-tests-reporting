package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;
import jetbrains.buildServer.xmlReportPlugin.checkstyle.CheckstyleReportParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 25.12.2009
 * Time: 15:01:28
 */
public class CheckstyleReportParserTest extends TestCase {
  private void runTest(final String fileName) throws Exception {
    final String reportName = getAbsoluteTestDataPath(fileName, "checkstyle");
    final String resultsFile = reportName + ".tmp";
    final String expectedFile = reportName + ".gold";
    final String baseDir = getAbsoluteTestDataPath(null, "checkstyle");

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();

    final BaseServerLoggerFacade logger = new BuildLoggerForTesting(results);
    final InspectionReporter reporter = TestUtil.createFakeReporter(results);

    final CheckstyleReportParser parser = new CheckstyleReportParser(logger, reporter, "##BASE_DIR##");

    final File report = new File(reportName);
    final Map<String, String> params = new HashMap<String, String>();
    XmlReportPluginUtil.enableXmlReportParsing(params, CheckstyleReportParser.TYPE);
    XmlReportPluginUtil.setMaxErrors(params, 5);
    XmlReportPluginUtil.setMaxWarnings(params, 5);
    parser.parse(new ReportData(report, "pmd"));
    parser.logReportTotals(report, true);
    parser.logParsingTotals(params, true);

    final File expected = new File(expectedFile);
    final String actual = results.toString().replace(baseDir, "##BASE_DIR##").replace("/", File.separator).replace("\\", File.separator).trim();
    if (!readFile(expected, true).equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(readFile(expected, true), actual);
    }
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
