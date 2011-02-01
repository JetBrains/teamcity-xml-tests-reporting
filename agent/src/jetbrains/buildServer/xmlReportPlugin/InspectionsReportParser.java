/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
import jetbrains.buildServer.agent.inspections.*;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public abstract class InspectionsReportParser extends XmlReportParser implements Parser {
  @NotNull
  protected InspectionReporter myInspectionReporter;
  @NotNull
  protected String myCheckoutDirectory;
  @NotNull
  protected BuildProgressLogger myLogger;

  @NotNull
  private final Set<String> myReportedInstanceTypes;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  protected InspectionInstance myCurrentBug;

  protected InspectionsReportParser(@NotNull XMLReader xmlReader,
                                    @NotNull InspectionReporter inspectionReporter,
                                    @NotNull File checkoutDirectory,
                                    @NotNull BuildProgressLogger logger,
                                    boolean useCData) {
    super(xmlReader, useCData);
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory.getAbsolutePath();
    myLogger = logger;
    myReportedInstanceTypes = new HashSet<String>();
  }

  public ParsingResult getParsingResult() {
    return new InspectionsParsingResult(myErrors, myWarnings, myInfos);
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

  protected void reportInspectionType(String id, String name, String category, String descr, InspectionReporter inspectionReporter) {
    if (myReportedInstanceTypes.contains(id)) {
      return;
    }
    final InspectionTypeInfo type = new InspectionTypeInfo();
    type.setId(id);
    type.setName(name);
    type.setCategory(category);
    type.setDescription(descr);
    inspectionReporter.reportInspectionType(type);
    myReportedInstanceTypes.add(id);
  }

  protected String getRelativePath(String path, String baseDir) {
    baseDir = baseDir.replace("\\", "/");
    path = path.replace("\\", "/");

    return path.startsWith(baseDir) ? FileUtil.getRelativePath(baseDir, path, '/') : path;
  }

  protected static int getNumber(String number) {
    if (number != null) {
      try {
        return Integer.parseInt(number);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }
}