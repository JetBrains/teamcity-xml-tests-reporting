package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BuildProgressLogger;
import junit.framework.TestCase;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;

/**
 * User: vbedrosova
 * Date: 07.09.2010
 * Time: 15:21:04
 */
public abstract class ParserTestCase extends TestCase {
  protected void runTest(final String fileName) throws Exception {
    final String reportName = getAbsoluteTestDataPath(fileName, getType());
    final String resultsFile = reportName + ".tmp";
    final String expectedFile = reportName + ".gold";
    final String baseDir = getAbsoluteTestDataPath(null, getType());

    //noinspection ResultOfMethodCallIgnored
    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();

    final XmlReportParser parser = getParser(results);

    final BuildProgressLogger logger = new BuildLoggerForTesting(results);

    final File report = new File(reportName);

    final DummyReportFileContext reportFileContext = new DummyReportFileContext(report, getType(), logger);

    final Map<String, String> params = new HashMap<String, String>();
    prepareParams(params);

    parser.parse(reportFileContext);
    parser.logReportTotals(reportFileContext, true);
    parser.logParsingTotals(new DummySessionContext(logger), params, true);

    final File expected = new File(expectedFile);
    final String actual = results.toString().replace(baseDir, "##BASE_DIR##").replace("/", File.separator).replace("\\", File.separator).trim();
    if (!readFile(expected, true).equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(readFile(expected, true), actual);
    }
  }

  protected abstract String getType();
  protected abstract XmlReportParser getParser(StringBuilder result);
  protected abstract void prepareParams(Map<String, String> paramsMap);
}
