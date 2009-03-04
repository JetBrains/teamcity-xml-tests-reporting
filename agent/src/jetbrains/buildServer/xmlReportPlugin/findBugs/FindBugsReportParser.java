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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.InspectionslReportParser;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FindBugsReportParser extends InspectionslReportParser {
  public static final String TYPE = "findBugs";

  private static final String DEFAULT_MESSAGE = "No message";

  private FileFinder myFileFinder;

  private FindBugsCategories myCategories;
  private String myCurrentCategory;

  private FindBugsPatterns myBugPatterns;
  private String myCurrentPattern;

  private String myCurrentClass;

  private List<InspectionInstance> myWaitingForTypeBugs;

  private boolean myDataLoaded;

  public FindBugsReportParser(@NotNull final BaseServerLoggerFacade logger,
                              @NotNull InspectionReporter inspectionReporter,
                              @NotNull String checkoutDirectory) {
    super(logger, inspectionReporter, checkoutDirectory);
    myDataLoaded = false;
    myCategories = new FindBugsCategories();
    myBugPatterns = new FindBugsPatterns();
  }

  private static boolean hasNoMessage(InspectionInstance i) {
    return DEFAULT_MESSAGE.equals(i.getMessage());
  }

  private static boolean hasNoFilePath(InspectionInstance i) {
    return "".equals(i.getFilePath());
  }

  public int parse(@NotNull File report, int testsToSkip) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    myFileFinder = new FileFinder();
    myWaitingForTypeBugs = new ArrayList<InspectionInstance>();

    try {
      if (!myDataLoaded) {
        myDataLoaded = true;
        myCategories.loadCategories(myLogger, this.getClass().getResourceAsStream("categories.xml"));
        myBugPatterns.loadPatterns(myLogger, this.getClass().getResourceAsStream("patterns.xml"));
      }
      parse(report);
      for (InspectionInstance bug : myWaitingForTypeBugs) {
        if (hasNoMessage(bug)) {
          bug.setMessage(getPattern(bug.getInspectionId()).getDescription());
        }
        myInspectionReporter.reportInspection(bug);
        reportInspectionType(bug.getInspectionId());
      }
    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      if (myFileFinder != null) {
        myFileFinder.close();
      }
      myInspectionReporter.flush();
    }
    return -1;
  }

//  Handler methods

  public void startElement(String uri, String localName,
                           String qName, Attributes attributes) throws SAXException {
    if ("BugCategory".equals(localName)) {
      myCurrentCategory = attributes.getValue("category");
      myCategories.getCategories().put(myCurrentCategory, new FindBugsCategories.Category());
    } else if ("BugPattern".equals(localName)) {
      myCurrentPattern = attributes.getValue("type");
      myBugPatterns.getPatterns().put(myCurrentPattern, new FindBugsPatterns.Pattern(attributes.getValue("category")));
    } else if ("BugInstance".equals(localName)) {
      myCurrentBug = new InspectionInstance();
      myCurrentBug.setInspectionId(attributes.getValue("type"));
      myCurrentBug.setMessage(DEFAULT_MESSAGE);
      myCurrentBug.setLine(0);
      myCurrentBug.setFilePath("");

      processPriority(getNumber(attributes.getValue("priority")));
    } else if ("Class".equals(localName) && (myCurrentClass == null)) {
      myCurrentClass = attributes.getValue("classname");
    } else if ("SourceLine".equals(localName) && attributes.getValue("classname").equals(myCurrentClass)) {
      myCurrentBug.setLine(getNumber(attributes.getValue("start")));
      if (hasNoFilePath(myCurrentBug)) {
        myCurrentBug.setFilePath(createPathSpec(attributes));
      }
    }
  }

  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("Jar".equals(localName) || "SrcDir".equals(localName)) {
      myFileFinder.addJar(formatText(myCData));
    } else if ("BugCategory".equals(localName)) {
      myCurrentCategory = null;
    } else if ("BugPattern".equals(localName)) {
      myCurrentPattern = null;
    } else if ("BugInstance".equals(localName)) {
      if (isTypeKnown(myCurrentBug)) {
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
      if ("Description".equals(localName)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setName(text);
        }
      } else if ("Details".equals(localName) && (myCData.length() > 0)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setDescription(text);
        } else if (myCurrentPattern != null) {
          getPattern(myCurrentPattern).setDescription(text);
        }
      } else if ("ShortDescription".equals(localName) && (myCData.length() > 0)) {
        if (myCurrentPattern != null) {
          getPattern(myCurrentPattern).setName(text);
        }
      } else if ("ShortMessage".equals(localName) || "LongMessage".equals(localName)) {
        if ((myCurrentBug != null) && hasNoMessage(myCurrentBug)) {
          myCurrentBug.setMessage(text);
        }
      }
    }
    myCData.delete(0, myCData.length());
  }

  // Auxiliary methods

  private FindBugsCategories.Category getCategory(String id) {
    return myCategories.getCategories().get(id);
  }

  private FindBugsPatterns.Pattern getPattern(String id) {
    return myBugPatterns.getPatterns().get(id);
  }

  private void reportInspectionType(String id) {
    final FindBugsPatterns.Pattern pattern = getPattern(id);
    reportInspectionType(id, pattern.getName(), getCategory(pattern.getCategory()).getName(), getCategory(pattern.getCategory()).getDescription());
  }

  private boolean isTypeKnown(InspectionInstance bug) {
    return (getPattern(bug.getInspectionId()) != null);
  }

  private String createPathSpec(Attributes attributes) {
    String pathSpec = myFileFinder.getVeryFullFilePath(myCurrentClass.replace(".", File.separator) + ".class");
    if (pathSpec.startsWith(myCheckoutDirectory)) {
      pathSpec = pathSpec.substring(myCheckoutDirectory.length());
    }
    if (pathSpec.startsWith(File.separator)) {
      pathSpec = pathSpec.substring(1);
    }
    pathSpec = pathSpec.replace(File.separator, "/");

    String path;
    path = myFileFinder.getVeryFullFilePath(attributes.getValue("sourcepath"));

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
    return pathSpec;
  }
}