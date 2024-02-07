

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.util.Map;
import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 23.01.11
 * Time: 20:32
 */
public class FindBugsFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "findBugs";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    final String stageName = TeamCityProperties.getPropertyOrNull(TEAMCITY_PROPERTY_STAGE_PREFIX + "." + getType());
    final ParsingStage stage = ParsingStage.of(stageName);
    if (stage != null) return stage;
    else return ParsingStage.BEFORE_FINISH;
  }

  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    final Map<String,String> params = parameters.getParameters();
    return new FindBugsReportParser(parameters.getInspectionReporter(), XmlReportPluginUtil.getFindBugsHomePath(params),
                                    parameters.getCheckoutDir(), XmlReportPluginUtil.isFindBugsLookupFiles(params));
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return InspectionParsingResult.createEmptyResult();
  }
}