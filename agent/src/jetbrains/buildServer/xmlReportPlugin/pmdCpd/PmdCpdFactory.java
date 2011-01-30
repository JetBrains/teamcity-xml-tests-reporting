package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import jetbrains.buildServer.xmlReportPlugin.*;
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
  public ResultProcessor createResultsProcessor() {
    return ResultProcessor.DO_NOTHING_PROCESSOR;
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return new ParsingResult() {
      public void accumulate(@NotNull ParsingResult parsingResult) {
      }
    };
  }
}
