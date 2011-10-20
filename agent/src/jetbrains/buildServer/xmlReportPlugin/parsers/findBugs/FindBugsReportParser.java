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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionParsingResult;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

class FindBugsReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FindBugsReportParser.class);

  @NotNull
  private final InspectionReporter myInspectionReporter;

  @NotNull
  private final File myBaseFolder;

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
                              @Nullable final String findBugsHome,
                              @NotNull final File baseFolder) {
    this(inspectionReporter, findBugsHome, baseFolder, true);
  }

  public FindBugsReportParser(@NotNull final InspectionReporter inspectionReporter,
                              @Nullable final String findBugsHome,
                              @NotNull final File baseFolder,
                              boolean lookForFiles) {
    myInspectionReporter = inspectionReporter;
    myBaseFolder = baseFolder;
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

    myFileFinder = lookForFiles ? new FileFinder() : new FileFinder() {
      @Override
      public void addJar(@NotNull final String jar) {}
      @Override
      public String getVeryFullFilePath(@Nullable final String filePath) {return null;}
      @Override
      public void close() {}
    };

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
        public void jarFound(@NotNull final String jar) {
          myFileFinder.addJar(FileUtil.resolvePath(myBaseFolder, jar).getAbsolutePath());
        }

        public void bugInstanceFound(@Nullable final String file,
                                     @Nullable final String clazz,
                                     final int line,
                                     @Nullable final String type,
                                     @Nullable final String category,
                                     @Nullable final String message,
                                     @Nullable final String details,
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
          final String cName = myCategories.containsKey(category) ? myCategories.get(category).getName() : category;
          final String descr = myCategories.containsKey(category) ? myCategories.get(category).getDescription() : null;
          final String mess = getFullMessage(message, myPatterns.containsKey(type) ? myPatterns.get(type).getDescription() : null, details);
          final String pName = myPatterns.containsKey(type) ? myPatterns.get(type).getName() : type;

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

  @Nullable
  private static String getFullMessage(@Nullable String message, @Nullable String defaultMessage, @Nullable String details) {
    if (StringUtil.isEmpty(message)) message = defaultMessage;
    if (StringUtil.isEmpty(message)) return details;
    if (StringUtil.isEmpty(details)) return message;
    return message + details;
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

    if (file != null) return file;
    if (sourcepath != null) return sourcepath;
    return clazz;
  }

  @SuppressWarnings({"ConstantConditions"})
  private String formatText(@NotNull String s) {
    if (myDetailsParser == null) return s;
    try {
      myDetailsParser.parse(new BufferedReader(new StringReader(s)));
    } catch (IOException e) {
      LOG.warn("Couldn't format html description to text", e);
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
}