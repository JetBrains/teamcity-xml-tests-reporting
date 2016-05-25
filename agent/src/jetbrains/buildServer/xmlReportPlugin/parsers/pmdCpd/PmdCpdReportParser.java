/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 27.08.2010
 * Time: 16:50:03
 */
class PmdCpdReportParser implements Parser {
  @NotNull
  private final DuplicationReporter myDuplicationReporter;
  private final File myCheckoutDirectory;

  public PmdCpdReportParser(@NotNull DuplicationReporter duplicationReporter, final File checkoutDirectory) {
    myDuplicationReporter = duplicationReporter;
    myCheckoutDirectory = checkoutDirectory;
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "pmd-cpd")) {
      return false;
    }

    try {
      new PmdCpdXmlReportParser(new PmdCpdXmlReportParser.Callback() {
        public void startDuplicates() {
          myDuplicationReporter.startDuplicates();
        }

        public void finishDuplicates() {
          myDuplicationReporter.finishDuplicates();
        }

        public void reportDuplicate(@NotNull DuplicationResult duplicate) {
          myDuplicationReporter.reportDuplicate(duplicate);
        }
      }, myCheckoutDirectory.getAbsolutePath()).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new PmdCpdParsingResult();
  }
}