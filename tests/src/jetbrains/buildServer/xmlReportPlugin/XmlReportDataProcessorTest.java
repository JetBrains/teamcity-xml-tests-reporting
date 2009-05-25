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
import jetbrains.buildServer.agent.inspections.InspectionReporterListener;
import jetbrains.buildServer.util.EventDispatcher;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class XmlReportDataProcessorTest extends TestCase {
  private Mockery myContext;

  public void setUp() {
    myContext = new JUnit4Mockery();
  }

  private void runTest(Map<String, String> arguments, String fileName) throws Exception {
    final File resultsFile = File.createTempFile(fileName, ".tmp");
    final File expectedFile = TestUtil.getTestDataFile(fileName + ".gold", "dataProcessor");

    final StringBuilder results = new StringBuilder();
    final XmlReportPlugin plugin = createFakePlugin(results, TestUtil.getTestDataFile(null, null).getAbsoluteFile().getParentFile().getParentFile());

    final XmlReportDataProcessor processor = new XmlReportDataProcessor.JUnitDataProcessor(plugin);
    processor.processData(new File(getTestDataPath("Report.xml", "dataProcessor")), arguments);

    final String expectedContent = readFile(expectedFile).replace("/", File.separator).replace("\\", File.separator).trim();
    final String actual = results.toString().replace("/", File.separator).replace("\\", File.separator).trim();
    if (!expectedContent.equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(results.toString());
      resultsWriter.close();

      assertEquals(expectedContent, actual);
    }
  }

  private XmlReportPlugin createFakePlugin(final StringBuilder results, final File base) {
    final EventDispatcher dispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    final InspectionReporter reporter = myContext.mock(InspectionReporter.class);
    myContext.checking(new Expectations() {
      {
        allowing(reporter).addListener(with(any(InspectionReporterListener.class)));
      }
    });
    return new XmlReportPlugin(dispatcher, reporter) {
      public void processReports(Map<String, String> params, Set<File> reportDirs) {
        for (String key : params.keySet()) {
          results.append("<").append(key).append(", ").append(params.get(key)).append(">\n");
        }
        for (File f : reportDirs) {
          results.append(TestUtil.getRelativePath(f, base)).append("\n");
        }
      }
    };
  }

  @Test
  public void testDefault() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    runTest(arguments, "default");
  }

  @Test
  public void testVerbose() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.VERBOSE_ARGUMENT, "true");
    runTest(arguments, "verbose");
  }

  @Test
  public void testOutOfDate() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.PARSE_OUT_OF_DATE_ARGUMENT, "true");
    runTest(arguments, "outOfDate");
  }

  @Test
  public void testErrorsLimit() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.ERRORS_LIMIT_ARGUMENT, "10");
    runTest(arguments, "errorsLimit");
  }

  @Test
  public void testWarningsLimit() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.WARNINGS_LIMIT_ARGUMENT, "10");
    runTest(arguments, "warningsLimit");
  }
}
