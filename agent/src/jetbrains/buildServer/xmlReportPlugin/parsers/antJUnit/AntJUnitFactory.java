

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 12:57
 */
public class AntJUnitFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "junit";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }

  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new AntJUnitReportParser(parameters.getTestReporter(), new SecondDurationParser(),
                                    XmlReportPluginUtil.isLogInternalSystemError(parameters.getParameters()));
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestParsingResult.createEmptyResult();
  }
}