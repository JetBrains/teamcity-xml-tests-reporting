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

package jetbrains.buildServer.testReportParserPlugin.findBugs;

import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionTypeInfo;
import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;


public class FindBugsReportParser implements TestReportParser {
  public static final String TYPE = "findBugs";

  private final TestReportLogger myLogger;
  private final InspectionReporter myInspectionReporter;

  private final Set<String> myReportedInstanceTypes;
  private final Map<String, String> myCategories;

  private InspectionInstance myInspectionInstance = null;

  public FindBugsReportParser(@NotNull final TestReportLogger logger, @NotNull InspectionReporter inspectionReporter) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;

    myReportedInstanceTypes = new HashSet<String>();
    myCategories = new HashMap<String, String>();
  }

  public void parse(@NotNull File report) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      final Element root = new SAXBuilder().build(report).getRootElement();

      processBugCategories(root);
      processBugPatterns(root);
      processBugInstances(root);

    } catch (Exception e) {
      myLogger.exception(e);
    }
    myInspectionReporter.flush();
  }

  private void processBugInstances(Element root) {
    final List bugs = root.getChildren("BugInstance");
    for (Object o : bugs) {
      final Element bug = (Element) o;
      myInspectionInstance = new InspectionInstance();

      final String id = bug.getAttributeValue("type");
      if (id == null) {
        //illegal report
        return;
      }
      myInspectionInstance.setInspectionId(id);
      myInspectionInstance.setInspectionId(id);

      if (bug.getChild("ShortMessage") != null) {
        myInspectionInstance.setMessage(bug.getChild("ShortMessage").getText());
      } else if (bug.getChild("LongMessage") != null) {
        myInspectionInstance.setMessage(bug.getChild("LongMessage").getText());
      } else {
        myInspectionInstance.setMessage("<no description>");
      }

      final List lines = bug.getChildren("SourceLine");
      if (lines.size() != 0) {
        final Element line = (Element) lines.get(0);
        getLocationDetails(line);
      } else {
        final Element classElement = bug.getChild("Class");
        if (classElement == null) {
          //illegal report
          return;
        }
        final Element line = classElement.getChild("SourceLine");
        getLocationDetails(line);
      }

      myInspectionReporter.reportInspection(myInspectionInstance);
      myInspectionInstance = null;

      if (!myReportedInstanceTypes.contains(id)) {
        if (FindBugsPatterns.isCommonPattern(id)) {
          final InspectionTypeInfo type = new InspectionTypeInfo();

          type.setId(id);
          type.setName(FindBugsPatterns.getName(id));
          type.setCategory(FindBugsCategories.getName(FindBugsPatterns.getCategory(id)));
          type.setDescription(FindBugsPatterns.getDescription(id));

          myInspectionReporter.reportInspectionType(type);
          myReportedInstanceTypes.add(id);
        }
      }
    }
  }

  private void getLocationDetails(Element line) {
    if (line.getAttributeValue("start") != null) {
      myInspectionInstance.setLine(Integer.parseInt(line.getAttributeValue("start")));
    } else {
      myInspectionInstance.setLine(0);
    }
    if (line.getAttributeValue("sourcepath") != null) {
      myInspectionInstance.setFilePath(line.getAttributeValue("sourcepath"));
    } else {
      myInspectionInstance.setFilePath("<file path not available>");
    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    for (Object o : categories) {
      final Element category = (Element) o;

      final String id = category.getAttributeValue("category");
      final Element description = category.getChild("Description");
      if ((id == null) || FindBugsCategories.isCommonCategory(id) || myCategories.containsKey(id) || (description == null)) {
        //illegal report
        return;
      }
      myCategories.put(id, description.getText());
    }
  }

  private void processBugPatterns(Element root) {
    final List patterns = root.getChildren("BugPattern");
    for (Object o : patterns) {
      final Element pattern = (Element) o;

      final String id = pattern.getAttributeValue("type");
      final Element shortDescription = pattern.getChild("ShortDescription");
      final Element details = pattern.getChild("Details");
      String category = pattern.getAttributeValue("category");

      if ((id == null) || FindBugsPatterns.isCommonPattern(id)
        || (shortDescription == null) || (details == null) || (category == null)) {
        //illegal report
        return;
      }

      if (!myReportedInstanceTypes.contains(id)) {
        final InspectionTypeInfo type = new InspectionTypeInfo();

        type.setId(id);
        type.setName(shortDescription.getText());
        type.setDescription(details.getText());

        if (FindBugsCategories.isCommonCategory(category)) {
          category = FindBugsCategories.getName(category);
        } else {
          category = myCategories.get(category);
        }

        type.setCategory(category);

        myInspectionReporter.reportInspectionType(type);
        myReportedInstanceTypes.add(id);
      }
    }
  }

  public long parse(@NotNull File report, long testsToSkip) {
    parse(report);
    return -1;
  }

  public boolean abnormalEnd() {
    return false;
  }

  public void logReportTotals(File report) {
  }
}