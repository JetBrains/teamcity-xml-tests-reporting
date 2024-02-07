

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import org.jetbrains.annotations.NotNull;

public class SurefireFactory extends AntJUnitFactory {
  @NotNull
  @Override
  public String getType() {
    return "surefire";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }
}