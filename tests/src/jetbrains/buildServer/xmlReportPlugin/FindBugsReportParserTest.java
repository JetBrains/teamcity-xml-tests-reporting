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
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.*;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;


public class FindBugsReportParserTest extends TestCase {
//  @BeforeClass
//  public static void prepareTestData() {
//    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
//    Transformer transformer = null;
//    try {
//      transformer = transformerFactory.newTransformer(new StreamSource(FindBugsReportParserTest.class.getResourceAsStream(getAbsoluteTestDataPath("reportPaths.xsl", ""))));
//
//      final File testDataDir = new File(getAbsoluteTestDataPath("", "findBugs"));
//      assert testDataDir.isDirectory();
//
//      File[] testData = testDataDir.listFiles(new FilenameFilter() {
//
//        public boolean accept(File dir, String name) {
//          return dir.equals(testDataDir) && name.endsWith("sample.xml");
//        }
//      });
//
//      for (int i = 0; i < testData.length; ++i) {
//        final StreamSource source = new StreamSource(testData[i]);
//        final File newFile = new File(testData[i].getAbsolutePath() + ".trans");
//        final StreamResult result = new StreamResult(newFile);
//        transformer.transform(source, result);
//      }
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }

  private void runTest(final String sampleName) throws Exception {
    final String fileName = sampleName.replace(".sample", "");

    final String baseDir = getAbsoluteTestDataPath(null, "findBugs");
    final File samleFile = getTestDataFile(sampleName, "findBugs");
    final String reportName = samleFile.getAbsolutePath().replace(".sample", "");

    final FileWriter writer = new FileWriter(reportName);
    writer.write(readFile(samleFile, false).replace("##BASE_DIR##", baseDir.replace(File.separator, "\\")));
    writer.close();

//    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
//    Transformer transformer = transformerFactory.newTransformer(new StreamSource(getClass().getResourceAsStream(getAbsoluteTestDataPath("reportPaths.xsl", ""))));
//    transformer.transform(new StreamSource(samleName), new StreamResult(reportName));

    final String resultsFileName = reportName + ".tmp";
    final String expectedFileName = reportName + ".gold";

    new File(resultsFileName).delete();

    final StringBuilder results = new StringBuilder();

    final BaseServerLoggerFacade logger = new BuildLoggerForTesting(results);
    final InspectionReporter reporter = TestUtil.createFakeReporter(results);

    final FindBugsReportParser parser = new FindBugsReportParser(logger, reporter, reportName.substring(0, reportName.lastIndexOf(fileName)));

    final File report = new File(reportName);
    final Map<String, String> params = new HashMap<String, String>();
    XmlReportPluginUtil.enableXmlReportParsing(params, FindBugsReportParser.TYPE);
    XmlReportPluginUtil.setMaxErrors(params, 5);
    XmlReportPluginUtil.setMaxWarnings(params, 5);
    parser.parse(new ReportData(report, "findBugs"));
    parser.logReportTotals(report, true);
    parser.logParsingTotals(params, true);

    final File expectedFile = new File(expectedFileName);
    final String actual = results.toString().replace(baseDir, "##BASE_DIR##").replace("\\", File.separator).replace("/", File.separator).trim();
    if (!readFile(expectedFile, true).equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFileName);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(readFile(expectedFile, true), actual);
    }
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.sample.xml");
  }

  @Test
  public void testNoSrcSimple() throws Exception {
    runTest("noSrcSimple.sample.xml");
  }

  @Test
  public void testNoSrcJar() throws Exception {
    runTest("jar.sample.xml");
  }

  @Test
  public void testComplexSrc() throws Exception {
    runTest("complexSrc.sample.xml");
  }

  @Test
  public void testNoSrcDir() throws Exception {
    runTest("dir.sample.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.sample.xml");
  }

  @Test
  public void testPattern() throws Exception {
    runTest("pattern.sample.xml");
  }

  @Test
  public void testCategory() throws Exception {
    runTest("category.sample.xml");
  }

  @Test
  public void testBuildFailsErrors() throws Exception {
    runTest("failureErr.sample.xml");
  }

  @Test
  public void testBuildFailsWarnings() throws Exception {
    runTest("failureWarn.sample.xml");
  }
}