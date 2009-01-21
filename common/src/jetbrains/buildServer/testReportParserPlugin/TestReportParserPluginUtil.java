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

  public static boolean isTestReportParsingEnabled(@NotNull final Map<String, String> runParams) {
    return runParams.containsKey(TEST_REPORT_PARSING_REPORT_TYPE) &&
      !runParams.get(TEST_REPORT_PARSING_REPORT_TYPE).equals("");
  }

  public static boolean isOutputVerbose(@NotNull final Map<String, String> runParams) {
    return runParams.containsKey(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
  }

  public static void enableTestReportParsing(@NotNull final Map<String, String> runParams, String reportType) {
    if (reportType.equals("")) {
      runParams.remove(TEST_REPORT_PARSING_REPORT_TYPE);
      runParams.remove(TEST_REPORT_PARSING_REPORT_DIRS);
      runParams.remove(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
    } else {
      runParams.put(TEST_REPORT_PARSING_REPORT_TYPE, reportType);
    }
  }

  public static void setVerboseOutput(@NotNull final Map<String, String> runParams, boolean verboseOutput) {
    if (isTestReportParsingEnabled(runParams)) {
      if (verboseOutput) {
        runParams.put(TEST_REPORT_PARSING_VERBOSE_OUTPUT, "true");
      } else {
        runParams.remove(TEST_REPORT_PARSING_VERBOSE_OUTPUT);
      }
    }
  }

  public static void setTestReportDirs(@NotNull final Map<String, String> runParams, String reportDirs) {
    if (isTestReportParsingEnabled(runParams)) {
      runParams.put(TEST_REPORT_PARSING_REPORT_DIRS, reportDirs);
    }
  }

  public static String getTestReportDirs(@NotNull final Map<String, String> runParams) {
    return runParams.get(TEST_REPORT_PARSING_REPORT_DIRS);
  }

  public static String getReportType(@NotNull final Map<String, String> runParams) {
    return runParams.get(TEST_REPORT_PARSING_REPORT_TYPE);
  }
}