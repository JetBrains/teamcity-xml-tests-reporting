

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Oleg Rybak (oleg.rybak@jetbrains.com)
 */
public class TRXFactory implements ParserFactory {

  private static final String DEFAULT_TEST_SUITE = "TRX";

  @NotNull
  @Override
  public String getType() {
    return "trx";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }

  @NotNull
  public final Parser createParser(@NotNull final ParseParameters parameters) {
    return new TRXParser(parameters.getTestReporter(), getDefaultSuiteName());
  }

  @NotNull
  public final ParsingResult createEmptyResult() {
    return TestParsingResult.createEmptyResult();
  }

  @NotNull
  protected String getDefaultSuiteName() {
    return DEFAULT_TEST_SUITE;
  }
}