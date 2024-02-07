

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 22:29
 */
public interface ParsingResult {
  /**
   * Accumulate the given result into current
   * @param parsingResult the result to accumulate
   */
  void accumulate(@NotNull ParsingResult parsingResult);

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


  /**
   * Returns problem that occurred during parsing if any
   * @return Problem or null if no problem took place during parsing
   */
  @Nullable
  Throwable getProblem();


  /**
   * Sets problem that occurred during parsing if any
   * @param problem the problem
   */
  void setProblem(@NotNull Throwable problem);
}