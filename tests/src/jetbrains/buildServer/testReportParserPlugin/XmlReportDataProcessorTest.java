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

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;

import java.io.*;
import java.util.List;
import java.util.Map;


public class XmlReportDataProcessorTest extends TestCase {
  private Mockery myContext;

  private static String getTestDataPath(final String fileName) {
    File file = new File("tests/testData/dataProcessor/" + fileName);
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

  public void setUp() {
    myContext = new JUnit4Mockery();
  }

  private void runTest(Map<String, String> arguments, String fileName) throws Exception {
    final String prefix = getTestDataPath(fileName);
    final String resultsFile = prefix + ".tmp";
    final String expectedFile = prefix + ".exp";

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();
    final TestReportParserPlugin plugin = createFakePlugin(results);

    XmlReportDataProcessor processor = new XmlReportDataProcessor(plugin);
    processor.processData(new File("file"), arguments);

    final File expected = new File(expectedFile);
    if (!readFile(expected).equals(results.toString())) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(results.toString());
      resultsWriter.close();

      assertEquals(readFile(expected), results.toString());
    }
  }

  private TestReportParserPlugin createFakePlugin(final StringBuilder results) {
    final EventDispatcher dispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    final InspectionReporter reporter = myContext.mock(InspectionReporter.class);
    final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
    final SimpleBuildLogger logger = new BuildLoggerForTesting(results);

    myContext.checking(new Expectations() {
      {
        allowing(runningBuild).getBuildLogger();
        will(returnValue(logger));
        ignoring(runningBuild);
      }
    });

    final TestReportParsingParameters parameters = new TestReportParsingParametersForTesting(runningBuild);
    return new TestReportParserPlugin(dispatcher, reporter) {
      public void processReports(String reportType, List<File> reportDirs) {
        results.append("TestReportParserPlugin: PROCESSING STARTED");
      }

      public TestReportParsingParameters getParameters() {
        return parameters;
      }

      public StringBuilder getText() {
        return results.append(((TestReportParsingParametersForTesting) getParameters()).getImportantParametersString());
      }
    };
  }

  private static class TestReportParsingParametersForTesting extends TestReportParsingParameters {
    public TestReportParsingParametersForTesting(@NotNull AgentRunningBuild build) {
      super(build);
    }

    public String getImportantParametersString() {
      return "<REPORT_TYPE=" + this.getReportType() + " PARSING_ENABLED=" + this.isParsingEnabled() +
        " VEROSE=" + this.isVerboseOutput() + " PARSE_OLD=" + this.isParseOutOfDateFiles() + ">\n";
    }
  }

  public void testEmpty() throws Exception {
//    runTest(new HashMap<String, String>(), "empty.xml");
  }
}
