/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;


public class PmdReportParserTest extends TestCase {
  private void runTest(final String fileName) throws Exception {
    final String reportName = getAbsoluteTestDataPath(fileName, "pmd");
    final String resultsFile = reportName + ".tmp";
    final String expectedFile = reportName + ".gold";
    final String baseDir = getAbsoluteTestDataPath(null, "pmd");

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();

    final BaseServerLoggerFacade logger = new BuildLoggerForTesting(results);
    final InspectionReporter reporter = TestUtil.createFakeReporter(results);

    final PmdReportParser parser = new PmdReportParser(logger, reporter, "C:\\work\\teamcityworkspace\\xml-report-plugin\\tests\\testData\\pmd");

    final File report = new File(reportName);
    final Map<String, String> params = new HashMap<String, String>();
    XmlReportPluginUtil.enableXmlReportParsing(params, FindBugsReportParser.TYPE);
    XmlReportPluginUtil.setMaxErrors(params, 5);
    XmlReportPluginUtil.setMaxWarnings(params, 5);
    parser.parse(new ReportData(report, "pmd"));
    parser.logReportTotals(report, true);
    parser.logParsingTotals(params, true);

    final File expected = new File(expectedFile);
    final String actual = results.toString().replace(baseDir, "##BASE_DIR##").trim();
    if (!readFile(expected).replace("/", File.separator).replace("\\", File.separator).trim().equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(readFile(expected), actual);
    }
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.xml");
  }
}
