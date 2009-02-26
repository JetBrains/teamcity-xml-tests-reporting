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

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionTypeInfo;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.Map;


public class FindBugsReportParserTest extends TestCase {
//  @BeforeClass
//  public static void prepareTestData() {
//    System.out.println("I AM HERE");
//    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
//    Transformer transformer = null;
//    try {
////      transformer = transformerFactory.newTransformer(new StreamSource(this.getClass().getResourceAsStream("reportPaths.xsl")));
//
//      final File testDataDir = new File("tests/testData/findBugs/");
//      assert testDataDir.isDirectory();
//
//      File[] testData = testDataDir.listFiles(new FilenameFilter() {
//
//        public boolean accept(File dir, String name) {
//          return dir.equals(testDataDir) && name.endsWith(".xml");
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

  private String getTestDataPath(final String fileName) {
    File file = new File("tests/testData/findBugs/" + fileName);
    return file.getAbsolutePath();
  }

  static private String readFile(@NotNull final File file) throws IOException {
    final FileInputStream inputStream = new FileInputStream(file);
    try {
      final BufferedInputStream bis = new BufferedInputStream(inputStream);
      final byte[] bytes = new byte[(int) file.length()];
      bis.read(bytes);
      bis.close();

      return new String(bytes);
    }
    finally {
      inputStream.close();
    }
  }

  private void runTest(final String fileName) throws Exception {
    final String reportName = getTestDataPath(fileName);
    final String resultsFile = reportName + ".tmp";
    final String expectedFile = reportName + ".gold";

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();

    final SimpleBuildLogger logger = new BuildLoggerForTesting(results);
    final InspectionReporter reporter = createFakeReporter(results);

    final FindBugsReportParser parser = new FindBugsReportParser(logger, reporter, reportName.substring(0, reportName.lastIndexOf(fileName)));

    final File report = new File(reportName);
    parser.parse(report, 0);
    final Map<String, String> params = new HashMap<String, String>();
    XmlReportPluginUtil.enableXmlReportParsing(params, FindBugsReportParser.TYPE);
    XmlReportPluginUtil.setMaxErrors(params, 5);
    XmlReportPluginUtil.setMaxWarnings(params, 5);
    parser.logReportTotals(report, params);

    final File expected = new File(expectedFile);
    if (!readFile(expected).equals(results.toString())) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(results.toString());
      resultsWriter.close();

      assertEquals(readFile(expected), results.toString());
    }
  }

  private InspectionReporter createFakeReporter(final StringBuilder results) {
    return new InspectionReporter() {
      public void reportInspection(@NotNull final InspectionInstance inspection) {
        results.append(inspection.toString()).append("\n");
      }

      public void reportInspectionType(@NotNull final InspectionTypeInfo inspectionType) {
        results.append(inspectionType.toString()).append("\n");
      }

      public void markBuildAsInspectionsBuild() {
      }

      public void flush() {
      }
    };
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.xml");
  }

  @Test
  public void testJar() throws Exception {
    runTest("jar.xml");
  }

  @Test
  public void testComplexSrc() throws Exception {
    runTest("complexSrc.xml");
  }

  @Test
  public void testDir() throws Exception {
    runTest("dir.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.xml");
  }

  @Test
  public void testPattern() throws Exception {
    runTest("pattern.xml");
  }

  @Test
  public void testCategory() throws Exception {
    runTest("category.xml");
  }

  @Test
  public void testBuildFailsErrors() throws Exception {
    runTest("failureErr.xml");
  }

  @Test
  public void testBuildFailsWarnings() throws Exception {
    runTest("failureWarn.xml");
  }
}