/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import java.io.FileNotFoundException;
import java.io.IOException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.xml.sax.XMLReader;


public abstract class BaseParserTestCase extends TestCase {
  private StringBuilder myResult;

  private BuildProgressLogger myLogger;
  private InspectionReporter myInspectionReporter;
  private DuplicatesReporter myDuplicatesReporter;

  private XMLReader myXMLReader;

  private File myBaseDir;


  @Override
  @Before
  public void setUp() throws Exception {
    myResult = new StringBuilder();

    myLogger = new BuildLoggerForTesting(myResult);
    myInspectionReporter = TestUtil.createInspectionReporter(myResult);
    myDuplicatesReporter = TestUtil.createDuplicatesReporter(myResult);

    myXMLReader = ParserUtils.createXmlReader(false);

    myBaseDir = TestUtil.getTestDataFile(null, getReportDir());
  }

  @NotNull
  protected File getReport(@NotNull final String fileName) throws FileNotFoundException {
    return TestUtil.getTestDataFile(fileName, getReportDir());
  }

  @NotNull
  protected String getExpectedResult(@NotNull final String fileName) throws IOException {
    return FileUtil.readText(TestUtil.getTestDataFile(fileName, getReportDir()));
  }

  @NotNull
  protected ParsingResult parse(@NotNull String reportName,
                                @Nullable ParsingResult prevResult) throws Exception {
    final Parser parser = getParser();
    parser.parse(getReport(reportName), prevResult);
    final ParsingResult result = parser.getParsingResult();
    assertNotNull("Result is null", result);
    return result;
  }

  @NotNull
  protected ParsingResult parse(@NotNull String reportName) throws Exception {
    return parse(reportName, null);
  }

  protected void assertResultEquals(@NotNull String expected) {
    final String actual = prepareResult();
    assertEquals("Actual result: " + actual, expected, actual);
  }

  @NotNull
  protected String prepareResult() {
    return myResult.toString().replace(myBaseDir.getAbsolutePath(), "##BASE_DIR##").replace("\\", "/");
  }

  protected BuildProgressLogger getLogger() {
    return myLogger;
  }

  protected InspectionReporter getInspectionReporter() {
    return myInspectionReporter;
  }

  protected DuplicatesReporter getDuplicatesReporter() {
    return myDuplicatesReporter;
  }

  protected XMLReader getXMLReader() {
    return myXMLReader;
  }

  protected File getBaseDir() {
    return myBaseDir;
  }

  @NotNull
  protected abstract Parser getParser();

  @NotNull
  protected abstract String getReportDir();
}
