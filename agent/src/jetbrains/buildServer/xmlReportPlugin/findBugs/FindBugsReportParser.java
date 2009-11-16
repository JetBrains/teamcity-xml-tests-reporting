/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.InspectionsReportParser;
import jetbrains.buildServer.xmlReportPlugin.ReportData;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class FindBugsReportParser extends InspectionsReportParser {
  public static final String TYPE = "findBugs";

  private static final String DEFAULT_MESSAGE = "No message";

  private FileFinder myFileFinder;

  private String myFindBugsHome;

  private final BugCollection myBugCollection;
  private String myCurrentCategory;
  private String myCurrentPattern;

  private String myCurrentClass;

  private List<InspectionInstance> myWaitingForTypeBugs;

  private boolean myPatternsLoaded;

  public FindBugsReportParser(@NotNull final BaseServerLoggerFacade logger,
                              @NotNull InspectionReporter inspectionReporter,
                              @NotNull String checkoutDirectory,
                              String findBugsHome) {
    super(logger, inspectionReporter, checkoutDirectory);
    myPatternsLoaded = false;
    myBugCollection = new BugCollection();
    myFindBugsHome = findBugsHome;
  }

  private static boolean hasNoMessage(InspectionInstance i) {
    return DEFAULT_MESSAGE.equals(i.getMessage());
  }

  private static boolean hasNoFilePath(InspectionInstance i) {
    return "".equals(i.getFilePath());
  }

  public void parse(@NotNull final ReportData data) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    final File report = data.getFile();
    if (!isReportComplete(report, "</BugCollection>")) {
      data.setProcessedEvents(0);
      return;
    }
    myFileFinder = new FileFinder();
    myWaitingForTypeBugs = new ArrayList<InspectionInstance>();
    try {
      if (!myPatternsLoaded) {
        myPatternsLoaded = true;
        if (myFindBugsHome != null) {
          myBugCollection.loadPatternsFromFindBugs(new File(myFindBugsHome));
        } else {
          myLogger.warning("FindBugs home path setting is not specified. Please specify FindBugs home path setting. This path is used for loading bug patterns names and descriptions.");
          myBugCollection.loadBundledPatterns();
        }
      }
      parse(report);
      for (InspectionInstance bug : myWaitingForTypeBugs) {
        if (hasNoMessage(bug)) {
          if (isTypeKnown(bug)) {
            bug.setMessage(getPattern(bug.getInspectionId()).getDescription());
          }
        }
        myInspectionReporter.reportInspection(bug);
        reportInspectionType(bug.getInspectionId());
      }
    } catch (SAXParseException spe) {
      myLogger.error(report.getAbsolutePath() + " is not parsable by FindBugs parser");
    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      if (myFileFinder != null) {
        myFileFinder.close();
      }
      myInspectionReporter.flush();
    }
    data.setProcessedEvents(-1);
  }

  public void logParsingTotals(@NotNull Map<String, String> parameters, boolean verbose) {
    if (myPatternsLoaded) {
      super.logParsingTotals(parameters, verbose);
    }
  }

