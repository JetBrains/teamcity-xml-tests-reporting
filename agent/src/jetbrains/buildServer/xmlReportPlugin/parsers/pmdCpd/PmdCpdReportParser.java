

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 27.08.2010
 * Time: 16:50:03
 */
class PmdCpdReportParser implements Parser {
  @NotNull
  private final DuplicationReporter myDuplicationReporter;
  private final File myCheckoutDirectory;

  public PmdCpdReportParser(@NotNull DuplicationReporter duplicationReporter, final File checkoutDirectory) {
    myDuplicationReporter = duplicationReporter;
    myCheckoutDirectory = checkoutDirectory;
  }

  @Override
  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "pmd-cpd")) {
      return false;
    }

    try {
      new PmdCpdXmlReportParser(new PmdCpdXmlReportParser.Callback() {
        public void startDuplicates() {
          myDuplicationReporter.startDuplicates();
        }

        public void finishDuplicates() {
          myDuplicationReporter.finishDuplicates();
        }

        public void reportDuplicate(@NotNull DuplicationResult duplicate) {
          myDuplicationReporter.reportDuplicate(duplicate);
        }

        @Override
        public void error(@NotNull final String message) {
          myDuplicationReporter.error(message);
        }
      }, myCheckoutDirectory.getAbsolutePath()).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new PmdCpdParsingResult();
  }
}