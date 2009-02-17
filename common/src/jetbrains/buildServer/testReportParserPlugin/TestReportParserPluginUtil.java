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

package jetbrains.buildServer.testReportParserPlugin;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;


public class TestReportParserPluginUtil {
  public static final Map<String, String> SUPPORTED_REPORT_TYPES = new HashMap<String, String>();

  static {
    SUPPORTED_REPORT_TYPES.put("junit", "Ant JUnit reports");
    SUPPORTED_REPORT_TYPES.put("nunit", "NUnit reports");
    SUPPORTED_REPORT_TYPES.put("surefire", "Surefire reports");
    SUPPORTED_REPORT_TYPES.put("findBugs", "FindBugs reports");
  }

  public static final String TEST_REPORT_PARSING_REPORT_TYPE = "testReportParsing.reportType";
  public static final String TEST_REPORT_PARSING_REPORT_DIRS = "testReportParsing.reportDirs";
  public static final String TEST_REPORT_PARSING_VERBOSE_OUTPUT = "testReportParsing.verboseOutput";
  public static final String TEST_REPORT_PARSING_PARSE_OUT_OF_DATE = "testReportParsing.parse.outofdate";
  public static final String TEST_REPORT_PARSING_BUILD_START = "testReportParsing.buildStart";
  public static final String TEST_REPORT_PARSING_WORKING_DIR = "testReportParsing.workingDir";
  public static final String TEST_REPORT_PARSING_TMP_DIR = "testReportParsing.tmpDir";
  public static final String TEST_REPORT_PARSING_MAX_ERRORS = "testReportParsing.max.errors";
  public static final String TEST_REPORT_PARSING_MAX_WARNINGS = "testReportParsing.max.warnings";

  public static boolean isParsingEnabled(@NotNull final Map<String, String> runParams) {
    return runParams.containsKey(TEST_REPORT_PARSING_REPORT_TYPE) &&
      !runParams.get(TEST_REPORT_PARSING_REPORT_TYPE).equals("");
  }

  public static boolean isOutputVerbose(@NotNull final Map<String, String> runParams) {
    return runParams.containsKey(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
  }

  public static boolean shouldParseOutOfDateReports(@NotNull final Map<String, String> systemProperties) {
    return systemProperties.containsKey(TEST_REPORT_PARSING_PARSE_OUT_OF_DATE);
  }

  public static void enableTestReportParsing(@NotNull final Map<String, String> runParams, String reportType) {
    if (reportType.equals("")) {
      runParams.remove(TEST_REPORT_PARSING_REPORT_TYPE);
      runParams.remove(TEST_REPORT_PARSING_REPORT_DIRS);
      runParams.remove(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
      runParams.remove(TEST_REPORT_PARSING_PARSE_OUT_OF_DATE);
    } else {
      runParams.put(TEST_REPORT_PARSING_REPORT_TYPE, reportType);
    }
  }

  public static void setVerboseOutput(@NotNull final Map<String, String> runParams, boolean verboseOutput) {
    if (isParsingEnabled(runParams)) {
      if (verboseOutput) {
        runParams.put(TEST_REPORT_PARSING_VERBOSE_OUTPUT, "true");
      } else {
        runParams.remove(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
      }
    }
  }

  public static void setParseOutOfDateReports(@NotNull final Map<String, String> systemProperties, boolean shouldParse) {
    if (shouldParse) {
      systemProperties.put(TEST_REPORT_PARSING_PARSE_OUT_OF_DATE, "true");
    } else {
      systemProperties.remove(TEST_REPORT_PARSING_PARSE_OUT_OF_DATE);
    }
  }

  public static void setTestReportDirs(@NotNull final Map<String, String> runParams, String reportDirs) {
    if (isParsingEnabled(runParams)) {
      runParams.put(TEST_REPORT_PARSING_REPORT_DIRS, reportDirs);
    }
  }

  public static String getTestReportDirs(@NotNull final Map<String, String> runParams) {
    return runParams.get(TEST_REPORT_PARSING_REPORT_DIRS);
  }

  public static String getReportType(@NotNull final Map<String, String> runParams) {
    return runParams.get(TEST_REPORT_PARSING_REPORT_TYPE);
  }

  public static void setReportType(@NotNull final Map<String, String> runParams, String type) {
    if (isParsingEnabled(runParams)) {
      runParams.put(TEST_REPORT_PARSING_REPORT_TYPE, type);
    }
  }

  public static int getMaxErrors(@NotNull final Map<String, String> runParams) {
    if (runParams.containsKey(TEST_REPORT_PARSING_MAX_ERRORS)) {
      try {
        return Integer.parseInt(runParams.get(TEST_REPORT_PARSING_MAX_ERRORS));
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  public static void setMaxErrors(@NotNull final Map<String, String> runParams, int maxErrors) {
    if (isParsingEnabled(runParams)) {
      runParams.put(TEST_REPORT_PARSING_MAX_ERRORS, "" + maxErrors);
    }
  }

  public static int getMaxWarnings(@NotNull final Map<String, String> runParams) {
    if (runParams.containsKey(TEST_REPORT_PARSING_MAX_WARNINGS)) {
      try {
        return Integer.parseInt(runParams.get(TEST_REPORT_PARSING_MAX_WARNINGS));
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  public static void setMaxWarnings(@NotNull final Map<String, String> runParams, int maxWarnings) {
    if (isParsingEnabled(runParams)) {
      runParams.put(TEST_REPORT_PARSING_MAX_WARNINGS, "" + maxWarnings);
    }
  }
}