//  Handler methods

  public void startElement(String uri, String name,
                           String qName, Attributes attributes) throws SAXException {
    if ("BugCategory".equals(name)) {
      myCurrentCategory = attributes.getValue("category");
      myBugCollection.getCategories().put(myCurrentCategory, new BugCollection.Category());
    } else if ("BugPattern".equals(name)) {
      myCurrentPattern = attributes.getValue("type");
      myBugCollection.getPatterns().put(myCurrentPattern, new BugCollection.Pattern(attributes.getValue("category")));
    } else if ("BugInstance".equals(name)) {
      myCurrentBug = new InspectionInstance();
      myCurrentBug.setInspectionId(attributes.getValue("type"));
      myCurrentBug.setMessage(DEFAULT_MESSAGE);
      myCurrentBug.setLine(0);
      myCurrentBug.setFilePath("");

      processPriority(getNumber(attributes.getValue("priority")));
    } else if ("Class".equals(name) && (myCurrentClass == null)) {
      myCurrentClass = attributes.getValue("classname");
    } else if ("SourceLine".equals(name) && attributes.getValue("classname").equals(myCurrentClass)) {
      myCurrentBug.setLine(getNumber(attributes.getValue("start")));
      if (hasNoFilePath(myCurrentBug)) {
        myCurrentBug.setFilePath(createPathSpec(attributes.getValue("sourcepath")));
      }
    }
  }

  public void endElement(String uri, String name, String qName) throws SAXException {
    if ("Jar".equals(name) || "SrcDir".equals(name)) {
      myFileFinder.addJar(formatText(myCData));
    } else if ("BugCategory".equals(name)) {
      myCurrentCategory = null;
    } else if ("BugPattern".equals(name)) {
      myCurrentPattern = null;
    } else if ("BugInstance".equals(name)) {
      if (isTypeKnown(myCurrentBug)) {
        if (hasNoFilePath(myCurrentBug)) {
          myCurrentBug.setFilePath(createPathSpec(""));
        }
        if (hasNoMessage(myCurrentBug)) {
          myCurrentBug.setMessage(getPattern(myCurrentBug.getInspectionId()).getDescription());
        }
        reportInspectionType(myCurrentBug.getInspectionId());
        myInspectionReporter.reportInspection(myCurrentBug);
      } else {
        myWaitingForTypeBugs.add(myCurrentBug);
      }
      myCurrentBug = null;
      myCurrentClass = null;
    } else if (myCData.length() > 0) {
      final String text = formatText(myCData);
      if ("Description".equals(name)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setName(text);
        }
      } else if ("Details".equals(name) && (myCData.length() > 0)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setDescription(text);
        } else if (myCurrentPattern != null) {
          getPattern(myCurrentPattern).setDescription(text);
        }
      } else if ("ShortDescription".equals(name) && (myCData.length() > 0)) {
        if (myCurrentPattern != null) {
          getPattern(myCurrentPattern).setName(text);
        }
      } else if ("ShortMessage".equals(name) || "LongMessage".equals(name)) {
        if ((myCurrentBug != null) && hasNoMessage(myCurrentBug)) {
          myCurrentBug.setMessage(text);
        }
      } else if ("MissingClass".equals(name)) {
        myLogger.warning("Missing class " + text);
      }
    }
    myCData.delete(0, myCData.length());
  }

  // Auxiliary methods

  private BugCollection.Category getCategory(String id) {
    return myBugCollection.getCategories().get(id);
  }

  private BugCollection.Pattern getPattern(String id) {
    return myBugCollection.getPatterns().get(id);
  }

  private void reportInspectionType(String id) {
    final BugCollection.Pattern pattern = getPattern(id);
    reportInspectionType(id, pattern.getName(), getCategory(pattern.getCategory()).getName(), getCategory(pattern.getCategory()).getDescription());
  }

  private boolean isTypeKnown(InspectionInstance bug) {
    return (getPattern(bug.getInspectionId()) != null);
  }

  private String createPathSpec(String sourcepath) {
    String pathSpec = "";
    if ((sourcepath != null) && (sourcepath.length() > 0)) {
      pathSpec = myFileFinder.getVeryFullFilePath(sourcepath);
    }
    if (pathSpec.length() == 0) {
      pathSpec = myFileFinder.getVeryFullFilePath(myCurrentClass.replace(".", File.separator) + ".class");
    }
    if (pathSpec.startsWith(myCheckoutDirectory)) {
      pathSpec = pathSpec.substring(myCheckoutDirectory.length());
    }
    if (pathSpec.startsWith(File.separator)) {
      pathSpec = pathSpec.substring(1);
    }
    pathSpec = pathSpec.replace(File.separator, "/");
    return pathSpec;
  }
}