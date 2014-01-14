/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import java.util.Collection;
import java.util.Vector;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionAttributesId;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionSeverityValues;
import jetbrains.buildServer.xmlReportPlugin.utils.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 13:26
 */
public class TeamCityInspectionReporter implements InspectionReporter {
  @NotNull
  private final jetbrains.buildServer.agent.inspections.InspectionReporter myInspectionReporter;

  @NotNull
  private final BuildProgressLogger myLogger;

  @NotNull
  private final File myBaseFolder;

  public TeamCityInspectionReporter(@NotNull jetbrains.buildServer.agent.inspections.InspectionReporter inspectionReporter,
                                    @NotNull BuildProgressLogger logger,
                                    @NotNull File baseFolder) {
    myInspectionReporter = inspectionReporter;
    myLogger = logger;
    myBaseFolder = baseFolder;
  }

  public void reportInspection(@NotNull final InspectionResult inspection) {
    final InspectionInstance inspectionInstance = new InspectionInstance();

    inspectionInstance.setFilePath(PathUtils.getRelativePath(myBaseFolder.getAbsolutePath(), inspection.getFilePath()));
    inspectionInstance.setLine(inspection.getLine());
    inspectionInstance.setMessage(getValueOrUnknown(inspection.getMessage()));
    inspectionInstance.setInspectionId(getValueOrUnknown(inspection.getInspectionId()));

    InspectionSeverityValues level;
    switch (inspection.getPriority()) {
      case 1:
        level = InspectionSeverityValues.ERROR;
        break;
      case 2:
        level = InspectionSeverityValues.WARNING;
        break;
      default:
        level = InspectionSeverityValues.INFO;
    }

    final Collection<String> attrValue = new Vector<String>();
    attrValue.add(level.toString());
    inspectionInstance.addAttribute(InspectionAttributesId.SEVERITY.toString(), attrValue);

    myInspectionReporter.reportInspection(inspectionInstance);
  }

  public void reportInspectionType(@NotNull final InspectionTypeResult inspectionType) {
    final jetbrains.buildServer.agent.inspections.InspectionTypeInfo inspectionTypeInfo = new jetbrains.buildServer.agent.inspections.InspectionTypeInfo();

    inspectionTypeInfo.setId(getValueOrUnknown(inspectionType.getId()));
    inspectionTypeInfo.setName(getValueOrUnknown(inspectionType.getName()));
    inspectionTypeInfo.setCategory(getValueOrUnknown(inspectionType.getCategory()));
    inspectionTypeInfo.setDescription(getValueOrUnknown(inspectionType.getDescription()));

    myInspectionReporter.reportInspectionType(inspectionTypeInfo);
  }

  @NotNull
  private String getValueOrUnknown(@Nullable String val) {
    return val == null || val.trim().length() == 0 ? "<unknown>" : val;
  }

  public void info(@NotNull final String message) {
    myLogger.message(message);
  }

  public void warning(@NotNull final String message) {
    myLogger.warning(message);
  }

  public void error(@NotNull final String message) {
    myLogger.error(message);
  }
}
