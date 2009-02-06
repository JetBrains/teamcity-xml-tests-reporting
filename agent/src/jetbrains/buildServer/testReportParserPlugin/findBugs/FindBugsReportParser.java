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

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionTypeInfo;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;


public class FindBugsReportParser implements TestReportParser {
  public static final String TYPE = "findBugs";

  private final SimpleBuildLogger myLogger;
  private final InspectionReporter myInspectionReporter;
  private final String myCheckoutDirectory;

  private final Set<String> myReportedInstanceTypes;
  private final Map<String, String> myCategories;
  private final Map<String, String> myBugPatterns;

  private Set<String> mySrcDirs;

  private InspectionInstance myInspectionInstance = null;
  private int mySourceLine = 0;
  private String myFilePath;
  private String myCurrentClass;

  public static String formatText(@NotNull String s) {
    return s.replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
  }

  public FindBugsReportParser(@NotNull final SimpleBuildLogger logger,
                              @NotNull InspectionReporter inspectionReporter,
                              @NotNull String checkoutDirectory) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory;

    myReportedInstanceTypes = new HashSet<String>();
    myCategories = new HashMap<String, String>();
    myBugPatterns = new HashMap<String, String>();
    mySrcDirs = new HashSet<String>();
  }

  public void parse(@NotNull File report) {
    mySourceLine = 0;
    myFilePath = null;
    myCurrentClass = null;
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      FindBugsCategories.loadCategories(myLogger, this.getClass().getResourceAsStream("categories.xml"));
      FindBugsPatterns.loadPatterns(myLogger, this.getClass().getResourceAsStream("patterns.xml"));

      final Element root = new SAXBuilder().build(report).getRootElement();

      processSrcDirs(root);
      processBugCategories(root);
      processBugPatterns(root);
      processBugInstances(root);

    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      myInspectionReporter.flush();
    }
  }

//  private void processJars(Element root) {
//    final Element project = root.getChild("Project");
//    if (project == null) {
//      //illegal report
//      myLogger.error("Illegal report");
//      return;
//    }
//    final List jars = project.getChildren("Jar");
//    for (Object o : jars) {
//      final Element jar = (Element) o;
//      final String path = formatText(jar.getText());
//      final String classname;
//      if (path.endsWith(".class")) {
//        classname = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf('.'));
//      } else {
//        //may be illegal report
//        myLogger.error("Illegal report");
//        continue;
//      }
//
//      myFilePaths.put(classname, path);
//    }
//  }

  private void processSrcDirs(Element root) {
    final Element project = root.getChild("Project");
    if (project == null) {
      //illegal report
      myLogger.error("Illegal report");
      return;
    }
    final List srcDirs = project.getChildren("SrcDir");
    for (Object o : srcDirs) {
      final Element dir = (Element) o;

      mySrcDirs.add(formatText(dir.getText()));
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

      getBugMessage(bug, id);
//      processSourceLine(bug);

      if (!processSourceLine(bug)) {
        final Element classElement = bug.getChild("Class");

        if (classElement != null) {
          if (!processSourceLine(classElement)) {
            final String classname = classElement.getAttributeValue("classname");
            if (classname != null) {
              myInspectionInstance.setFilePath(classname.replace(".class", ".java"));
            } else {
              //illegal report
              myLogger.error("Illegal report");
              return;
            }
          }
        } else {
          //TODO what to do? (may be try method element)
          myLogger.exception(new Exception("Do not know what to do!"));
        }
      }
//      myInspectionInstance.setLine(mySourceLine);
//      myInspectionInstance.setFilePath(myFilePath);
      myInspectionReporter.reportInspection(myInspectionInstance);
      myInspectionInstance = null;

      reportInstanceType(id);
    }
  }

//  private void processSourceLine(Element bug) {
//    final Element sourceLine = bug.getChild("SourceLine");
//    if (sourceLine != null) {
//      final String line = sourceLine.getAttributeValue("start");
//      if (line != null) {
//        myInspectionInstance.setLine(Integer.parseInt(line));
//      }
//
//      String path = sourceLine.getAttributeValue("sourcepath");
//      if (path != null) {
//        path = getPath(path);
//        if (path.startsWith(myCheckoutDirectory)) {
//          path = path.substring(myCheckoutDirectory.length());
//        }
//        path = path.replace("\\", "|").replace("/", "|");
//        if (path.startsWith("|")) {
//          path = path.substring(1);
//        }
//        myFilePath = path;
//      }
//    }
//  }

  private void reportInstanceType(String id) {
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

  private void getBugMessage(Element bug, String id) {
    String message = "<No message>";
    if (bug.getChild("ShortMessage") != null) {
      message = formatText(bug.getChild("ShortMessage").getText());
    } else if (bug.getChild("LongMessage") != null) {
      message = formatText(bug.getChild("LongMessage").getText());
    } else if (FindBugsPatterns.isCommonPattern(id)) {
      message = FindBugsPatterns.getDescription(id);
    } else if (myBugPatterns.containsKey(id)) {
      message = myBugPatterns.get(id);
    } else {
      //illegal report
      myLogger.error("Illegal report");
    }
    myInspectionInstance.setMessage(message);
  }

  private boolean processSourceLine(Element e) {
    final Element sourceLine = e.getChild("SourceLine");
    if (sourceLine != null) {
      final String line = sourceLine.getAttributeValue("start");
      if ((mySourceLine == 0) && (line != null)) {
        myInspectionInstance.setLine(Integer.parseInt(line));
      } else {
        myInspectionInstance.setLine(0);
      }

      String path = sourceLine.getAttributeValue("sourcepath");
      if (path != null) {
        path = getPath(path);
        if (path.startsWith(myCheckoutDirectory)) {
          path = path.substring(myCheckoutDirectory.length());
        }
        path = path.replace("\\", "|").replace("/", "|");
        if (path.startsWith("|")) {
          path = path.substring(1);
        }
        myInspectionInstance.setFilePath(path);
        return true;
      }
    }
    return false;
  }

  private String getPath(String relativePath) {
    for (String s : mySrcDirs) {
      System.out.println(s);
      final File f = new File(s + File.separator + relativePath);
      if (f.exists()) {
        System.out.println("FOUND SRC");
        return f.getPath();
      }
    }
    System.out.println("COULD NOT FIND SRC");
    return "";
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
      myCategories.put(id, formatText(description.getText()));
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
      type.setName(formatText(shortDescription.getText()));
      type.setDescription(formatText(details.getText()));

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
      myBugPatterns.put(id, formatText(details.getText()));
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