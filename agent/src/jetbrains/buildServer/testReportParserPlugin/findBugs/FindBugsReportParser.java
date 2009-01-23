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

import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;


public class FindBugsReportParser implements TestReportParser {
  //TODO: add optional attributes and elements logging
  public static final String TYPE = "findBugs";

  private final TestReportLogger myLogger;

  public FindBugsReportParser(@NotNull final TestReportLogger logger) {
    myLogger = logger;
  }

  public void parse(@NotNull File report) {
    myLogger.message("Start processing FindBugs report");
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
  }

  private void processRootAttributes(Element root) {
    myLogger.message("Version: " + root.getAttributeValue("version"));
    myLogger.message("Sequence: " + root.getAttributeValue("sequence"));
    myLogger.message("Timestamp: " + root.getAttributeValue("timestamp"));
    myLogger.message("Analysis timestamp: " + root.getAttributeValue("analysisTimestamp"));
    myLogger.message("Release: " + root.getAttributeValue("release"));
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
      myLogger.message("[Clases analized from jars]");
      for (Object o : jars) {
        final Element jar = (Element) o;
        myLogger.message(jar.getText());
      }
    }
  }

  private void processAuxCPs(Element project) {
    final List auxClassPaths = project.getChildren("AuxClasspathEntry");
    if (auxClassPaths.size() > 0) {
      myLogger.message("[Auxiliary class paths]");
      for (Object o : auxClassPaths) {
        final Element path = (Element) o;
        myLogger.message(path.getText());
      }
    }
  }

  private void processSrcPaths(Element project) {
    final List srcPaths = project.getChildren("SrcDir");
    if (srcPaths.size() > 0) {
      myLogger.message("[Source paths]");
      for (Object o : srcPaths) {
        final Element path = (Element) o;
        myLogger.message(path.getText());
      }
    }
  }

  private void processBugInstances(Element root) {
    final List bugs = root.getChildren("BugInstance");
    if (bugs.size() > 0) {
      myLogger.message("[Bug instances]");
      for (Object o : bugs) {
        final Element bug = (Element) o;

        processBugDetails(bug);
        myLogger.message("Type: " + bug.getAttributeValue("type"));
        myLogger.message("Priority: " + bug.getAttributeValue("priority"));
        myLogger.message("Abbreviation: " + bug.getAttributeValue("abbrev"));
        myLogger.message("Category: " + bug.getAttributeValue("category"));

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

        myLogger.message("[Property]");
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
    }
  }

  private void processString(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[String]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
        processMessages(e);
      }
    }
  }

  private void processInt(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Int]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
        processMessages(e);
      }
    }
  }

  private void processField(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Field]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processLocalVariable(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[LocalVariable]");
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Register: " + e.getAttributeValue("register"));
        myLogger.message("Pc: " + e.getAttributeValue("pc"));
        myLogger.message("Role: " + e.getAttributeValue("role"));
        processMessages(e);
      }
    }
  }

  private void processMethod(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Method]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processType(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Descriptor] " + e.getAttributeValue("descriptor"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processClasses(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Class] " + e.getAttributeValue("classname"));
        processSourceLines(e);
        processMessages(e);
      }
    }
  }

  private void processUserAnnotation(Element bug) {
    final Element annotation = bug.getChild("UserAnnotation");
    if (annotation != null) {
//  optional attrs
//      myLogger.message("Designation: " + annotation.getAttributeValue("designation"));
//      myLogger.message("User: " + annotation.getAttributeValue("user"));
//      myLogger.message("Timestamp: " + annotation.getAttributeValue("timestamp"));
      myLogger.message("Annotation: " + annotation.getText());
    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    if (categories.size() > 0) {
      myLogger.message("[Bug categories]");
      for (Object o : categories) {
        final Element category = (Element) o;

        myLogger.message("[Category] " + category.getAttributeValue("category"));
        myLogger.message("Description: " + category.getChild("Description").getText());

        logTextIfNotNull("Abbreviation: ", category.getChild("Abbreviation"));
        logTextIfNotNull("Details: ", category.getChild("Details"));
      }
    }
  }

  private void processBugPatterns(Element root) {
    final List patterns = root.getChildren("BugPattern");
    if (patterns.size() > 0) {
      myLogger.message("[Bug patterns]");
      for (Object o : patterns) {
        final Element pattern = (Element) o;

        myLogger.message("[Bug pattern]");
        myLogger.message("Type: " + pattern.getAttributeValue("type"));
        myLogger.message("Abbreviation: " + pattern.getAttributeValue("abbrev"));
        myLogger.message("Category: " + pattern.getAttributeValue("category"));
        myLogger.message("Description: " + pattern.getChild("ShortDescription").getText());
        myLogger.message("Details: " + pattern.getChild("Details").getText());
      }
    }
  }

  private void processBugCodeElements(Element root) {
    final List bugCodeElements = root.getChildren("BugCode");
    if (bugCodeElements.size() > 0) {
      myLogger.message("[Bug code]");
      int i = 0;
      for (Object o : bugCodeElements) {
        ++i;
        final Element bugCodeElement = (Element) o;

        myLogger.message("[" + i + "] ");
        myLogger.message("Abbreviation: " + bugCodeElement.getAttributeValue("abbrev"));
        myLogger.message("Description: " + bugCodeElement.getChild("ShortDescription").getText());
      }
    }
  }

  private void processErrors(Element root) {
    final List errors = root.getChild("Errors").getChildren("MissingClass");
    if (errors.size() > 0) {
      myLogger.message("[Errors]");
      for (Object o : errors) {
        final Element error = (Element) o;

        myLogger.message(error.getText());
      }
    }
  }

  private void processFindBugsSummary(Element root) {
    final Element summary = root.getChild("FindBugsSummary");

    myLogger.message("[Summary]");
    myLogger.message("Total classes: " + summary.getAttributeValue("total_classes"));
    myLogger.message("Total bugs: " + summary.getAttributeValue("total_bugs"));
    myLogger.message("Total size: " + summary.getAttributeValue("total_size"));
    myLogger.message("Packages number: " + summary.getAttributeValue("num_packages"));
    myLogger.message("Timestamp: " + summary.getAttributeValue("timestamp"));
    processFileStatusElements(summary);
    processPackageStatusElements(summary);
  }

  private void processFileStatusElements(Element element) {
    final List fileStatusElements = element.getChildren("FileStatus");
    if (fileStatusElements.size() > 0) {
      myLogger.message("[File statuses]");
      for (Object o : fileStatusElements) {
        final Element fileStatus = (Element) o;

        myLogger.message("Path: " + fileStatus.getAttributeValue("path"));
        myLogger.message("Bug count: " + fileStatus.getAttributeValue("bugCount"));
      }
    }
  }

  private void processPackageStatusElements(Element element) {
    final List packageStatusElements = element.getChildren("FileStatus");
    if (packageStatusElements.size() > 0) {
      myLogger.message("[Package statuses]");
      for (Object o : packageStatusElements) {
        final Element packageStatus = (Element) o;

        myLogger.message("Package: " + packageStatus.getAttributeValue("package"));
        myLogger.message("Total bugs: " + packageStatus.getAttributeValue("total_bugs"));
        myLogger.message("Total types: " + packageStatus.getAttributeValue("total_types"));
        myLogger.message("Total size: " + packageStatus.getAttributeValue("total_size"));

        processClassStatusElements(packageStatus);
      }
    }
  }

  private void processClassStatusElements(Element element) {
    final List classStatusElements = element.getChildren("ClassStats");
    myLogger.message("[Class statuses]");
    for (Object o : classStatusElements) {
      final Element classStatus = (Element) o;

      myLogger.message("Class: " + classStatus.getAttributeValue("class"));
      myLogger.message("Interface: " + classStatus.getAttributeValue("interface"));
      myLogger.message("Size: " + classStatus.getAttributeValue("size"));
      myLogger.message("Bugs: " + classStatus.getAttributeValue("bugs"));
    }
  }

//  <xs:element name="SummaryHTML" type="xs:string" minOccurs="0"/>  

  private void processClassFeaturesElement(Element element) {
    final List featureSets = element.getChild("ClassFeatures").getChildren("ClassFeatureSet");
    for (Object o1 : featureSets) {
      final Element featureSet = (Element) o1;
      final List features = featureSet.getChildren("Feature");
      if (features.size() > 0) {
        myLogger.message("[Class features] " + featureSet.getAttributeValue("class"));
        for (Object o2 : features) {
          final Element feature = (Element) o2;
          myLogger.message(feature.getAttributeValue("value"));
        }
      }
    }
  }

  private void processHistory(Element root) {
    final List versions = root.getChild("History").getChildren("AppVersion");
    if (versions.size() > 0) {
      myLogger.message("[History]");
      for (Object o : versions) {
        final Element version = (Element) o;

        myLogger.message("Squence: " + version.getAttributeValue("sequence"));
        myLogger.message("Timestamp: " + version.getAttributeValue("timestamp"));
        myLogger.message("Release: " + version.getAttributeValue("release"));
        myLogger.message("Code size: " + version.getAttributeValue("codeSize"));
        myLogger.message("Class number: " + version.getAttributeValue("numClasses"));
      }
    }
  }

  private void processSourceLines(Element root) {
    List lines = root.getChildren("SourceLine");

    for (Object o : lines) {
      Element line = (Element) o;
      myLogger.message("Class : " + line.getAttributeValue("classname"));
      processMessages(line);
    }
  }

  private void processMessages(Element root) {
    List messages = root.getChildren("Message");

    for (Object o : messages) {
      Element message = (Element) o;
      myLogger.message(message.getText());
    }
  }

  private void logTextIfNotNull(String message, Element e) {
    if (e != null) {
      myLogger.message(message + e.getText());
    }
  }

  private void logAttrIfNotNull(String message, String m) {
    if (m != null) {
      myLogger.message(message + m);
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
