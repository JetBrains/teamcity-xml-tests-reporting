/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.xmlReportPlugin.checkstyle;

import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 23.12.2009
 * Time: 16:11:49
 */
public class CheckstyleReportParser implements Parser {
  @NotNull
  private final InspectionReporter myInspectionReporter;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  public CheckstyleReportParser(@NotNull final InspectionReporter inspectionReporter) {
    myInspectionReporter = inspectionReporter;
  }

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
      }).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new InspectionsParsingResult(myErrors, myWarnings, myInfos);
  }
}