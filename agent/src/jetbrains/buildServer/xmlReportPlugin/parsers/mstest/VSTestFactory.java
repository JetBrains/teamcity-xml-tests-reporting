

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import org.jetbrains.annotations.NotNull;

public class VSTestFactory extends TRXFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "vstest";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }

  @NotNull
  @Override
  protected String getDefaultSuiteName() {
    return "VSTest";
  }
}