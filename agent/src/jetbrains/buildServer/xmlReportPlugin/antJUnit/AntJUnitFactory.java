package jetbrains.buildServer.xmlReportPlugin.antJUnit;

import jetbrains.buildServer.xmlReportPlugin.*;
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
