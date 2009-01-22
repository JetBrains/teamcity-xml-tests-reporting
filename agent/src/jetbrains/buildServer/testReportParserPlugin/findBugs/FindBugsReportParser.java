package jetbrains.buildServer.testReportParserPlugin.findBugs;

import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;


public class FindBugsReportParser implements TestReportParser {
  public static final String TYPE = "findBugs";

  private final TestReportLogger myLogger;

  public FindBugsReportParser(@NotNull final TestReportLogger logger) {
    myLogger = logger;
  }

  public void parse(@NotNull File report) {
    myLogger.message("");
    myLogger.message("Start processing FindBugs report");
    try {
      final Element root = new SAXBuilder().build(report).getRootElement();

      processProjectElement(root.getChild("Project"));

      processBugInstances(root);

      processBugCategories(root);

      processBugPatterns(root);

      processBugCodeElements(root);

      processErrors(root);

      processFindBugsSummary(root);

      processClassFeaturesElement(root);

      processHistory(root);

//    } catch (JDOMException e) {
//      e.printStackTrace();
//    } catch (IOException e) {
//      e.printStackTrace();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void processProjectElement(Element project) {
    String attrValue;

    attrValue = project.getAttributeValue("projectName");
    logMessageIfNotNull("[Project name] " + attrValue, attrValue);

    attrValue = project.getAttributeValue("filename");
    logMessageIfNotNull("[File name] " + attrValue, attrValue);

    processJars(project);
    processAuxCPs(project);
    processSrcPaths(project);
  }

  private void processJars(Element project) {
    final List jars = project.getChildren("Jar");
    if (jars.size() > 0) {
      myLogger.message("");
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
      myLogger.message("");
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
      myLogger.message("");
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
      myLogger.message("");
      myLogger.message("[Bug instances]");
//      <xs:element name="ShortMessage" type="xs:string" minOccurs="0"/>
//      <xs:element name="LongMessage" type="xs:string" minOccurs="0"/>
      for (Object o : bugs) {
        final Element bug = (Element) o;

        processBugChoice(bug);
        myLogger.message("Type: " + bug.getAttributeValue("type"));
        myLogger.message("Priority: " + bug.getAttributeValue("priority"));
        myLogger.message("Abbreviation: " + bug.getAttributeValue("abbrev"));
        myLogger.message("Category: " + bug.getAttributeValue("category"));

//                     <xs:element name="UserAnnotation" minOccurs="0">
//                <xs:complexType>
//                  <xs:simpleContent>
//                    <xs:extension base="xs:string">
//                      <xs:attribute name="designation" type="designationType" use="optional"/>
//                      <xs:attribute name="user" type="xs:string" use="optional"/>
//                      <xs:attribute name="timestamp" type="xs:unsignedLong" use="optional"/>
//                    </xs:extension>
//                  </xs:simpleContent>
//                </xs:complexType>
//              </xs:element>
        //TODO: add optional attributes and elements logging
      }
    }
  }

  private void processBugChoice(Element root) {
    processClass(root.getChildren("Class"));
    processType(root.getChildren("Type"));
    processMethod(root.getChildren("Method"));
//    choice = root.getChildren("SourceLine");
//    if (choice.size() != 0) {
//      for (Object o: choice) {
//        Element e = (Element) o;
//
//        myLogger.message("[SourceLine]");
//      }
//      return;
//    }
    processLocalVariable(root.getChildren("LocalVariable"));
    processField(root.getChildren("Field"));
    processInt(root.getChildren("Int"));
    processString(root.getChildren("String"));
    processProperty(root.getChildren("Property"));
  }

  private void processProperty(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
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

        myLogger.message("");
        myLogger.message("[String]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
    }
  }

  private void processInt(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[Int]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
    }
  }

  private void processField(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[Field]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
      }
    }
  }

  private void processLocalVariable(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[LocalVariable]");
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Register: " + e.getAttributeValue("register"));
        myLogger.message("Pc: " + e.getAttributeValue("pc"));
        myLogger.message("Role: " + e.getAttributeValue("role"));
      }
    }
  }

  private void processMethod(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[Method]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
      }
    }
  }

  private void processType(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[Type] " + e.getAttributeValue("descriptor"));
      }
    }
  }

  private void processClass(List choice) {
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("");
        myLogger.message("[Class] " + e.getAttributeValue("classname"));
      }
    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    if (categories.size() > 0) {
      myLogger.message("");
      myLogger.message("[Bug categories]");
      for (Object o : categories) {
        final Element category = (Element) o;

        myLogger.message("[Category] " + category.getAttributeValue("category"));
        myLogger.message("Description: " + category.getChild("Description").getText());

        Element optional;
        optional = category.getChild("Abbreviation");
        logMessageIfNotNull("Abbreviation: ", optional);
        optional = category.getChild("Details");
        logMessageIfNotNull("Details: ", optional);
      }
    }
  }

  private void processBugPatterns(Element root) {
    final List patterns = root.getChildren("BugPattern");
    if (patterns.size() > 0) {
      myLogger.message("");
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
      myLogger.message("");
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

    myLogger.message("");
    myLogger.message("[Summary]");
    myLogger.message("Total classes: " + summary.getAttributeValue("total_classes"));
    myLogger.message("Total bugs: " + summary.getAttributeValue("total_bugs"));
    myLogger.message("Total size: " + summary.getAttributeValue("total_size"));
    myLogger.message("Packages number: " + summary.getAttributeValue("num_packages"));
    processFileStatusElements(summary);
    processPackageStatusElements(summary);
  }

  private void processFileStatusElements(Element element) {
    final List fileStatusElements = element.getChildren("FileStatus");
    myLogger.message("");
    myLogger.message("[File statuses]");
    for (Object o : fileStatusElements) {
      final Element fileStatus = (Element) o;

      myLogger.message("");
      myLogger.message("Path: " + fileStatus.getAttributeValue("path"));
      myLogger.message("Bug count: " + fileStatus.getAttributeValue("bugCount"));
    }
  }

  private void processPackageStatusElements(Element element) {
    final List packageStatusElements = element.getChildren("FileStatus");
    myLogger.message("");
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

  private void processSourceLine(Element root) {
    Element line = root.getChild("SourceLine");

    if (line != null) {
      myLogger.message("Class : " + line.getAttributeValue("classname"));
      processMessage(line);
    }
  }

  private void processMessage(Element root) {
    Element message = root.getChild("Message");

    if (message != null) {
      myLogger.message(message.getText());
    }
  }

  private void logMessageIfNotNull(String message, Object o) {
    if (o != null) {
      myLogger.message(message);
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
