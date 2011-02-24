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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatesReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 27.08.2010
 * Time: 16:50:03
 */
public class PmdCpdReportParser implements Parser {
  @NotNull
  private final DuplicatesReporter myDuplicatesReporter;

  public PmdCpdReportParser(@NotNull DuplicatesReporter duplicatesReporter) {
    myDuplicatesReporter = duplicatesReporter;
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "pmd-cpd")) {
      return false;
    }

    try {
      new PmdCpdXmlReportParser(new PmdCpdXmlReportParser.Callback() {
        public void startDuplicates() {
          myDuplicatesReporter.startDuplicates();
        }

        public void finishDuplicates() {
          myDuplicatesReporter.finishDuplicates();
        }

        public void reportDuplicate(@NotNull DuplicationResult duplicate) {
          myDuplicatesReporter.reportDuplicate(duplicate);
        }
      }).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new PmdCpdParsingResult();
  }
}