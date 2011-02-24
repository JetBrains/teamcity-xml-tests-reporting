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

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import javax.swing.text.html.parser.DTD;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionParsingResult;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class FindBugsReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FindBugsReportParser.class);

  @NotNull
  private static final String BUNDLED_VERSION = "1.3.9";

  @NotNull
  private final InspectionReporter myInspectionReporter;

  @Nullable
  private final File myFindBugsHome;

  private int myErrors;
  private int myWarnings;
  private int myInfos;

  @NotNull
  private final PatternXmlParser myPatternXmlParser;
  @NotNull
  private final Map<String, NameAndDescritionInfo> myPatterns = new HashMap<String, NameAndDescritionInfo>();
  @NotNull
  private final CategoryXmlParser myCategoryXmlParser;
  @NotNull
  private final Map<String, NameAndDescritionInfo> myCategories = new HashMap<String, NameAndDescritionInfo>();

  @NotNull
  private final FileFinder myFileFinder;

  @Nullable
  private DetailsParser myDetailsParser;

  public FindBugsReportParser(@NotNull final InspectionReporter inspectionReporter,
                              @Nullable final String findBugsHome) {
    myInspectionReporter = inspectionReporter;
    myFindBugsHome = findBugsHome == null ? null : new File(findBugsHome);

    myPatternXmlParser = new PatternXmlParser(new PatternXmlParser.Callback() {
      public void patternFound(@NotNull final String type) {
        if (!myPatterns.containsKey(type)) myPatterns.put(type, new NameAndDescritionInfo());
      }

      public void patternShortDescriptionFound(@NotNull final String type, @NotNull final String description) {
        myPatterns.get(type).setName(description);
      }

      public void patternDetailsFound(@NotNull final String type, @NotNull final String details) {
        myPatterns.get(type).setDescription(formatText(details));
      }
    });

    myCategoryXmlParser = new CategoryXmlParser(new CategoryXmlParser.Callback() {
      public void categoryFound(@NotNull final String category) {
        if (!myCategories.containsKey(category)) myCategories.put(category, new NameAndDescritionInfo());
      }

      public void categoryDescriptionFound(@NotNull final String category, @NotNull final String description) {
        myCategories.get(category).setName(description);
      }

      public void categoryDetailsFound(@NotNull final String category, @NotNull final String details) {
        final String text = formatText(details);
        myCategories.get(category).setDescription(text.substring(0, 1).toUpperCase() + text.substring(1));
      }
    });

    myFileFinder = new FileFinder();
    try {
      myDetailsParser = new DetailsParser(DTD.getDTD(""));
    } catch (IOException e) {
      LOG.warn("Failed to create empty DTD");
    }
  }

  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "BugCollection")) {
      return false;
    }

    if (myFindBugsHome != null) {
      new FindBugsPluginVisitor(new FindBugsPluginVisitor.Callback() {
        public void pluginFound(@NotNull File messages) {
          try {
            //myPatternCategoryXmlParser.parse(findBugs);
            myPatternXmlParser.parse(messages);
            myCategoryXmlParser.parse(messages);
          } catch (IOException e) {
            myInspectionReporter.error("Error occurred while loading bug patterns from " + myFindBugsHome);
          }
        }
      }).visit(myFindBugsHome);
    }

    try {
      myPatternXmlParser.parse(file);
      myCategoryXmlParser.parse(file);

      new FindBugsReportXmlParser(new FindBugsReportXmlParser.Callback() {
        public void findBugsVersionFound(@NotNull final String version) {
          if (myFindBugsHome == null && !BUNDLED_VERSION.equals(version)) {
            myInspectionReporter.warning(
              file + " was generated with FindBugs " + version + ". Bundled FindBugs version is " + BUNDLED_VERSION +
              ". In \"XML Report Processing\" settings in the web specify \"FindBugs home path\" for loading bug patterns straight from FindBugs");
          }
        }

        public void jarFound(@NotNull final String jar) {
          myFileFinder.addJar(jar);
        }

        public void bugInstanceFound(@Nullable final String file,
                                     @Nullable final String clazz,
                                     final int line,
                                     @Nullable final String type,
                                     @Nullable final String category,
                                     @Nullable final String message,
                                     final int priority) {
          switch (priority) {
            case 1:
              ++myErrors;
              break;
            case 2:
              ++myWarnings;
              break;
            default:
              ++myInfos;
          }
          final String cName = myCategories.containsKey(category) ? myCategories.get(category).getName() : null;
          final String descr = myCategories.containsKey(category) ? myCategories.get(category).getDescription() : null;
          final String mess = message == null || message.length() == 0 ? (myPatterns.containsKey(type) ? myPatterns.get(type).getDescription() : null) : message;
          final String pName = myPatterns.containsKey(type) ? myPatterns.get(type).getName() : null;

          myInspectionReporter.reportInspectionType(new InspectionTypeResult(type, pName, descr, cName));
          myInspectionReporter.reportInspection(new InspectionResult(findFile(file, clazz), type, mess, line, priority));
        }
      }).parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    } finally {
      myFileFinder.close();
    }
    return true;
  }

  public ParsingResult getParsingResult() {
    return new InspectionParsingResult(myErrors, myWarnings, myInfos);
  }

  @Nullable
  private String findFile(@Nullable String sourcepath, @Nullable String clazz) {
    String file = null;

    if (sourcepath != null && sourcepath.length() > 0) {
      file = myFileFinder.getVeryFullFilePath(sourcepath);
    }

    if (file == null && clazz != null && clazz.length() > 0) {
      if (clazz.contains("$")) {
        clazz = clazz.substring(0, clazz.indexOf("$"));
      }
      //noinspection ConstantConditions
      file = myFileFinder.getVeryFullFilePath(clazz.replace(".", File.separator) + ".class");
    }

    return file;
  }

  @SuppressWarnings({"ConstantConditions"})
  private String formatText(@NotNull String s) {
    if (myDetailsParser == null) return s;
    try {
      myDetailsParser.parse(new BufferedReader(new StringReader(s)));
    } catch (IOException e) {
      LoggingUtils.LOG.warn("Couldn't format html description to text", e);
    }
    return myDetailsParser.getText().replace("&nbsp", "");
  }


  private static final class NameAndDescritionInfo {
    @NotNull
    private String myName = "";
    @NotNull
    private String myDescription = "";

    @NotNull
    public String getName() {
      return myName;
    }

    public void setName(@NotNull final String name) {
      myName = name;
    }

    @NotNull
    public String getDescription() {
      return myDescription;
    }

    public void setDescription(@NotNull final String description) {
      myDescription = description;
    }
  }

  //  public static final String TYPE = "findBugs";
