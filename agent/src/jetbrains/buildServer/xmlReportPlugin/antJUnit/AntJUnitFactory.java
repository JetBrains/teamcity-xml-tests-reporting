package jetbrains.buildServer.xmlReportPlugin.antJUnit;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestsParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 12:57
 */
public class AntJUnitFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new AntJUnitReportParser(parameters.getXmlReader(), parameters.getInternalizingThreadLogger());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestsParsingResult.createEmptyResult();
  }
}
