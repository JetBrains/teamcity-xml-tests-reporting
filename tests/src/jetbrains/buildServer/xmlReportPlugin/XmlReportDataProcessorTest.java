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

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class XmlReportDataProcessorTest extends TestCase {
  private Mockery myContext;

  private static String getTestDataPath(final String fileName) {
    File file = new File("tests/testData/dataProcessor/" + fileName);
    return file.getPath();
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

  public void setUp() {
    myContext = new JUnit4Mockery();
  }

  private void runTest(Map<String, String> arguments, String fileName) throws Exception {
    final String prefix = getTestDataPath(fileName);
    final String resultsFile = prefix + ".tmp";
    final String expectedFile = prefix + ".gold";

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();
    final XmlReportPlugin plugin = createFakePlugin(results);

    final XmlReportDataProcessor processor = new XmlReportDataProcessor.JUnitDataProcessor(plugin);
    processor.processData(new File(getTestDataPath("Report.xml")), arguments);

    final File expected = new File(expectedFile);
    if (!readFile(expected).equals(results.toString())) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(results.toString());
      resultsWriter.close();

      assertEquals(readFile(expected), results.toString());
    }
  }

  private XmlReportPlugin createFakePlugin(final StringBuilder results) {
    final EventDispatcher dispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    final InspectionReporter reporter = myContext.mock(InspectionReporter.class);
    return new XmlReportPlugin(dispatcher, reporter) {
      public void processReports(Map<String, String> params, List<File> reportDirs) {
        for (String key : params.keySet()) {
          results.append("<").append(key).append(", ").append(params.get(key)).append(">\n");
        }
        for (File f : reportDirs) {
          results.append(f.getPath()).append("\n");
        }
      }
    };
  }


  @Test
  public void testDefault() throws Exception {
    Map<String, String> arguments = new HashMap<String, String>();
    runTest(arguments, "default");
  }

  @Test
  public void testVerbose() throws Exception {
    Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.VERBOSE_ARGUMENT, "true");
    runTest(arguments, "verbose");
  }

  @Test
  public void testOutOfDate() throws Exception {
    Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.PARSE_OUT_OF_DATE_ARGUMENT, "true");
    runTest(arguments, "outOfDate");
  }

  @Test
  public void testErrorsLimit() throws Exception {
    Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.ERRORS_LIMIT_ARGUMENT, "10");
    runTest(arguments, "errorsLimit");
  }

  @Test
  public void testWarningsLimit() throws Exception {
    Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.WARNINGS_LIMIT_ARGUMENT, "10");
    runTest(arguments, "warningsLimit");
  }
}
