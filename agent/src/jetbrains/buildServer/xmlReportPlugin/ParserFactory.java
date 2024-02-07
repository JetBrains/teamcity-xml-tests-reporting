

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.AgentExtension;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 23:19
 */
public interface ParserFactory extends AgentExtension {
  String TEAMCITY_PROPERTY_STAGE_PREFIX = "teamcity.xmlReport.parsingStage";

  @NotNull String getType();
  @NotNull Parser createParser(@NotNull ParseParameters parameters);
  @NotNull ParsingResult createEmptyResult();
  @NotNull ParsingStage getParsingStage();

  enum ParsingStage{
    RUNTIME, BEFORE_FINISH;

    @Nullable
    public static ParsingStage of(@Nullable final String name) {
      if (StringUtil.isEmptyOrSpaces(name)) return null;
      for(ParsingStage stage: values()) {
        if (stage.name().equalsIgnoreCase(name)) {
          return stage;
        }
      }
      return null;
    }
  }
}