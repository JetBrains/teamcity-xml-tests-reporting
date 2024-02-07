

package jetbrains.buildServer.xmlReportPlugin.parsers.jslint;

import jetbrains.buildServer.serverSide.TeamCityProperties;
import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 06.05.11
 * Time: 18:33
 */
public class JSLintFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "jslint";
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
  public Parser createParser(@NotNull final ParseParameters parameters) {
    return new JSLintReportParser(parameters.getInspectionReporter());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return InspectionParsingResult.createEmptyResult();
  }
}