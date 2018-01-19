/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginConstants.*;


public class XmlReportPluginUtil {
  public static final Map<String, String> SUPPORTED_REPORT_TYPES = new HashMap<String, String>();
  public static final List<String> INSPECTIONS_TYPES = new LinkedList<String>();
  public static final List<String> DUPLICATES_TYPES = new LinkedList<String>();

  static {
    SUPPORTED_REPORT_TYPES.put("junit", "Ant JUnit");
    SUPPORTED_REPORT_TYPES.put("nunit", "NUnit");
    SUPPORTED_REPORT_TYPES.put("surefire", "Surefire");
    SUPPORTED_REPORT_TYPES.put("findBugs", "FindBugs");
    SUPPORTED_REPORT_TYPES.put("pmd", "PMD");
    SUPPORTED_REPORT_TYPES.put("checkstyle", "Checkstyle");
    SUPPORTED_REPORT_TYPES.put("pmdCpd", "PMD CPD");
    SUPPORTED_REPORT_TYPES.put("mstest", "MSTest");
    SUPPORTED_REPORT_TYPES.put("vstest", "VSTest");
    SUPPORTED_REPORT_TYPES.put("trx", "TRX");
    SUPPORTED_REPORT_TYPES.put("gtest", "Google Test");
    SUPPORTED_REPORT_TYPES.put("jslint", "JSLint");
    SUPPORTED_REPORT_TYPES.put("ctest", "CTest");

    INSPECTIONS_TYPES.add("findBugs");
    INSPECTIONS_TYPES.add("pmd");
    INSPECTIONS_TYPES.add("checkstyle");
    INSPECTIONS_TYPES.add("jslint");

    DUPLICATES_TYPES.add("pmdCpd");
  }

  public static boolean isParsingEnabled(@NotNull final Map<String, String> params) {
    return params.containsKey(REPORT_TYPE) && !"".equals(params.get(REPORT_TYPE));
  }

  public static boolean isOutputVerbose(@NotNull final Map<String, String> params) {
    return params.containsKey(VERBOSE_OUTPUT) && Boolean.parseBoolean(params.get(VERBOSE_OUTPUT));
  }

  public static boolean isParseOutOfDateReports(@NotNull final Map<String, String> params) {
    return params.containsKey(PARSE_OUT_OF_DATE) && Boolean.parseBoolean(params.get(PARSE_OUT_OF_DATE));
  }

  public static boolean isReparseUpdatedReports(@NotNull final Map<String, String> params) {
    final String reparseUpdated = params.get(REPARSE_UPDATED);
    return reparseUpdated == null || Boolean.parseBoolean(reparseUpdated);
  }

  public static void enableXmlReportParsing(@NotNull final Map<String, String> params, String reportType) {
    if (reportType.equals("")) {
      params.remove(REPORT_TYPE);
      params.remove(REPORT_DIRS);
      params.remove(VERBOSE_OUTPUT);
      params.remove(PARSE_OUT_OF_DATE);
    } else {
      params.put(REPORT_TYPE, reportType);
    }
  }

  public static void setVerboseOutput(@NotNull final Map<String, String> params, boolean verboseOutput) {
    if (isParsingEnabled(params)) {
      if (verboseOutput) {
        params.put(VERBOSE_OUTPUT, "true");
      } else {
        params.remove(VERBOSE_OUTPUT);
      }
    }
  }

  public static void setParseOutOfDateReports(@NotNull final Map<String, String> systemProperties, boolean shouldParse) {
    if (shouldParse) {
      systemProperties.put(PARSE_OUT_OF_DATE, "true");
    } else {
      systemProperties.remove(PARSE_OUT_OF_DATE);
    }
  }

  public static void setXmlReportPaths(@NotNull final Map<String, String> params, String reportDirs) {
    if (isParsingEnabled(params)) {
      params.put(REPORT_DIRS, reportDirs);
    }
  }

  public static String getXmlReportPaths(@NotNull final Map<String, String> params) {
    return params.get(REPORT_DIRS);
  }

  public static String getReportType(@NotNull final Map<String, String> params) {
    return params.get(REPORT_TYPE);
  }

  public static int getMaxErrors(@NotNull final Map<String, String> params) {
    return getMaxErrorsOrWarnings(params, MAX_ERRORS);
  }

  public static void setMaxErrors(@NotNull final Map<String, String> params, int maxErrors) {
    if (isParsingEnabled(params)) {
      params.put(MAX_ERRORS, "" + maxErrors);
    }
  }

  public static int getMaxWarnings(@NotNull final Map<String, String> params) {
    return getMaxErrorsOrWarnings(params, MAX_WARNINGS);
  }

  public static void setMaxWarnings(@NotNull final Map<String, String> params, int maxWarnings) {
    if (isParsingEnabled(params)) {
      params.put(MAX_WARNINGS, "" + maxWarnings);
    }
  }

  private static int getMaxErrorsOrWarnings(@NotNull final Map<String, String> params, String what) {
    if (params.containsKey(what)) {
      try {
        return Integer.parseInt(params.get(what));
      } catch (NumberFormatException e) {
        return -1;
      }
    }
    return -1;
  }

  public static String getFindBugsHomePath(@NotNull final Map<String, String> params) {
    return params.get(FINDBUGS_HOME);
  }

  public static boolean isFindBugsLookupFiles(@NotNull final Map<String, String> params) {
    final String param = params.get(FINDBUGS_LOOKUP_FILES);
    return param == null || Boolean.parseBoolean(param);
  }

//  public static boolean isCheckReportComplete(@NotNull final Map<String, String> params) {
//    return !"false".equalsIgnoreCase(params.get(CHECK_REPORT_COMPLETE));
//  }
//
//  public static boolean isCheckReportGrows(@NotNull final Map<String, String> params) {
//    return !"false".equalsIgnoreCase(params.get(CHECK_REPORT_GROWS));
//  }

  @Nullable
  public static String whenNoDataPublished(@NotNull final Map<String, String> params) {
    return params.containsKey(WHEN_NO_DATA_PUBLISHED) ? params.get(WHEN_NO_DATA_PUBLISHED) : "error";
  }

  public static boolean isFailBuildIfParsingFailed(@NotNull final Map<String, String> params) {
    return !params.containsKey(FAIL_BUILD_IF_PARSING_FAILED) || Boolean.parseBoolean(params.get(FAIL_BUILD_IF_PARSING_FAILED));
  }

  public static boolean isLogIsInternal(@NotNull final Map<String, String> params) {
    return params.containsKey(LOG_AS_INTERNAL) && params.get(LOG_AS_INTERNAL) != null
      ? Boolean.parseBoolean(params.get(LOG_AS_INTERNAL)) : !isOutputVerbose(params);
  }

  public static boolean isLogInternalSystemError(@NotNull final Map<String, String> params) {
    return Boolean.parseBoolean(params.get(LOG_INTERNAL_SYSTEM_ERROR));
  }

  public static boolean isInspectionType(@NotNull String type) {
    return INSPECTIONS_TYPES.contains(type);
  }

  @Nullable
  public static String getReportTypeName(@NotNull String type) {
    return SUPPORTED_REPORT_TYPES.get(type);
  }
}
