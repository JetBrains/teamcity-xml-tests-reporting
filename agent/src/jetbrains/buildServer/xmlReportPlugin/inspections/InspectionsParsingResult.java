package jetbrains.buildServer.xmlReportPlugin.inspections;

import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 17:49
 */
public class InspectionsParsingResult implements ParsingResult {
  private int myErrors;
  private int myWarnings;
  private int myInfos;

  public InspectionsParsingResult(int errors, int warnings, int infos) {
    myErrors = errors;
    myWarnings = warnings;
    myInfos = infos;
  }

  public int getErrors() {
    return myErrors;
  }

  public int getWarnings() {
    return myWarnings;
  }

  public int getInfos() {
    return myInfos;
  }

  public void accumulate(@NotNull ParsingResult parsingResult) {
    final InspectionsParsingResult inspectionsParsingResult = (InspectionsParsingResult) parsingResult;
    myErrors += inspectionsParsingResult.getErrors();
    myWarnings += inspectionsParsingResult.getWarnings();
    myInfos += inspectionsParsingResult.getInfos();
  }

  @NotNull
  public static InspectionsParsingResult createEmptyResult() {
    return new InspectionsParsingResult(0, 0, 0);
  }
}
