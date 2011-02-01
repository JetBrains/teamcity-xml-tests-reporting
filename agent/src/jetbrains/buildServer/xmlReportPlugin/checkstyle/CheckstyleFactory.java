package jetbrains.buildServer.xmlReportPlugin.checkstyle;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionsParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:04
 */
public class CheckstyleFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new CheckstyleReportParser(parameters.getXmlReader(), parameters.getInspectionReporter(),
      parameters.getCheckoutDir(), parameters.getThreadLogger());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return InspectionsParsingResult.createEmptyResult();
  }
}
