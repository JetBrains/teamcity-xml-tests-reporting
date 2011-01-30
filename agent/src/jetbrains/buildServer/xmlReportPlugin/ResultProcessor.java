package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 13:53
 */

/**
 * Specific parser results processor
 */
public interface ResultProcessor {
  /**
   * Processes result of parsing the file
   * @param file file
   * @param result result
   * @param parameters additional parameters
   */
  void processResult(@NotNull File file,
                     @NotNull ParsingResult result,
                     @NotNull ParseParameters parameters);

  /**
   * Processes total parsing result
   * @param result result
   * @param parameters additional parameters
   */
  void processTotalResult(@NotNull ParsingResult result,
                          @NotNull ParseParameters parameters);

  static final ResultProcessor DO_NOTHING_PROCESSOR =
    new ResultProcessor() {
      public void processResult(@NotNull File file,
                                @NotNull ParsingResult result,
                                @NotNull ParseParameters parameters) {
      }

      public void processTotalResult(@NotNull ParsingResult result,
                                     @NotNull ParseParameters parameters) {
      }
    };
}
