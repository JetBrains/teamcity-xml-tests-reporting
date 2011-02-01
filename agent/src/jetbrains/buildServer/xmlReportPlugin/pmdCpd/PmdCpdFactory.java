package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 17:08
 */
public class PmdCpdFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new PmdCpdReportParser(parameters.getXmlReader(), parameters.getDuplicatesReporter(),
      parameters.getCheckoutDir());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return new ParsingResult() {
      public void accumulate(@NotNull ParsingResult parsingResult) {
      }

      public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {
        final String message = file.getAbsolutePath() + " report processed: ";

        if (parameters.isVerbose()) {
          parameters.getThreadLogger().message(message);
        }

        LoggingUtils.LOG.debug(message);
      }

      public void logAsTotalResult(@NotNull ParseParameters parameters) {
      }
    };
  }
}
