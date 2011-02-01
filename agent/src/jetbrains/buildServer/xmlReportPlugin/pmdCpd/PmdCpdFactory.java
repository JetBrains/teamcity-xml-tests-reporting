package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 17:08
 */
public class PmdCpdFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new PmdCpdReportParser(parameters.getXmlReader(), parameters.getDuplicatesReporter(),
      parameters.getCheckoutDir());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return new PmdCpdParsingResult();
  }
}
