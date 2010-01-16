/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin.LOG;


public abstract class InspectionsReportParser extends XmlReportParser {
  protected final InspectionReporter myInspectionReporter;
  protected final String myCheckoutDirectory;
  private final Set<String> myReportedInstanceTypes;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  private int myTotalErrors;
  private int myTotalWarnings;
  private int myTotalInfos;

  protected InspectionInstance myCurrentBug;

  protected InspectionsReportParser(@NotNull final BaseServerLoggerFacade logger,
                                    @NotNull InspectionReporter inspectionReporter,
                                    @NotNull String checkoutDirectory) {
    super(logger);
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory.replace("\\", File.separator).replace("/", File.separator);
    myReportedInstanceTypes = new HashSet<String>();
  }

  private static String generateBuildStatus(int errors, int warnings, int infos) {
    return "Errors: " + errors + ", warnings: " + warnings + ", information: " + infos;
  }

  public void logReportTotals(@NotNull File report, boolean verbose) {
    String message = report.getPath() + " report processed";
    if (myErrors > 0) {
      message = message.concat(": " + myErrors + " error(s)");
    }
    if (myWarnings > 0) {
      message = message.concat(": " + myWarnings + " warning(s)");
    }
    if (myInfos > 0) {
      message = message.concat(": " + myInfos + " info message(s)");
    }
    if (verbose) {
      myLogger.message(message);
    }
    LOG.debug(message);
    myTotalErrors += myErrors;
    myTotalWarnings += myWarnings;
    myTotalInfos += myInfos;
    myErrors = 0;
    myWarnings = 0;
    myInfos = 0;
  }

  protected void logParsingTotals(@NotNull Map<String, String> parameters, boolean verbose) {
    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters);
    if ((errorLimit != -1) && (myTotalErrors > errorLimit)) {
      myLogger.error("Errors limit reached: found " + myTotalErrors + " errors, limit " + errorLimit);
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters);
    if ((warningLimit != -1) && (myTotalWarnings > warningLimit)) {
      myLogger.error("Warnings limit reached: found " + myTotalWarnings + " warnings, limit " + warningLimit);
      limitReached = true;
    }

    if (limitReached) {
      myLogger.message("##teamcity[buildStatus status='FAILURE' " + "text='" + generateBuildStatus(myTotalErrors, myTotalWarnings, myTotalInfos) + "']");
    }
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

  protected String resolveSourcePath(String path) {
    path = path.replace("\\", File.separator).replace("/", File.separator);
    if (path.startsWith(myCheckoutDirectory)) {
      path = path.substring(myCheckoutDirectory.length());
    }
    if (path.startsWith(File.separator)) {
      path = path.substring(1);
    }
    return path.replace(File.separator, "/");
  }

  public abstract void parse(@NotNull ReportData data);
}
