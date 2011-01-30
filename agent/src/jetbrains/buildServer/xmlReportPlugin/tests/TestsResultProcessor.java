package jetbrains.buildServer.xmlReportPlugin.tests;

import jetbrains.buildServer.xmlReportPlugin.LoggingUtils;
import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.ResultProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:34
 */
public class TestsResultProcessor implements ResultProcessor {
  public void processResult(@NotNull File file, @NotNull ParsingResult result, @NotNull ParseParameters parameters) {
    final TestsParsingResult testsParsingResult = (TestsParsingResult) result;

    String message = file.getAbsolutePath() + " report processed";
    if (testsParsingResult.getSuites() > 0) {
      message = message.concat(": " + testsParsingResult.getSuites() + " suite(s)");
      if (testsParsingResult.getTests() > 0) {
        message = message.concat(", " + testsParsingResult.getTests() + " test(s)");
      }
    }
    if (parameters.isVerbose()) {
      parameters.getThreadLogger().message(message);
    }
    LoggingUtils.LOG.debug(message);
  }

  public void processTotalResult(@NotNull ParsingResult result, @NotNull ParseParameters parameters) {

  }
}
