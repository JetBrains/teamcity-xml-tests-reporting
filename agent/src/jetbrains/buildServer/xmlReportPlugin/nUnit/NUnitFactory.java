package jetbrains.buildServer.xmlReportPlugin.nUnit;

import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.tests.TestsParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:05
 */
public class NUnitFactory implements ParserFactory {
  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new NUnitReportParser(parameters.getXmlReader(), parameters.getInternalizingThreadLogger(),
      XmlReportPluginUtil.getNUnitSchemaPath(parameters.getParameters()), parameters.getTempDir());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return TestsParsingResult.createEmptyResult();
  }
}
