package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 22:29
 */
public interface ParsingResult {
  public void accumulate(@NotNull ParsingResult parsingResult);
}
