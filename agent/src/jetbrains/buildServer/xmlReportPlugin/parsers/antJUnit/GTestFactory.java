

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import org.jetbrains.annotations.NotNull;

public class GTestFactory extends AntJUnitFactory {
  @NotNull
  @Override
  public String getType() {
    return "gtest";
  }

  @NotNull
  @Override
  public ParsingStage getParsingStage() {
    return ParsingStage.RUNTIME;
  }
}