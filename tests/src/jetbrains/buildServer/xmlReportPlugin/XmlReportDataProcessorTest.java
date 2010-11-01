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
import java.util.List;
import java.util.Map;
import java.util.Set;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.DataProcessorContext;
import jetbrains.buildServer.util.EventDispatcher;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.*;

public class XmlReportDataProcessorTest extends TestCase {
  private Mockery myContext;

  @Override
  public void setUp() {
    myContext = new JUnit4Mockery();
  }

  private void runTest(final Map<String, String> arguments, final String fileName) throws Exception {
    final File expectedFile = TestUtil.getTestDataFile(fileName + ".gold", "dataProcessor");
    final File resultsFile = new File(expectedFile.getAbsolutePath().replace(".gold", ".tmp"));

    final StringBuilder results = new StringBuilder();
    final XmlReportPlugin plugin = createFakePlugin(results, TestUtil.getTestDataFile(null, null).getAbsoluteFile().getParentFile().getParentFile());

    final File testDataPath = new File(getTestDataPath("Report.xml", "dataProcessor"));
    final AgentRunningBuild runningBuildMock = myContext.mock(AgentRunningBuild.class);

    final XmlReportDataProcessor processor = new XmlReportDataProcessor.JUnitDataProcessor(plugin);

    processor.processData(new DataProcessorContext() {
      @NotNull
      public AgentRunningBuild getBuild() {
        return runningBuildMock;
      }

      @NotNull
      public File getFile() {
        return testDataPath;
      }

      @NotNull
      public Map<String, String> getArguments() {
        return arguments;
      }
    });

    final FileWriter resultsWriter = new FileWriter(resultsFile);
    resultsWriter.write(results.toString());
    resultsWriter.close();

    final List<String> expectedList = readFileToList(expectedFile);
    final List<String> actualList = readFileToList(resultsFile);

    assertEquals(expectedList, actualList);
  }

  private XmlReportPlugin createFakePlugin(final StringBuilder results, final File base) {
    return new XmlReportPlugin(EventDispatcher.create(AgentLifeCycleListener.class),
      createInspectionReporter(myContext), createDuplicatesReporter(myContext)) {
      @Override
      public void processReports(@NotNull Map<String, String> params, @NotNull Set<File> reportDirs) {
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

  @Test
  public void testWhenNoDataPublished() throws Exception {
    final Map<String, String> arguments = new HashMap<String, String>();
    arguments.put(XmlReportDataProcessor.WHEN_NO_DATA_PUBLISHED_ARGUMENT, "warning");
    runTest(arguments, "whenNoDataPublished");
  }
}
