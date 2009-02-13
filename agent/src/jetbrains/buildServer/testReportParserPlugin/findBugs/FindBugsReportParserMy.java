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
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FindBugsReportParserMy implements TestReportParser {
  public static final String TYPE = "findBugs";

  private final SimpleBuildLogger myLogger;
  private final InspectionReporter myInspectionReporter;
  private final String myCheckoutDirectory;

  private final Set<String> myReportedInstanceTypes;
  private final Map<String, String> myCategories;
  private final Map<String, String> myBugPatterns;

  private Set<String> mySrcDirs;
  private Set<String> myJars;

  public static String formatText(@NotNull String s) {
    return s.replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
  }

  public FindBugsReportParserMy(@NotNull final SimpleBuildLogger logger,
                                @NotNull InspectionReporter inspectionReporter,
                                @NotNull String checkoutDirectory) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory;

    myReportedInstanceTypes = new HashSet<String>();
    myCategories = new HashMap<String, String>();
    myBugPatterns = new HashMap<String, String>();
    mySrcDirs = new HashSet<String>();
    myJars = new HashSet<String>();
  }

  public void parse(@NotNull File report) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      FindBugsCategories.loadCategories(myLogger, this.getClass().getResourceAsStream("categories.xml"));
      FindBugsPatterns.loadPatterns(myLogger, this.getClass().getResourceAsStream("patterns.xml"));

      final Element root = new SAXBuilder().build(report).getRootElement();
      final Element project = root.getChild("Project");
      if (project == null) {
        //illegal report
        myLogger.error("Illegal report: no Project element specified");
        return;
      }
      processJars(project);
      processSrcDirs(project);
      processBugCategories(root);
      processBugPatterns(root);
      processBugInstances(root);

    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      myInspectionReporter.flush();
    }
  }

  private void processJars(Element project) {
    final List jars = project.getChildren("Jar");
    for (Object o : jars) {
      final Element jar = (Element) o;
      myJars.add(formatText(jar.getText()));
    }
  }

  private void processSrcDirs(Element project) {
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
      final InspectionInstance inspection = new InspectionInstance();

      final String id = bug.getAttributeValue("type");
      if (id == null) {
        //illegal report
        myLogger.error("Illegal report: BugInstance has no type");
        return;
      }
      inspection.setInspectionId(id);
      inspection.setMessage(getBugMessage(bug, id));

      final Element classElement = bug.getChild("Class");
      if (classElement == null) {
        //illegal report
        myLogger.error("Illegal report: no Class element for " + id + " bug");
        return;
      }
      int sourceLine;
      String filePath = "";
      final Element sourceLineElement = bug.getChild("SourceLine");
      if (sourceLineElement != null) {
        sourceLine = getSourceLine(sourceLineElement);
        filePath += getFilePath(sourceLineElement);
      } else {
        sourceLine = getSourceLine(classElement);
        filePath += getFilePath(classElement);
      }
      String filePathSpec = getEntitySpec(classElement);
      if (filePath.length() > 0) {
        filePathSpec += " :: " + filePath;
      }
      inspection.setLine(sourceLine);
      inspection.setFilePath(filePathSpec);

      myInspectionReporter.reportInspection(inspection);

      reportInstanceType(id);
    }
  }

  private String getEntitySpec(Element classElem) {
    String classname = classElem.getAttributeValue("classname");
    if (classname == null) {
      //illegal report
      myLogger.error("Illegal report: no classname attribute specified");
      return "";
    }
    return getEntityPath(classname);
  }

  private String getEntityPath(String classname) {
    if (classname.contains("$")) {
      classname = classname.substring(0, classname.indexOf("$"));
    }
    classname = classname + ".class";
    for (String jarPath : myJars) {
      final File f = new File(jarPath);
      if (jarPath.endsWith(classname) && f.isFile()) {
        return f.getAbsolutePath();
      } else if (jarPath.endsWith(".zip") || jarPath.endsWith(".jar")) {
        try {
          final Enumeration<? extends ZipEntry> files = new ZipFile(f).entries();
          while (files.hasMoreElements()) {
            final ZipEntry e = files.nextElement();
            if (!e.isDirectory() && e.getName().endsWith(classname)) {
              myLogger.error(e.getName());
              return e.getName();
            }
          }
        } catch (IOException e) {
          myLogger.exception(e);
        }
      } else {
        final String path = getSourcePathRecursive(f.listFiles(), classname);
        if (path.length() > 0) {
          return path;
        }
      }
    }
    myLogger.error("Couldn't find class: " + classname);
    return "";
  }

  private int getSourceLine(Element sourceLineElem) {
    final String start = sourceLineElem.getAttributeValue("start");
    if (start != null) {
      try {
        return Integer.parseInt(start);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  private String getFilePath(Element sourceLineElem) {
    String path = sourceLineElem.getAttributeValue("sourcepath");
    if (path != null) {
      path = getSourcePath(path);
      if (path.startsWith(myCheckoutDirectory)) {
        path = path.substring(myCheckoutDirectory.length());
      }
      path = path.replace("\\", "|").replace("/", "|");
      if (path.startsWith("|")) {
        path = path.substring(1);
      }
      return path;
    }
    return "";
  }

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
        myLogger.error("Illegal report: unknown bug type " + id);
      }
    }
  }

  private String getBugMessage(Element bugElem, String id) {
    String message = "<No message>";
    if (bugElem.getChild("ShortMessage") != null) {
      message = formatText(bugElem.getChild("ShortMessage").getText());
    } else if (bugElem.getChild("LongMessage") != null) {
      message = formatText(bugElem.getChild("LongMessage").getText());
    } else if (FindBugsPatterns.isCommonPattern(id)) {
      message = FindBugsPatterns.getDescription(id);
    } else if (myBugPatterns.containsKey(id)) {
      message = myBugPatterns.get(id);
    }
    return message;
  }

  private String getSourcePath(String relativePath) {
    String path;
    for (String s : mySrcDirs) {
      final File srcDir = new File(s);
      if (!srcDir.isDirectory()) {
        myLogger.warning("Directory " + s + " doesn't exist on disc");
        continue;
      }
//      final File f = new File(s + File.separator + relativePath);
//      if (f.exists()) {
//        return f.getPath();
//      }
      path = getSourcePathRecursive(srcDir.listFiles(), relativePath);
      if (path.length() > 0) {
        return path;
      }
    }
    return "";
  }

  private String getSourcePathRecursive(File[] files, String relativePath) {
    if (files == null) {
      return "";
    }
    for (int i = 0; i < files.length; ++i) {
      if (files[i].isFile()) {
        final String path = files[i].getAbsolutePath();
        if (path.endsWith(relativePath)) {
          return path;
        }
      } else if (files[i].isDirectory()) {
        final String path = getSourcePathRecursive(files[i].listFiles(), relativePath);
        if (path.length() > 0) {
          return path;
        }
      }
    }
    return "";
  }

  private void processBugCategories(Element rootElem) {
    final List categories = rootElem.getChildren("BugCategory");
    for (Object o : categories) {
      final Element category = (Element) o;

      final String id = category.getAttributeValue("category");
      final Element description = category.getChild("Description");
      if ((id == null) || FindBugsCategories.isCommonCategory(id) || myCategories.containsKey(id) || (description == null)) {
        //illegal report
        myLogger.error("Illegal report: bad bug category specified");
        return;
      }
      myCategories.put(id, formatText(description.getText()));
    }
  }

  private void processBugPatterns(Element rootElem) {
    final List patterns = rootElem.getChildren("BugPattern");
    for (Object o : patterns) {
      final Element pattern = (Element) o;

      final String id = pattern.getAttributeValue("type");
      final Element shortDescription = pattern.getChild("ShortDescription");
      final Element details = pattern.getChild("Details");
      String category = pattern.getAttributeValue("category");

      if ((id == null) || FindBugsPatterns.isCommonPattern(id)
        || (shortDescription == null) || (details == null) || (category == null) || myReportedInstanceTypes.contains(id)) {
        //illegal report
        myLogger.error("Illegal report: bad bug pattern specified");
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
        myLogger.error("Illegal report: unknown bug category specified in bug " + id);
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