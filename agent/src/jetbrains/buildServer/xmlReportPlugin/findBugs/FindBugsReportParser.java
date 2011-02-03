/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FindBugsReportParser extends InspectionsReportParser {
  public static final String TYPE = "findBugs";

  public static final String BUNDLED_VERSION = "1.3.9";

  private static final String DEFAULT_MESSAGE = "No message";

  private FileFinder myFileFinder;

  private String myCurrentReport;

  @Nullable
  private final String myFindBugsHome;

  @NotNull
  private final BugCollection myBugCollection;
  private String myCurrentCategory;
  private String myCurrentPattern;

  private String myCurrentClass;

  private List<InspectionInstance> myWaitingForTypeBugs;

  private boolean myPatternsFromFindBugsLoaded;
  private boolean myBundledPatternsLoaded;

  public FindBugsReportParser(@NotNull XMLReader xmlReader,
                              @NotNull InspectionReporter inspectionReporter,
                              @NotNull File checkoutDirectory,
                              @Nullable String findBugsHome,
                              @NotNull BuildProgressLogger logger) {
    super(xmlReader, inspectionReporter, checkoutDirectory, logger, true);
    myFindBugsHome = findBugsHome;
    myPatternsFromFindBugsLoaded = false;
    myBundledPatternsLoaded = false;
    myBugCollection = new BugCollection();
  }

  private static boolean hasNoMessage(InspectionInstance i) {
    return DEFAULT_MESSAGE.equals(i.getMessage());
  }

  private static boolean hasNoFilePath(InspectionInstance i) {
    return "".equals(i.getFilePath());
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "BugCollection")) {
      return false;
    }

    myCurrentReport = file.getAbsolutePath();

    myFileFinder = new FileFinder();
    myWaitingForTypeBugs = new ArrayList<InspectionInstance>();

    try {
      if (!myPatternsFromFindBugsLoaded && !myBundledPatternsLoaded) {
        if (myFindBugsHome != null) {
          myPatternsFromFindBugsLoaded = true;
          myBugCollection.loadPatternsFromFindBugs(new File(myFindBugsHome), myLogger);
        } else {
          myBundledPatternsLoaded = true;
          myBugCollection.loadBundledPatterns();
        }
      }
      parse(file);
      for (InspectionInstance bug : myWaitingForTypeBugs) {
        if (hasNoMessage(bug)) {
          if (isTypeKnown(bug)) {
            bug.setMessage(getPattern(bug.getInspectionId()).getDescription());
          }
        }
        myInspectionReporter.reportInspection(bug);
        reportInspectionType(bug.getInspectionId());
      }
    } finally {
      myFileFinder.close();
      myInspectionReporter.flush();
    }
    return true;
  }

//  Handler methods

  @Override
  public void startElement(String uri, String name,
                           String qName, Attributes attributes) throws SAXException {
    if ("BugCollection".equals(name)) {
      final String version = attributes.getValue("version");
      if (myBundledPatternsLoaded && !BUNDLED_VERSION.equals(version)) {
        myLogger.warning("FindBugs report " + myCurrentReport + " version is " + version + ", but bundled with xml-report-plugin patterns version is " + BUNDLED_VERSION
          + ". Plugin can be unacquainted with some names and descriptions. Specify FindBugs home path setting for loading patterns straight from FindBugs.");
      }
    } else if ("BugCategory".equals(name)) {
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

  @Override
  public void endElement(String uri, String name, String qName) throws SAXException {
    if ("Jar".equals(name) || "SrcDir".equals(name)) {
      myFileFinder.addJar(ParserUtils.formatText(getCData()));
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
    } else if (getCData().length() > 0) {
      final String text = ParserUtils.formatText(getCData());
      if ("Description".equals(name)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setName(text);
        }
      } else if ("Details".equals(name) && (getCData().length() > 0)) {
        if (myCurrentCategory != null) {
          getCategory(myCurrentCategory).setDescription(text);
        } else if (myCurrentPattern != null) {
          getPattern(myCurrentPattern).setDescription(text);
        }
      } else if ("ShortDescription".equals(name) && (getCData().length() > 0)) {
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
    clearCData();
  }

  // Auxiliary methods

  private BugCollection.Category getCategory(String id) {
    if (myBugCollection.getCategories().containsKey(id)) {
      return myBugCollection.getCategories().get(id);
    } else {
      LoggingUtils.LOG.warn("Couldn't get category for " + id);
      return UNKNOWN_CATEGORY;
    }
  }

  private BugCollection.Pattern getPattern(String id) {
    if (myBugCollection.getPatterns().containsKey(id)) {
      return myBugCollection.getPatterns().get(id);
    } else {
      LoggingUtils.LOG.warn("Couldn't get patterns for " + id);
      return UNKNOWN_PATTERN;
    }
  }

  private void reportInspectionType(String id) {
    final BugCollection.Pattern pattern = getPattern(id);
    final BugCollection.Category category = getCategory(pattern.getCategory());
    reportInspectionType(id, pattern.getName(), category.getName(), category.getDescription(), myInspectionReporter);
  }

  private boolean isTypeKnown(InspectionInstance bug) {
    return (getPattern(bug.getInspectionId()) != UNKNOWN_PATTERN);
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

  private static final BugCollection.Category UNKNOWN_CATEGORY = new BugCollection.Category();
  private static final BugCollection.Pattern UNKNOWN_PATTERN = new BugCollection.Pattern();
}