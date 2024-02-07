

package jetbrains.buildServer.xmlReportPlugin.parsers.testng;

import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.tests.MillisecondDurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;

public class TestNGFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "testng";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }

  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new TestNGReportParser(parameters.getTestReporter(), new MillisecondDurationParser());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestParsingResult.createEmptyResult();
  }
}