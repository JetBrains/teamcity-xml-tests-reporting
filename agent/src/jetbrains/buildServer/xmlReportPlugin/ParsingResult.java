package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 22:29
 */
public interface ParsingResult {
  /**
   * Accumulate the given result into current
   * @param parsingResult te result to accumulate
   */
  public void accumulate(@NotNull ParsingResult parsingResult);

  /**
   * Processes result of parsing the file
   * @param file file
   * @param parameters additional parameters
   */
  void logAsFileResult(@NotNull File file,
                       @NotNull ParseParameters parameters);

  /**
   * Processes total parsing result
   * @param parameters additional parameters
   */
  void logAsTotalResult(@NotNull ParseParameters parameters);
}
