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

  private final Map<String, String> myFilePaths;

  private InspectionInstance myInspectionInstance = null;

  public FindBugsReportParser(@NotNull final TestReportLogger logger, @NotNull InspectionReporter inspectionReporter) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;

    myReportedInstanceTypes = new HashSet<String>();
    myCategories = new HashMap<String, String>();

    myFilePaths = new HashMap<String, String>();
  }

  public void parse(@NotNull File report) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      final Element root = new SAXBuilder().build(report).getRootElement();

      processJars(root);
      processBugCategories(root);
      processBugPatterns(root);
      processBugInstances(root);

    } catch (Exception e) {
      myLogger.exception(e);
    }
    myInspectionReporter.flush();
  }

  private void processJars(Element root) {
    final Element project = root.getChild("Project");
    if (project == null) {
      //illegal report
      myLogger.error("Illegal report");
      return;
    }
    final List jars = project.getChildren("Jar");
    for (Object o : jars) {
      final Element jar = (Element) o;
      final String path = jar.getText();
      final String classname;
      if (path.endsWith(".class")) {
        classname = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf('.'));
      } else {
        //may be illegal report
        myLogger.error("Illegal report");
        return;
      }

      myFilePaths.put(classname, path);
    }
  }

  private void processBugInstances(Element root) {
    final List bugs = root.getChildren("BugInstance");
    for (Object o : bugs) {
      final Element bug = (Element) o;
      myInspectionInstance = new InspectionInstance();

      final String id = bug.getAttributeValue("type");
      if (id == null) {
        //illegal report
        myLogger.error("Illegal report");
        return;
      }
      myInspectionInstance.setInspectionId(id);

      if (bug.getChild("ShortMessage") != null) {
        myInspectionInstance.setMessage(bug.getChild("ShortMessage").getText());
      } else if (bug.getChild("LongMessage") != null) {
        myInspectionInstance.setMessage(bug.getChild("LongMessage").getText());
      } else {
        myInspectionInstance.setMessage("<no description>");    //TODO take data from other tags
      }

      final Element classElement = bug.getChild("Class");
      if (classElement == null) {
        //may be illegal report
        myLogger.error("Illegal report");
        return;
      }
      final String classname = classElement.getAttributeValue("classname");
      if (classname == null) {
        //illegal report
        myLogger.error("Illegal report");
        return;
      }
      String path = myFilePaths.get(classname);
      if (path == null) {
        //may be illegal report
        myLogger.error("Illegal report");
        return;
      }
      myInspectionInstance.setFilePath(path.replace(".class", ".java").replace("\\", "|").replace("/", "|"));

      final List lines = bug.getChildren("SourceLine");
      Element line;
      if (lines.size() != 0) {
        line = (Element) lines.get(0);
        getLocationDetails(line);
      } else {
        line = classElement.getChild("SourceLine");
        if (line == null) {
          //illegal report
          myLogger.error("Illegal report");
          return;
        }
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
        } else {
          //illegal report          
          myLogger.error("Illegal report");
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
// sourcepath attribute deprecated
//    if (line.getAttributeValue("sourcepath") != null) {
//      myInspectionInstance.setFilePath(line.getAttributeValue("sourcepath"));
//    } else {
//      myInspectionInstance.setFilePath("<file path not available>");
//    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    for (Object o : categories) {
      final Element category = (Element) o;

      final String id = category.getAttributeValue("category");
      final Element description = category.getChild("Description");
      if ((id == null) || FindBugsCategories.isCommonCategory(id) || myCategories.containsKey(id) || (description == null)) {
        //illegal report
        myLogger.error("Illegal report");
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
        || (shortDescription == null) || (details == null) || (category == null) || myReportedInstanceTypes.contains(id)) {
        //illegal report
        myLogger.error("Illegal report");
        return;
      }

      final InspectionTypeInfo type = new InspectionTypeInfo();

      type.setId(id);
      type.setName(shortDescription.getText());
      type.setDescription(details.getText());

      if (FindBugsCategories.isCommonCategory(category)) {
        category = FindBugsCategories.getName(category);
      } else if (myCategories.containsKey(category)) {
        category = myCategories.get(category);
      } else {
        //illegal report
        myLogger.error("Illegal report");
        return;
      }

      type.setCategory(category);

      myInspectionReporter.reportInspectionType(type);
      myReportedInstanceTypes.add(id);
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