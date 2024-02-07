

package jetbrains.buildServer.xmlReportPlugin.parsers.jslint;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.*;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 06.05.11
 * Time: 18:34
 */
class JSLintReportParser implements Parser {
  @NotNull
  private final InspectionReporter myInspectionReporter;

  private int myWarnings;

  JSLintReportParser(@NotNull final InspectionReporter inspectionReporter) {
    myInspectionReporter = inspectionReporter;
  }

  @Override
  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "jslint")) {
      return false;
    }

    try {
      new JSLintXmlReportParser(new JSLintXmlReportParser.Callback() {
        @Override
        public void markBuildAsInspectionsBuild() {
          if (myInspectionReporter instanceof TeamCityInspectionReporter) {
            ((TeamCityInspectionReporter)myInspectionReporter).markBuildAsInspectionsBuild();
          }
        }

        public void reportInspection(@NotNull final InspectionResult inspection) {
          ++myWarnings;
          myInspectionReporter.reportInspection(inspection);
        }

        public void reportInspectionType(@NotNull final InspectionTypeResult inspectionType) {
          myInspectionReporter.reportInspectionType(inspectionType);
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
    return new InspectionParsingResult(0, myWarnings, 0);
  }
}