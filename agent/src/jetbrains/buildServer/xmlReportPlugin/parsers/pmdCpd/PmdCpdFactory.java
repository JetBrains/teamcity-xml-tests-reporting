

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import jetbrains.buildServer.serverSide.TeamCityProperties;
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
  @Override
  public String getType() {
    return "pmdCpd";
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
    return new PmdCpdReportParser(parameters.getDuplicationReporter(), parameters.getCheckoutDir());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return new PmdCpdParsingResult();
  }
}