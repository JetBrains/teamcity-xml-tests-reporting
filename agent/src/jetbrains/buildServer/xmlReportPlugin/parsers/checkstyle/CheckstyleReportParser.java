

package jetbrains.buildServer.xmlReportPlugin.parsers.checkstyle;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 23.12.2009
 * Time: 16:11:49
 */
class CheckstyleReportParser implements Parser {
  @NotNull
  private final InspectionReporter myInspectionReporter;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  public CheckstyleReportParser(@NotNull final InspectionReporter inspectionReporter) {
    myInspectionReporter = inspectionReporter;
  }

  @Override
  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "checkstyle")) {
      return false;
    }

    try {
      new CheckstyleXmlReportParser(new CheckstyleXmlReportParser.Callback() {
        public void reportInspection(@NotNull final InspectionResult inspection) {
          switch (inspection.getPriority()) {
            case 1:
              ++myErrors;
              break;
            case 2:
              ++myWarnings;
              break;
            default:
              ++myInfos;
          }
          myInspectionReporter.reportInspection(inspection);
        }

        public void reportInspectionType(@NotNull final InspectionTypeResult inspectionType) {
          myInspectionReporter.reportInspectionType(inspectionType);
        }

        public void reportException(@NotNull final String message) {
          myInspectionReporter.error("Exception in report " + file.getAbsolutePath() + "\n" + message);
        }

        @Override
        public void error(@NotNull final String message) {
          myInspectionReporter.error(message);
        }
      }).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new InspectionParsingResult(myErrors, myWarnings, myInfos);
  }
}