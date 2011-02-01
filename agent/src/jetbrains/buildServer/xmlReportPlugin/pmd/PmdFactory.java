package jetbrains.buildServer.xmlReportPlugin.pmd;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionsParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 23.01.11
 * Time: 20:23
 */
public class PmdFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new PmdReportParser(parameters.getXmlReader(), parameters.getInspectionReporter(),
      parameters.getCheckoutDir(), parameters.getThreadLogger());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return InspectionsParsingResult.createEmptyResult();
  }
}
