/*
 * Copyright 2008 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.*;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin.LOGGER;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;


public abstract class InspectionslReportParser extends XmlReportParser {
  protected final InspectionReporter myInspectionReporter;
  protected final String myCheckoutDirectory;
  private Set<String> myReportedInstanceTypes;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  protected InspectionInstance myCurrentBug;

  public InspectionslReportParser(@NotNull final BaseServerLoggerFacade logger,
                                  @NotNull InspectionReporter inspectionReporter,
                                  @NotNull String checkoutDirectory) {
    super(logger);
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory.replace("\\", File.separator).replace("/", File.separator);
    myReportedInstanceTypes = new HashSet<String>();
    myErrors = 0;
    myWarnings = 0;
    myInfos = 0;
  }

  public static String generateBuildStatus(int errors, int warnings, int infos) {
    return "Errors: " + errors + ", warnings: " + warnings + ", information: " + infos;
  }

  void logReportTotals(@NotNull File report) {
    String message = report.getPath() + " report processed";
    if (myErrors > 0) {
      message = message.concat(": " + myErrors + " errors(s)");
    }
    if (myWarnings > 0) {
      message = message.concat(": " + myWarnings + " warnings(s)");
    }
    myLogger.message(message);
    LOGGER.debug(message);
  }

  protected void logParsingTotals(@NotNull Map<String, String> parameters) {
    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters);
    if ((errorLimit != -1) && (myErrors > errorLimit)) {
      myLogger.error("Errors limit reached: found " + myErrors + " errors, limit " + errorLimit);
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters);
    if ((warningLimit != -1) && (myWarnings > warningLimit)) {
      myLogger.error("Warnings limit reached: found " + myWarnings + " warnings, limit " + warningLimit);
      limitReached = true;
    }

    final String buildStatus = generateBuildStatus(myErrors, myWarnings, myInfos);
    myLogger.message("##teamcity[buildStatus status='" +
      (limitReached ? "FAILURE" : "SUCCESS") +
      "' text='" + buildStatus + "']");
  }

  protected void processPriority(int priority) {
    InspectionSeverityValues level;
    switch (priority) {
      case 1:
        ++myErrors;
        level = InspectionSeverityValues.ERROR;
        break;
      case 2:
        ++myWarnings;
        level = InspectionSeverityValues.WARNING;
        break;
      default:
        ++myInfos;
        level = InspectionSeverityValues.INFO;
    }
    final Collection<String> attrValue = new Vector<String>();
    attrValue.add(level.toString());
    myCurrentBug.addAttribute(InspectionAttributesId.SEVERITY.toString(), attrValue);
  }

  protected void reportInspectionType(String id, String name, String category, String descr) {
    if (myReportedInstanceTypes.contains(id)) {
      return;
    }
    final InspectionTypeInfo type = new InspectionTypeInfo();
    type.setId(id);
    type.setName(name);
    type.setCategory(category);
    type.setDescription(descr);
    myInspectionReporter.reportInspectionType(type);
    myReportedInstanceTypes.add(id);
  }

  public abstract int parse(@NotNull File report, int testsToSkip);
}
