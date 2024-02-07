

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Rassokhin
 */
public class CTestReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CTestReportParser.class);

  @NotNull
  private final TestReporter myTestReporter;

  private int myLoggedTests;

  @Nullable
  private ParsingException myParsingException;

  public CTestReportParser(@NotNull final TestReporter testReporter) {
    myTestReporter = testReporter;
  }

  @Override
  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    try {
      createTestXmlParser(file).parse(file);
      return true;
    } catch (IOException e) {
      myParsingException = new ParsingException(e);
      LOG.debug("Couldn't completely parse " + file
          + " report, exception occurred: " + e + ", " + myLoggedTests + " tests logged");
    }

    return false;
  }

  private TestXmlReportParser createTestXmlParser(final File file) {
    return new TestXmlReportParser(new TestXmlReportParser.Callback() {
      @Override
      public void testFound(@NotNull final TestData testData) {
        try {
          final String testName = testData.getName();
          final TestData.Status status = testData.getStatus();
          // Currently I cannot know what to say about log. is it from StdErr or StdOut.
          // Assuming as StdErr if build failed and StdOut otherwise.
          final String log = testData.getLog();

          if (testName == null) {
            myTestReporter.warning("File " + file + " contains unnamed test");
            return;
          }

          myTestReporter.openTest(testName);
          switch (status) {
            case NOT_RUN:
              myTestReporter.testIgnored("");
              break;
            case COMPLETED:
              if (log != null && !StringUtil.isEmptyOrSpaces(log)) {
                myTestReporter.testStdOutput(log);
              }
              break;
            case FAILED:
              if (log != null && !StringUtil.isEmptyOrSpaces(log)) {
                myTestReporter.testErrOutput(log);
              }
              myTestReporter.testFail(testData.getFailureMessage(), null);
              break;
          }
          myTestReporter.closeTest(testData.getDuration());

        } finally {
          ++myLoggedTests;
        }
      }

      @Override
      public void error(@NotNull final String message) {
        myTestReporter.error(message);
      }

      @Override
      public void unexpectedFormat(@NotNull final String msg) {
        myTestReporter.error("File " + file + " doesn't match the expected format: " + msg + "\nPlease check CTest documentation for the supported schema");
      }
    });
  }

  @Override
  public ParsingResult getParsingResult() {
    return new TestParsingResult(myLoggedTests > 0 ? 1 : 0, myLoggedTests, myParsingException);
  }
}