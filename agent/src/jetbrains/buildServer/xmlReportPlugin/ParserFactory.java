package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 23:19
 */
public interface ParserFactory {
  @NotNull Parser createParser(@NotNull ParseParameters parameters);
  @NotNull ParsingResult createEmptyResult();
}
