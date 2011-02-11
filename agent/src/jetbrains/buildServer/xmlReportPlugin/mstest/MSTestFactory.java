package jetbrains.buildServer.xmlReportPlugin.mstest;

import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 09.02.11
 * Time: 13:36
 */
public class MSTestFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull final ParseParameters parameters) {
    return new MSTestTRXParser(new TeamCityTestsResultsWriter(parameters.getThreadLogger()));
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestsParsingResult.createEmptyResult();
  }
}
