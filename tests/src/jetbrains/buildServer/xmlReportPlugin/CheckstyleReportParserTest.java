/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.checkstyle.CheckstyleReportParser;
import junit.framework.TestCase;
import org.junit.Test;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;

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

    final BuildProgressLogger logger = new BuildLoggerForTesting(results);
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
