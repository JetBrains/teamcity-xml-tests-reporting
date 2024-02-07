

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Rassokhin
 */
public class CTestFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "ctest";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }

  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new CTestReportParser(parameters.getTestReporter());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestParsingResult.createEmptyResult();
  }
}