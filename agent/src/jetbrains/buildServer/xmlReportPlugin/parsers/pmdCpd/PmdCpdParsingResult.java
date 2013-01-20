/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.ProblemParsingResult;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 01.02.11
 * Time: 20:01
 */
class PmdCpdParsingResult extends ProblemParsingResult {
  public void accumulate(@NotNull ParsingResult parsingResult) {
  }

  public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {
    final String message = file.getAbsolutePath() + " report processed: ";

    if (parameters.isVerbose()) {
      parameters.getThreadLogger().message(message);
    }

    LoggingUtils.LOG.debug(message);
  }

  public void logAsTotalResult(@NotNull ParseParameters parameters) {
  }
}
