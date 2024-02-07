

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public abstract class BaseParserTestCase {
  protected StringBuilder myResult;

  private InspectionReporter myInspectionReporter;
  private DuplicationReporter myDuplicationReporter;
  private TestReporter myTestReporter;

  protected File myBaseDir;


  @BeforeMethod
  public void setUp() throws Exception {
    myResult = new StringBuilder();

    myInspectionReporter = TestUtil.createInspectionReporter(myResult);
    myDuplicationReporter = TestUtil.createDuplicationReporter(myResult);
    myTestReporter = TestUtil.createTestResultsWriter(myResult);

    myBaseDir = TestUtil.getTestDataFile(null, getReportDir());
  }

  @NotNull
  protected File getReport(@NotNull final String fileName) throws FileNotFoundException {
    return TestUtil.getTestDataFile(fileName, getReportDir());
  }

  @NotNull
  protected String getExpectedResult(@NotNull final String fileName) throws IOException {
    return FileUtil.readText(TestUtil.getTestDataFile(fileName, getReportDir()), "UTF-8");
  }

  @NotNull
  protected ParsingResult parse(@NotNull String reportName,
                                @Nullable ParsingResult prevResult) throws Exception {
    return parse(getParser(), reportName, prevResult);
  }

  @NotNull
  protected ParsingResult parse(@NotNull Parser parser,
                                @NotNull String reportName,
                                @Nullable ParsingResult prevResult) throws Exception {
    parser.parse(getReport(reportName), prevResult);
    final ParsingResult result = parser.getParsingResult();
    assertNotNull(result);
    return result;
  }

  @NotNull
  protected ParsingResult parse(@NotNull String reportName) throws Exception {
    return parse(reportName, null);
  }

  @NotNull
  protected ParsingResult parse(@NotNull Parser parser,
                                @NotNull String reportName) throws Exception {
    return parse(parser, reportName, null);
  }

  protected void assertResultEquals(@NotNull String expected) {
    final String actual = normalizeLineEndings(prepareResult());
    assertEquals(actual, normalizeLineEndings(expected), "Actual result: " + actual);
  }

  @NotNull
  private String normalizeLineEndings(@NotNull String text) {
    return text.replace("\r", "");
  }

  @NotNull
  protected String prepareResult() {
    return unifySlashes(myResult.toString()).replace(unifySlashes(myBaseDir.getAbsolutePath()), "##BASE_DIR##").replace(unifySlashes(myBaseDir.getPath()), "##BASE_DIR##");
  }

  @NotNull
  protected String unifySlashes(@NotNull String s) {
    return s.replace("\\", "/");
  }

  protected InspectionReporter getInspectionReporter() {
    return myInspectionReporter;
  }

  protected DuplicationReporter getDuplicationReporter() {
    return myDuplicationReporter;
  }

  public TestReporter getTestReporter() {
    return myTestReporter;
  }

  protected File getBaseDir() {
    return myBaseDir;
  }

  @NotNull
  protected abstract Parser getParser();

  @NotNull
  protected abstract String getReportDir();
}