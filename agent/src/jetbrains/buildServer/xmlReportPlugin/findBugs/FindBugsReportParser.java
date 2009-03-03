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

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.*;
import static jetbrains.buildServer.xmlReportPlugin.XmlParserUtil.*;
import jetbrains.buildServer.xmlReportPlugin.XmlReportParser;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.*;


public class FindBugsReportParser extends DefaultHandler implements XmlReportParser {
  public static final String TYPE = "findBugs";

  private static final String DEFAULT_MESSAGE = "No message";

  private final SimpleBuildLogger myLogger;
  private final InspectionReporter myInspectionReporter;
  private final String myCheckoutDirectory;
  private Set<String> myReportedInstanceTypes;

  private int myErrors;
  private int myWarnings;

  private XMLReader myXmlReader;
  private FileFinder myFileFinder;

  private FindBugsCategories myCategories;
  private String myCurrentCategory;

  private FindBugsPatterns myBugPatterns;
  private String myCurrentPattern;

  private InspectionInstance myCurrentBug;
  private String myCurrentClass;

  private StringBuffer myCData;

  private List<InspectionInstance> myWaitingForTypeBugs;

  private boolean myDataLoaded;

  public FindBugsReportParser(@NotNull final SimpleBuildLogger logger,
                              @NotNull InspectionReporter inspectionReporter,
                              @NotNull String checkoutDirectory) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory;
    myErrors = 0;
    myWarnings = 0;
    myReportedInstanceTypes = new HashSet<String>();

    myDataLoaded = false;
    myCategories = new FindBugsCategories();
    myBugPatterns = new FindBugsPatterns();
    try {
      myXmlReader = XMLReaderFactory.createXMLReader();
      myXmlReader.setContentHandler(this);
      myXmlReader.setFeature("http://xml.org/sax/features/validation", false);
    } catch (Exception e) {
      myLogger.exception(e);
    }
  }

  public void startElement(String uri, String localName,
                           String qName, Attributes attributes)
    throws SAXException {
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

  public void characters(char ch[], int start, int length) throws SAXException {
    myCData.append(ch, start, length);
  }

  private FindBugsCategories.Category getCategory(String id) {
    return myCategories.getCategories().get(id);
  }

  private FindBugsPatterns.Pattern getPattern(String id) {
    return myBugPatterns.getPatterns().get(id);
  }

  private void reportInspectionType(String id) {
    if (myReportedInstanceTypes.contains(id)) {
      return;
    }
    final FindBugsPatterns.Pattern pattern = getPattern(id);
    final InspectionTypeInfo type = new InspectionTypeInfo();
    type.setId(id);
    type.setName(pattern.getName());
    type.setDescription(getCategory(pattern.getCategory()).getDescription());
    type.setCategory(getCategory(pattern.getCategory()).getName());
    myInspectionReporter.reportInspectionType(type);
    myReportedInstanceTypes.add(id);
  }

  private static boolean hasNoMessage(InspectionInstance i) {
    return DEFAULT_MESSAGE.equals(i.getMessage());
  }

  private static boolean hasNoFilePath(InspectionInstance i) {
    return "".equals(i.getFilePath());
  }

  private boolean isTypeKnown(InspectionInstance bug) {
    return (getPattern(bug.getInspectionId()) != null);
  }

  private void processPriority(int priority) {
    InspectionSeverityValues level;
    switch (priority) {
      case 1:
        ++myErrors;
        level = InspectionSeverityValues.ERROR;
        break;
      case 2:
        ++myWarnings;
        level = InspectionSeverityValues.WARNING;
        break;
      default:
        level = InspectionSeverityValues.INFO;
    }
    final Collection<String> attrValue = new Vector<String>();
    attrValue.add(level.toString());
    myCurrentBug.addAttribute(InspectionAttributesId.SEVERITY.toString(), attrValue);
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

  public int parse(@NotNull File report, int testsToSkip) {
    myInspectionReporter.markBuildAsInspectionsBuild();
    myFileFinder = new FileFinder();
    myCData = new StringBuffer();
    myWaitingForTypeBugs = new ArrayList<InspectionInstance>();

    try {
      if (!myDataLoaded) {
        myDataLoaded = true;
        myCategories.loadCategories(myLogger, this.getClass().getResourceAsStream("categories.xml"));
        myBugPatterns.loadPatterns(myLogger, this.getClass().getResourceAsStream("patterns.xml"));
      }

      myXmlReader.parse(new InputSource(report.toURI().toString()));

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

  public boolean abnormalEnd() {
    return false;
  }

  public void logReportTotals(File report) {
  }

  public void logParsingTotals(Map<String, String> parameters) {
    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters);
    if ((errorLimit != -1) && (myErrors > errorLimit)) {
      myLogger.error("Errors limit reached: found " + myErrors + " errors, limit " + errorLimit);
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters);
    if ((warningLimit != -1) && (myWarnings > warningLimit)) {
      myLogger.error("Warnings limit reached: found " + myWarnings + " warnings, limit " + warningLimit);
      limitReached = true;
    }

    final String buildStatus = generateBuildStatus(myErrors, myWarnings);
    myLogger.message("##teamcity[buildStatus status='" +
      (limitReached ? "FAILURE" : "SUCCESS") +
      "' text='" + buildStatus + "']");
  }
}