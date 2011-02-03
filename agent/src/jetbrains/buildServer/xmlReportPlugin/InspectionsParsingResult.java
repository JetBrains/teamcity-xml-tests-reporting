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

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.messages.serviceMessages.BuildStatus;
import org.jetbrains.annotations.NotNull;

import java.io.File;

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

  public void logAsFileResult(@NotNull File file,
                              @NotNull ParseParameters parameters) {
    final StringBuilder message = new StringBuilder(file.getAbsolutePath()).append(" report processed: ");

    message.append(myErrors).append(" error").append(getEnding(myErrors));
    message.append(", ");
    message.append(myWarnings).append(" warning").append(getEnding(myWarnings));
    message.append(", ");
    message.append(myInfos).append(" info message").append(getEnding(myInfos));

    if (parameters.isVerbose()) {
      parameters.getThreadLogger().message(message.toString());
    }

    LoggingUtils.LOG.debug(message.toString());
  }

  public void logAsTotalResult(@NotNull ParseParameters parameters) {
    final BuildProgressLogger logger = parameters.getThreadLogger();

    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters.getParameters());
    if ((errorLimit != -1) && (myErrors > errorLimit)) {
      logger.error("Errors limit " + errorLimit + " reached: found " + myErrors + " error" + getEnding(myErrors));
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters.getParameters());
    if ((warningLimit != -1) && (myWarnings > warningLimit)) {
      logger.error("Warnings limit " + warningLimit + " reached: found " + myWarnings + " warning" + getEnding(myWarnings));
      limitReached = true;
    }

    if (limitReached) {
      logger.message(new BuildStatus(generateBuildStatus(myErrors, myWarnings, myInfos), Status.FAILURE).asString());
    }
  }

  @NotNull
  private static String generateBuildStatus(int errors, int warnings, int infos) {
    return "Errors: " + errors + ", warnings: " + warnings + ", information: " + infos;
  }

  @NotNull
  private static String getEnding(int number) {
    return (number == 1 ? "" : "s");
  }
}
