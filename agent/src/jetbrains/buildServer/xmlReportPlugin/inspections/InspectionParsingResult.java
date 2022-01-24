/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.inspections;

import java.io.File;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 17:49
 */
public class InspectionParsingResult extends ProblemParsingResult {
  private int myErrors;
  private int myWarnings;
  private int myInfos;

  public InspectionParsingResult(int errors, int warnings, int infos) {
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
    final InspectionParsingResult inspectionParsingResult = (InspectionParsingResult) parsingResult;
    myErrors += inspectionParsingResult.getErrors();
    myWarnings += inspectionParsingResult.getWarnings();
    myInfos += inspectionParsingResult.getInfos();
  }

  @NotNull
  public static InspectionParsingResult createEmptyResult() {
    return new InspectionParsingResult(0, 0, 0);
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

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters.getParameters());
    if ((errorLimit != -1) && (myErrors > errorLimit)) {
      final String prefix = "Inspection errors limit " + errorLimit + " reached";
      logger.error(prefix + ": found " + myErrors + " error" + getEnding(myErrors));
      logger.logBuildProblem(createBuildProblem("err" + myErrors, prefix));
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters.getParameters());
    if ((warningLimit != -1) && (myWarnings > warningLimit)) {
      final String prefix = "Inspection warnings limit " + warningLimit + " reached";
      logger.error(prefix + ": found " + myWarnings + " warning" + getEnding(myWarnings));
      logger.logBuildProblem(createBuildProblem("warn" + myWarnings, prefix));
    }
  }

  @NotNull
  private static BuildProblemData createBuildProblem(@NotNull String id, @NotNull String descr) {
    return BuildProblemData.createBuildProblem(id, XmlReportPluginConstants.BUILD_PROBLEM_TYPE, descr);
  }

  @NotNull
  private static String getEnding(int number) {
    return (number == 1 ? "" : "s");
  }

  @Override
  public Throwable getProblem() {
    return null;
  }
}
