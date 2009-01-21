package jetbrains.buildServer.testReportParserPlugin.findBugs;

import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class FindBugsReportParser {

  private final TestReportLogger myLogger;

  public FindBugsReportParser(@NotNull final TestReportLogger logger) {
    myLogger = logger;
  }

  public void parse(@NotNull File report) {
    myLogger.message("Start prcessing FindBugs report");
    try {
      final Element root = new SAXBuilder().build(report).getRootElement();

      processProjectElement(root.getChild("Project"));

      processbugInstanceElements(root);

      processBugCategories(root);

      processBugPatterns(root);

      processBugCodeElements(root);

      processErrors(root);

    } catch (JDOMException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void processBugCategories(Element root) {
    final List categories = root.getChildren("BugCategory");
    if (categories.size() > 0) {
      myLogger.message("[Bug categories]");
      int i = 0;
      for (Object o : categories) {
        ++i;
        final Element category = (Element) o;

        myLogger.message("[" + i + "] " + category.getAttributeValue("category"));
        myLogger.message("Description: " + category.getChild("Description").getText());

        Element optional;
        optional = category.getChild("Abbreviation");
        logMessageIfNotNull("Abbreviation: ", optional);
        optional = category.getChild("Details");
        logMessageIfNotNull("Details: ", optional);
      }
    }
  }

  private void processErrors(Element root) {
    final List errors = root.getChild("Errors").getChildren("MissingClass");
    if (errors.size() > 0) {
      myLogger.message("[Errors]");
      for (Object o : errors) {
        final Element category = (Element) o;

        myLogger.message(category.getText());
      }
    }
  }

  private void processBugPatterns(Element root) {
    final List patterns = root.getChildren("BugPattern");
    if (patterns.size() > 0) {
      myLogger.message("[Bug patterns]");
      int i = 0;
      for (Object o : patterns) {
        ++i;
        final Element pattern = (Element) o;

        myLogger.message("[" + i + "] ");
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
        myLogger.message("Description: " + bugCodeElement.getChild("ShortDescription").getText());
        myLogger.message("Abbreviation: " + bugCodeElement.getAttributeValue("abbrev"));
      }
    }
  }

  private void processbugInstanceElements(Element root) {
    final List bugs = root.getChildren("BugInstance");
    if (bugs.size() > 0) {
      myLogger.message("[Bug instances]");
      int i = 0;
      for (Object o : bugs) {
        ++i;
        final Element bug = (Element) o;

        myLogger.message("[" + i + "]");
        myLogger.message("Type: " + bug.getAttributeValue("type"));
        myLogger.message("Priority: " + bug.getAttributeValue("type"));
        myLogger.message("Abbreviation: " + bug.getAttributeValue("abbrev"));
        myLogger.message("Category: " + bug.getAttributeValue("category"));
        //TODO: add optional attributes and elements logging

        processBugChoice(bug);
      }
    }
  }

  private void processProjectElement(Element project) {
    String attrValue;
    attrValue = project.getAttributeValue("filename");
    logMessageIfNotNull("[File name] " + attrValue, attrValue);
    attrValue = project.getAttributeValue("projectName");
    logMessageIfNotNull("[Project name] " + attrValue, attrValue);

    final List jars = project.getChildren("Jar");
    if (jars.size() > 0) {
      myLogger.message("[Clases analized from jars]");
      for (Object o : jars) {
        final Element jar = (Element) o;
        myLogger.message(jar.getText());
      }
    }

    final List auxClassPaths = project.getChildren("AuxClasspathEntry");
    if (auxClassPaths.size() > 0) {
      myLogger.message("[Auxiliary class paths]");
      for (Object o : auxClassPaths) {
        final Element path = (Element) o;
        myLogger.message(path.getText());
      }
    }

    final List srcPaths = project.getChildren("SrcDir");
    if (srcPaths.size() > 0) {
      myLogger.message("[Source paths]");
      for (Object o : srcPaths) {
        final Element path = (Element) o;
        myLogger.message(path.getText());
      }
    }
  }

  private void processBugChoice(Element root) {
    List choice;
    choice = root.getChildren("Class");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Class] " + e.getAttributeValue("classname"));
      }
      return;
    }
    choice = root.getChildren("Type");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Type]" + e.getAttributeValue("descriptor"));
      }
      return;
    }
    choice = root.getChildren("Method");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Method]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
      }
      return;
    }
//    choice = root.getChildren("SourceLine");
//    if (choice.size() != 0) {
//      for (Object o: choice) {
//        Element e = (Element) o;
//
//        myLogger.message("[SourceLine]");
//      }
//      return;
//    }
    choice = root.getChildren("LocalVariable");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[LocalVariable]");
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Register: " + e.getAttributeValue("register"));
        myLogger.message("Pc: " + e.getAttributeValue("pc"));
        myLogger.message("Role: " + e.getAttributeValue("role"));
      }
      return;
    }
    choice = root.getChildren("Field");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Field]");
        myLogger.message("Classname: " + e.getAttributeValue("classname"));
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Signature: " + e.getAttributeValue("signature"));
        myLogger.message("Is static: " + e.getAttributeValue("isStatic"));
      }
      return;
    }
    choice = root.getChildren("Int");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Int]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
      return;
    }
    choice = root.getChildren("String");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[String]");
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
      return;
    }
    choice = root.getChildren("Property");
    if (choice.size() != 0) {
      for (Object o : choice) {
        Element e = (Element) o;

        myLogger.message("[Property]");
        myLogger.message("Name: " + e.getAttributeValue("name"));
        myLogger.message("Value: " + e.getAttributeValue("value"));
      }
      return;
    }
  }

  private void logMessageIfNotNull(String message, Object o) {
    if (o != null) {
      myLogger.message(message);
    }
  }
}
