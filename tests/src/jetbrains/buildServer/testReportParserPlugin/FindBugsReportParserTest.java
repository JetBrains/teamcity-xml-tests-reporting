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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionTypeInfo;
import jetbrains.buildServer.testReportParserPlugin.findBugs.FindBugsReportParser;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;

import java.io.*;


public class FindBugsReportParserTest extends TestCase {
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
    final String prefix = getTestDataPath(fileName);
    final String resultsFile = prefix + ".tmp";
    final String expectedFile = prefix + ".exp";

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();

    final SimpleBuildLogger logger = new BuildLoggerForTesting(results);
    final InspectionReporter reporter = createFakeReporter(results);

    final FindBugsReportParser parser = new FindBugsReportParser(logger, reporter, prefix.substring(0, prefix.lastIndexOf(fileName)));
    parser.parse(new File(prefix));
//    results.append("Errors: ").append(parser.getErrorsCount()).append("\r\n");
//    results.append("Warnings: ").append(parser.getWarningsCount()).append("\r\n");

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

  public void testSimple() throws Exception {
    runTest("simple.xml");
  }

  public void testPattern() throws Exception {
    runTest("pattern.xml");
  }

  public void testcategory() throws Exception {
    runTest("category.xml");
  }
}