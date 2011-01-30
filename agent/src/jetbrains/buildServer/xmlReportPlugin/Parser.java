package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 11:52
 */

/**
 * Report parser
 */
public interface Parser {
  /**
   * Parses the specified file
   * @param file file to parse
   * @param prevResult previous parsing result if available
   * @return true if file is fully parsed and doesn't need more parsing, false otherwise
   * @throws ParsingException if parser comes across critical error
   */
  boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException;

  /**
   * Gets parser implementation specific parsing result
   * @return parsing result which can be null if parser hasn't yet parsed anything
   */
  @Nullable ParsingResult getParsingResult();
}
