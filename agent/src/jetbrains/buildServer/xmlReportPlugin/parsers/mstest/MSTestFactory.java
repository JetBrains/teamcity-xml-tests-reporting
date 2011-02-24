package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.tests.TeamCityTestReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 09.02.11
 * Time: 13:36
 */
public class MSTestFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull final ParseParameters parameters) {
    return new MSTestTRXParser(new TeamCityTestReporter(parameters.getThreadLogger()));
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestParsingResult.createEmptyResult();
  }
}