//
//  public static final String BUNDLED_VERSION = "1.3.9";
//
//  private static final String DEFAULT_MESSAGE = "No message";
//
//  private FileFinder myFileFinder;
//
//  private String myCurrentReport;
//
//  @Nullable
//  private final String myFindBugsHome;
//
//  @NotNull
//  private final BugCollection myBugCollection;
//  private String myCurrentCategory;
//  private String myCurrentPattern;
//
//  private String myCurrentClass;
//
//  private List<InspectionInstance> myWaitingForTypeBugs;
//
//  private boolean myPatternsFromFindBugsLoaded;
//  private boolean myBundledPatternsLoaded;
//
//  public FindBugsReportParser(@NotNull XMLReader xmlReader,
//                              @NotNull InspectionReporter inspectionReporter,
//                              @NotNull File checkoutDirectory,
//                              @Nullable String findBugsHome,
//                              @NotNull BuildProgressLogger logger) {
//    super(xmlReader, inspectionReporter, checkoutDirectory, logger, true);
//    myFindBugsHome = findBugsHome;
//    myPatternsFromFindBugsLoaded = false;
//    myBundledPatternsLoaded = false;
//    myBugCollection = new BugCollection();
//  }
//
//  private static boolean hasNoMessage(InspectionInstance i) {
//    return DEFAULT_MESSAGE.equals(i.getMessage());
//  }
//
//  private static boolean hasNoFilePath(InspectionInstance i) {
//    return "".equals(i.getFilePath());
//  }
//
//  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
//    if (!ParserUtils.isReportComplete(file, "BugCollection")) {
//      return false;
//    }
//
//    myCurrentReport = file.getAbsolutePath();
//
//    myFileFinder = new FileFinder();
//    myWaitingForTypeBugs = new ArrayList<InspectionInstance>();
//
//    try {
//      if (!myPatternsFromFindBugsLoaded && !myBundledPatternsLoaded) {
//        if (myFindBugsHome != null) {
//          myPatternsFromFindBugsLoaded = true;
//          myBugCollection.loadPatternsFromFindBugs(new File(myFindBugsHome), myLogger);
//        } else {
//          myBundledPatternsLoaded = true;
//          myBugCollection.loadBundledPatterns();
//        }
//      }
//      parse(file);
//      for (InspectionInstance bug : myWaitingForTypeBugs) {
//        if (hasNoMessage(bug)) {
//          if (isTypeKnown(bug)) {
//            bug.setMessage(getPattern(bug.getInspectionId()).getDescription());
//          }
//        }
//        myInspectionReporter.reportInspection(bug);
//        reportInspectionType(bug.getInspectionId());
//      }
//    } finally {
//      myFileFinder.close();
//      myInspectionReporter.flush();
//    }
//    return true;
//  }
//
////  Handler methods
//
//  @Override
//  public void startElement(String uri, String name,
//                           String qName, Attributes attributes) throws SAXException {
//    if ("BugCollection".equals(name)) {
//      final String version = attributes.getValue("version");
//      if (myBundledPatternsLoaded && !BUNDLED_VERSION.equals(version)) {
//        myLogger.warning("FindBugs report " + myCurrentReport + " version is " + version + ", but bundled with xml-report-plugin patterns version is " + BUNDLED_VERSION
//          + ". Plugin can be unacquainted with some names and descriptions. Specify FindBugs home path setting for loading patterns straight from FindBugs.");
//      }
//    } else if ("BugCategory".equals(name)) {
//      myCurrentCategory = attributes.getValue("category");
//      myBugCollection.getCategories().put(myCurrentCategory, new BugCollection.Category());
//    } else if ("BugPattern".equals(name)) {
//      myCurrentPattern = attributes.getValue("type");
//      myBugCollection.getPatterns().put(myCurrentPattern, new BugCollection.Pattern(attributes.getValue("category")));
//    } else if ("BugInstance".equals(name)) {
//      myCurrentBug = new InspectionInstance();
//      myCurrentBug.setInspectionId(attributes.getValue("type"));
//      myCurrentBug.setMessage(DEFAULT_MESSAGE);
//      myCurrentBug.setLine(0);
//      myCurrentBug.setFilePath("");
//
//      processPriority(getNumber(attributes.getValue("priority")));
//    } else if ("Class".equals(name) && (myCurrentClass == null)) {
//      myCurrentClass = attributes.getValue("classname");
//    } else if ("SourceLine".equals(name) && attributes.getValue("classname").equals(myCurrentClass)) {
//      myCurrentBug.setLine(getNumber(attributes.getValue("start")));
//      if (hasNoFilePath(myCurrentBug)) {
//        myCurrentBug.setFilePath(createPathSpec(attributes.getValue("sourcepath")));
//      }
//    }
//  }
//
//  @Override
//  public void endElement(String uri, String name, String qName) throws SAXException {
//    if ("Jar".equals(name) || "SrcDir".equals(name)) {
//      myFileFinder.addJar(ParserUtils.formatText(getCData()));
//    } else if ("BugCategory".equals(name)) {
//      myCurrentCategory = null;
//    } else if ("BugPattern".equals(name)) {
//      myCurrentPattern = null;
//    } else if ("BugInstance".equals(name)) {
//      if (isTypeKnown(myCurrentBug)) {
//        if (hasNoFilePath(myCurrentBug)) {
//          myCurrentBug.setFilePath(createPathSpec(""));
//        }
//        if (hasNoMessage(myCurrentBug)) {
//          myCurrentBug.setMessage(getPattern(myCurrentBug.getInspectionId()).getDescription());
//        }
//        reportInspectionType(myCurrentBug.getInspectionId());
//        myInspectionReporter.reportInspection(myCurrentBug);
//      } else {
//        myWaitingForTypeBugs.add(myCurrentBug);
//      }
//      myCurrentBug = null;
//      myCurrentClass = null;
//    } else if (getCData().length() > 0) {
//      final String text = ParserUtils.formatText(getCData());
//      if ("Description".equals(name)) {
//        if (myCurrentCategory != null) {
//          getCategory(myCurrentCategory).setName(text);
//        }
//      } else if ("Details".equals(name) && (getCData().length() > 0)) {
//        if (myCurrentCategory != null) {
//          getCategory(myCurrentCategory).setDescription(text);
//        } else if (myCurrentPattern != null) {
//          getPattern(myCurrentPattern).setDescription(text);
//        }
//      } else if ("ShortDescription".equals(name) && (getCData().length() > 0)) {
//        if (myCurrentPattern != null) {
//          getPattern(myCurrentPattern).setName(text);
//        }
//      } else if ("ShortMessage".equals(name) || "LongMessage".equals(name)) {
//        if ((myCurrentBug != null) && hasNoMessage(myCurrentBug)) {
//          myCurrentBug.setMessage(text);
//        }
//      } else if ("MissingClass".equals(name)) {
//        myLogger.warning("Missing class " + text);
//      }
//    }
//    clearCData();
//  }
//
//  // Auxiliary methods
//
//  private BugCollection.Category getCategory(String id) {
//    if (myBugCollection.getCategories().containsKey(id)) {
//      return myBugCollection.getCategories().get(id);
//    } else {
//      LoggingUtils.LOG.warn("Couldn't get category for " + id);
//      return UNKNOWN_CATEGORY;
//    }
//  }
//
//  private BugCollection.Pattern getPattern(String id) {
//    if (myBugCollection.getPatterns().containsKey(id)) {
//      return myBugCollection.getPatterns().get(id);
//    } else {
//      LoggingUtils.LOG.warn("Couldn't get patterns for " + id);
//      return UNKNOWN_PATTERN;
//    }
//  }
//
//  private void reportInspectionType(String id) {
//    final BugCollection.Pattern pattern = getPattern(id);
//    final BugCollection.Category category = getCategory(pattern.getCategory());
//    reportInspectionType(id, pattern.getName(), category.getName(), category.getDescription(), myInspectionReporter);
//  }
//
//  private boolean isTypeKnown(InspectionInstance bug) {
//    return (getPattern(bug.getInspectionId()) != UNKNOWN_PATTERN);
//  }
//
//  private String createPathSpec(String sourcepath) {
//    String pathSpec = "";
//    if ((sourcepath != null) && (sourcepath.length() > 0)) {
//      pathSpec = myFileFinder.getVeryFullFilePath(sourcepath);
//    }
//    if (pathSpec.length() == 0) {
//      pathSpec = myFileFinder.getVeryFullFilePath(myCurrentClass.replace(".", File.separator) + ".class");
//    }
//    if (pathSpec.startsWith(myCheckoutDirectory)) {
//      pathSpec = pathSpec.substring(myCheckoutDirectory.length());
//    }
//    if (pathSpec.startsWith(File.separator)) {
//      pathSpec = pathSpec.substring(1);
//    }
//    pathSpec = pathSpec.replace(File.separator, "/");
//    return pathSpec;
//  }
//
//  private static final BugCollection.Category UNKNOWN_CATEGORY = new BugCollection.Category();
//  private static final BugCollection.Pattern UNKNOWN_PATTERN = new BugCollection.Pattern();
}