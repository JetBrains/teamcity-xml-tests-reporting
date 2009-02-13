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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.agent.inspections.InspectionTypeInfo;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;


public class FindBugsReportParser implements TestReportParser {
  public static final String TYPE = "findBugs";

  private final SimpleBuildLogger myLogger;
  private final InspectionReporter myInspectionReporter;
  private final String myCheckoutDirectory;
  private final Set<String> myReportedInstanceTypes;

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
  }

  public void parse(@NotNull File file) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    try {
      FindBugsCategories.loadCategories(myLogger, this.getClass().getResourceAsStream("categories.xml"));
      FindBugsPatterns.loadPatterns(myLogger, this.getClass().getResourceAsStream("patterns.xml"));

      SortedBugCollection collection = new SortedBugCollection();
      Project project = new Project();
      collection.readXML(file, project);
      Collection<BugInstance> bugs = collection.getCollection();
      FileFinder fileFinder = new FileFinder();

//      stupid action for preventing \n and Co symbols
      List<String> files = new ArrayList<String>();
      for (String s : project.getFileList()) {
        files.add(formatText(s));
      }
      fileFinder.addJars(files);
      List<String> srcDirs = new ArrayList<String>();
      for (String s : project.getSourceDirList()) {
        srcDirs.add(formatText(s));
      }
      fileFinder.addJars(srcDirs);
//      end stupid action for preventing \n and Co symbols

      for (BugInstance warning : bugs) {
//          switch (warning.getPriority()) {
//              case 1:
//                  priority = Priority.HIGH;
//                  break;
//              case 2:
//                  priority = Priority.NORMAL;
//                  break;
//              default:
//                  priority = Priority.LOW;
//          }

        SourceLineAnnotation sourceLine = warning.getPrimarySourceLineAnnotation();
        InspectionInstance instance = new InspectionInstance();
        int line = sourceLine.getStartLine();
        if (line < 0) {
          line = 0;
        }
        instance.setLine(line);
        instance.setMessage(warning.getMessageWithoutPrefix());
        instance.setInspectionId(warning.getType());
        reportType(warning.getType());

        String pathSpec = fileFinder.getVeryFullFilePath(warning.getPrimaryClass().getClassName().replace(".", File.separator) + ".class");
        if (pathSpec.startsWith(myCheckoutDirectory)) {
          pathSpec = pathSpec.substring(myCheckoutDirectory.length());
        }
        if (pathSpec.startsWith(File.separator)) {
          pathSpec = pathSpec.substring(1);
        }

        String path;
        path = fileFinder.getVeryFullFilePath(sourceLine.getSourcePath());

        if (path.startsWith(myCheckoutDirectory)) {
          path = path.substring(myCheckoutDirectory.length());
        }
        path = path.replace("\\", "|").replace("/", "|");
        if (path.startsWith("|")) {
          path = path.substring(1);
        }
        if (path.length() > 0) {
          pathSpec += " :: " + path;
        }
        instance.setFilePath(pathSpec);

        myInspectionReporter.reportInspection(instance);
      }
    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      myInspectionReporter.flush();
    }
  }

  private void reportType(String type) {
    if (!myReportedInstanceTypes.contains(type) && FindBugsPatterns.isCommonPattern(type)) {
      final InspectionTypeInfo info = new InspectionTypeInfo();
      info.setId(type);
      info.setName(FindBugsPatterns.getName(type));
      info.setDescription(FindBugsPatterns.getDescription(type));

      if (FindBugsCategories.isCommonCategory(FindBugsPatterns.getCategory(type))) {
        info.setCategory(FindBugsCategories.getName(FindBugsPatterns.getCategory(type)));
      } else {
        info.setCategory("other");
      }

      myInspectionReporter.reportInspectionType(info);
      myReportedInstanceTypes.add(type);
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
//    boolean limitReached = false;
//
//    final Integer errorLimit = PropertiesUtil.parseInt(runParameters.get(FxCopConstants.SETTINGS_ERROR_LIMIT));
//    if (errorLimit != null && errors > errorLimit) {
//      myLogger.error("Errors limit reached: found " + errors + " errors, limit " + errorLimit);
//      limitReached = true;
//    }
//
//    final Integer warningLimit = PropertiesUtil.parseInt(runParameters.get(FxCopConstants.SETTINGS_WARNING_LIMIT));
//    if (warningLimit != null && warnings > warningLimit) {
//      myLogger.error("Warnings limit reached: found " + warnings + " warnings, limit " + warningLimit);
//      limitReached = true;
//    }
//
//    final String buildStatus = generateBuildStatus(errors, warnings);
//    getLogger().message("##teamcity[buildStatus status='" +
//                        (limitReached ? "FAILURE" : "SUCCESS") +
//                        "' text='" + buildStatus + "']");
  }
}