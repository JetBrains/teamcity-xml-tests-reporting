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

import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Map;


public class FindBugsReportParserText implements TestReportParser {
  //TODO: may be add optional attributes and elements logging
  public static final String TYPE = "findBugs";

  private static final boolean DEBUG_MODE = true;

  private final TestReportLogger myLogger;
  private final InspectionReporter myInspectionReporter;

  public FindBugsReportParserText(@NotNull final TestReportLogger logger, @NotNull InspectionReporter inspectionReporter) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
  }

  private void message(String message) {
    if (DEBUG_MODE) {
      myLogger.message(message);
    }
  }

  public void parse(@NotNull File report) {
    message("Start processing FindBugs report");
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      final Element root = new SAXBuilder().build(report).getRootElement();

      processRootAttributes(root);

      processProject(root);

      processBugInstances(root);

      processBugCategories(root);

      processBugPatterns(root);

      processBugCodeElements(root);

      processErrors(root);

      processFindBugsSummary(root);

//      <xs:element name="SummaryHTML" type="xs:string" minOccurs="0"/>

      processClassFeaturesElement(root);

      processHistory(root);

//    } catch (JDOMException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
    } catch (Exception e) {
//      throw new RuntimeException(e);
      myLogger.exception(e);
    }
    myInspectionReporter.flush();
  }

  private void processRootAttributes(Element root) {
    message("Version: " + root.getAttributeValue("version"));
    message("Sequence: " + root.getAttributeValue("sequence"));
    message("Timestamp: " + root.getAttributeValue("timestamp"));
    message("Analysis timestamp: " + root.getAttributeValue("analysisTimestamp"));
    message("Release: " + root.getAttributeValue("release"));
  }

  private void processProject(Element root) {
    Element project = root.getChild("Project");

    String attrValue;

    attrValue = project.getAttributeValue("projectName");
    logAttrIfNotNull("[Project name] ", attrValue);

    attrValue = project.getAttributeValue("filename");
    logAttrIfNotNull("[File name] ", attrValue);

    processJars(project);
    processAuxCPs(project);
    processSrcPaths(project);
  }

  private void processJars(Element project) {
    final List jars = project.getChildren("Jar");
    if (jars.size() > 0) {
      message("[Clases analized from jars]");
      for (Object o : jars) {
        final Element jar = (Element) o;
        message(jar.getText());
      }
    }
  }

  private void processAuxCPs(Element project) {
    final List auxClassPaths = project.getChildren("AuxClasspathEntry");
    if (auxClassPaths.size() > 0) {
      message("[Auxiliary class paths]");
      for (Object o : auxClassPaths) {
        final Element path = (Element) o;
        message(path.getText());
      }
    }
  }

  private void processSrcPaths(Element project) {
    final List srcPaths = project.getChildren("SrcDir");
    if (srcPaths.size() > 0) {
      message("[Source paths]");
      for (Object o : srcPaths) {
        final Element path = (Element) o;
        message(path.getText());
      }
    }
  }

  private void processBugInstances(Element root) {
    final List bugs = root.getChildren("BugInstance");
    if (bugs.size() > 0) {
      message("[Bug instances]");
      for (Object o : bugs) {
        final Element bug = (Element) o;
        processBugDetails(bug);
        message("Type: " + bug.getAttributeValue("type"));
        message("Priority: " + bug.getAttributeValue("priority"));
        message("Abbreviation: " + bug.getAttributeValue("abbrev"));
        message("Category: " + bug.getAttributeValue("category"));

        logTextIfNotNull("Short message: ", bug.getChild("ShortMessage"));
        logTextIfNotNull("Long message: ", bug.getChild("LongMessage"));
        processUserAnnotation(bug);
      }
    }
  }

  private void processBugDetails(Element bug) {
    processClasses(bug.getChildren("Class"));
    processType(bug.getChildren("Type"));
    processMethod(bug.getChildren("Method"));
    processSourceLines(bug);
    processLocalVariable(bug.getChildren("LocalVariable"));
    processField(bug.getChildren("Field"));
    processInt(bug.getChildren("Int"));
    processString(bug.getChildren("String"));
    processProperty(bug.getChildren("Property"));
    processSourceLines(bug);
  }

  private void processProperty(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Property]");
        message("Name: " + e.getAttributeValue("name"));
        message("Value: " + e.getAttributeValue("value"));
      }
    }
  }

  private void processString(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[String]");
        message("Value: " + e.getAttributeValue("value"));
        processMessages(e);
      }
    }
  }

  private void processInt(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Int]");
        message("Value: " + e.getAttributeValue("value"));
        processMessages(e);
      }
    }
  }

  private void processField(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Field]");
        message("Classname: " + e.getAttributeValue("classname"));
        message("Name: " + e.getAttributeValue("name"));
        message("Signature: " + e.getAttributeValue("signature"));
        message("Is static: " + e.getAttributeValue("isStatic"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processLocalVariable(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[LocalVariable]");
        message("Name: " + e.getAttributeValue("name"));
        message("Register: " + e.getAttributeValue("register"));
        message("Pc: " + e.getAttributeValue("pc"));
        message("Role: " + e.getAttributeValue("role"));
        processMessages(e);
      }
    }
  }

  private void processMethod(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Method]");
        message("Classname: " + e.getAttributeValue("classname"));
        message("Name: " + e.getAttributeValue("name"));
        message("Signature: " + e.getAttributeValue("signature"));
        message("Is static: " + e.getAttributeValue("isStatic"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processType(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Descriptor] " + e.getAttributeValue("descriptor"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processClasses(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        message("[Class] " + e.getAttributeValue("classname"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processUserAnnotation(Element bug) {
    final Element annotation = bug.getChild("UserAnnotation");
    if (annotation != null) {
//  optional attrs
//      message("Designation: " + annotation.getAttributeValue("designation"));
//      message("User: " + annotation.getAttributeValue("user"));
//      message("Timestamp: " + annotation.getAttributeValue("timestamp"));
      message("Annotation: " + annotation.getText());
    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    if (categories.size() > 0) {
      message("[Bug categories]");
      for (Object o : categories) {
        final Element category = (Element) o;

        message("[Category] " + category.getAttributeValue("category"));
        message("Description: " + category.getChild("Description").getText());

        logTextIfNotNull("Abbreviation: ", category.getChild("Abbreviation"));
        logTextIfNotNull("Details: ", category.getChild("Details"));
      }
    }
  }

  private void processBugPatterns(Element root) {
    final List patterns = root.getChildren("BugPattern");
    if (patterns.size() > 0) {
      message("[Bug patterns]");
      for (Object o : patterns) {
        final Element pattern = (Element) o;

        message("[Bug pattern]");
        message("Type: " + pattern.getAttributeValue("type"));
        message("Abbreviation: " + pattern.getAttributeValue("abbrev"));
        message("Category: " + pattern.getAttributeValue("category"));
        message("Description: " + pattern.getChild("ShortDescription").getText());
        message("Details: " + pattern.getChild("Details").getText());
      }
    }
  }

  private void processBugCodeElements(Element root) {
    final List bugCodeElements = root.getChildren("BugCode");
    if (bugCodeElements.size() > 0) {
      message("[Bug code]");
      int i = 0;
      for (Object o : bugCodeElements) {
        ++i;
        final Element bugCodeElement = (Element) o;

        message("[" + i + "] ");
        message("Abbreviation: " + bugCodeElement.getAttributeValue("abbrev"));
        message("Description: " + bugCodeElement.getChild("ShortDescription").getText());
      }
    }
  }

  private void processErrors(Element root) {
    final List errors = root.getChild("Errors").getChildren("MissingClass");
    if (errors.size() > 0) {
      message("[Errors]");
      for (Object o : errors) {
        final Element error = (Element) o;

        message(error.getText());
      }
    }
  }

  private void processFindBugsSummary(Element root) {
    final Element summary = root.getChild("FindBugsSummary");

    message("[Summary]");
    message("Total classes: " + summary.getAttributeValue("total_classes"));
    message("Total bugs: " + summary.getAttributeValue("total_bugs"));
    message("Total size: " + summary.getAttributeValue("total_size"));
    message("Packages number: " + summary.getAttributeValue("num_packages"));
    message("Timestamp: " + summary.getAttributeValue("timestamp"));
    processFileStatusElements(summary);
    processPackageStatusElements(summary);
  }

  private void processFileStatusElements(Element element) {
    final List fileStatusElements = element.getChildren("FileStatus");
    if (fileStatusElements.size() > 0) {
      message("[File statuses]");
      for (Object o : fileStatusElements) {
        final Element fileStatus = (Element) o;

        message("Path: " + fileStatus.getAttributeValue("path"));
        message("Bug count: " + fileStatus.getAttributeValue("bugCount"));
      }
    }
  }

  private void processPackageStatusElements(Element element) {
    final List packageStatusElements = element.getChildren("FileStatus");
    if (packageStatusElements.size() > 0) {
      message("[Package statuses]");
      for (Object o : packageStatusElements) {
        final Element packageStatus = (Element) o;

        message("Package: " + packageStatus.getAttributeValue("package"));
        message("Total bugs: " + packageStatus.getAttributeValue("total_bugs"));
        message("Total types: " + packageStatus.getAttributeValue("total_types"));
        message("Total size: " + packageStatus.getAttributeValue("total_size"));

        processClassStatusElements(packageStatus);
      }
    }
  }

  private void processClassStatusElements(Element element) {
    final List classStatusElements = element.getChildren("ClassStats");
    message("[Class statuses]");
    for (Object o : classStatusElements) {
      final Element classStatus = (Element) o;

      message("Class: " + classStatus.getAttributeValue("class"));
      message("Interface: " + classStatus.getAttributeValue("interface"));
      message("Size: " + classStatus.getAttributeValue("size"));
      message("Bugs: " + classStatus.getAttributeValue("bugs"));
    }
  }

//  <xs:element name="SummaryHTML" type="xs:string" minOccurs="0"/>

  private void processClassFeaturesElement(Element element) {
    final List featureSets = element.getChild("ClassFeatures").getChildren("ClassFeatureSet");
    for (Object o1 : featureSets) {
      final Element featureSet = (Element) o1;
      final List features = featureSet.getChildren("Feature");
      if (features.size() > 0) {
        message("[Class features] " + featureSet.getAttributeValue("class"));
        for (Object o2 : features) {
          final Element feature = (Element) o2;
          message(feature.getAttributeValue("value"));
        }
      }
    }
  }

  private void processHistory(Element root) {
    final List versions = root.getChild("History").getChildren("AppVersion");
    if (versions.size() > 0) {
      message("[History]");
      for (Object o : versions) {
        final Element version = (Element) o;

        message("Squence: " + version.getAttributeValue("sequence"));
        message("Timestamp: " + version.getAttributeValue("timestamp"));
        message("Release: " + version.getAttributeValue("release"));
        message("Code size: " + version.getAttributeValue("codeSize"));
        message("Class number: " + version.getAttributeValue("numClasses"));
      }
    }
  }

  private void processSourceLines(Element root) {
    List lines = root.getChildren("SourceLine");

    for (Object o : lines) {
      Element line = (Element) o;
      message("Class : " + line.getAttributeValue("classname"));
      processMessages(line);
    }
  }

  private void processMessages(Element root) {
    List messages = root.getChildren("Message");

    for (Object o : messages) {
      Element message = (Element) o;
      message(message.getText());
    }
  }

  private void logTextIfNotNull(String message, Element e) {
    if (e != null) {
      message(message + e.getText());
    }
  }

  private void logAttrIfNotNull(String message, String m) {
    if (m != null) {
      message(message + m);
    }
  }

  public long parse(@NotNull File report, long testsToSkip) {
    parse(report);
    return -1;
  }

  public boolean abnormalEnd() {
    return false;
  }

  public void logReportTotals(File report, Map<String, String> params) {
  }
}