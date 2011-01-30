package jetbrains.buildServer.xmlReportPlugin.findBugs;

import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionsParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionsResultProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 23.01.11
 * Time: 20:32
 */
public class FindBugsFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new FindBugsReportParser(parameters.getXmlReader(), parameters.getInspectionReporter(),
      parameters.getCheckoutDir(), XmlReportPluginUtil.getFindBugsHomePath(parameters.getParameters()),
      parameters.getThreadLogger());
  }

  @NotNull
  public ResultProcessor createResultsProcessor() {
    return new InspectionsResultProcessor();
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return InspectionsParsingResult.createEmptyResult();
  }
}
