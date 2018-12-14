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

import java.util.*;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginConstants.*;

public class XmlReportPluginUtil {
  public static final Map<String, String> SUPPORTED_REPORT_TYPES;
  private static final List<String> INSPECTIONS_TYPES = Arrays.asList("findBugs",
                                                                      "pmd",
                                                                      "checkstyle",
                                                                      "jslint");

  static {
    final Map<String, String> reportTypes = new HashMap<String, String>();
    reportTypes.put("junit", "Ant JUnit");
    reportTypes.put("nunit", "NUnit");
    reportTypes.put("surefire", "Surefire");
    reportTypes.put("findBugs", "FindBugs");
    reportTypes.put("pmd", "PMD");
    reportTypes.put("checkstyle", "Checkstyle");
    reportTypes.put("pmdCpd", "PMD CPD");
    reportTypes.put("mstest", "MSTest");
    reportTypes.put("vstest", "VSTest");
    reportTypes.put("trx", "TRX");
    reportTypes.put("gtest", "Google Test");
    reportTypes.put("jslint", "JSLint");
    reportTypes.put("ctest", "CTest");
    reportTypes.put("testng", "TestNG");
    SUPPORTED_REPORT_TYPES = Collections.unmodifiableMap(reportTypes);
  }

  public static boolean isParsingEnabled(@NotNull final Map<String, String> params) {
    return StringUtil.isNotEmpty(params.get(REPORT_TYPE));
  }

  public static boolean isOutputVerbose(@NotNull final Map<String, String> params) {
    return Boolean.parseBoolean(params.get(VERBOSE_OUTPUT));
  }

  public static boolean isParseOutOfDateReports(@NotNull final Map<String, String> params) {
    return Boolean.parseBoolean(params.get(PARSE_OUT_OF_DATE));
  }

  public static boolean isReparseUpdatedReports(@NotNull final Map<String, String> params) {
    final String reparseUpdated = params.get(REPARSE_UPDATED);
    return reparseUpdated == null || Boolean.parseBoolean(reparseUpdated);
  }

  @Nullable
  public static String getXmlReportPaths(@NotNull final Map<String, String> params) {
    return params.get(REPORT_DIRS);
  }

  @Nullable
  public static String getReportType(@NotNull final Map<String, String> params) {
    return params.get(REPORT_TYPE);
  }

  public static int getMaxErrors(@NotNull final Map<String, String> params) {
    return getMaxErrorsOrWarnings(params, MAX_ERRORS);
  }

  public static int getMaxWarnings(@NotNull final Map<String, String> params) {
    return getMaxErrorsOrWarnings(params, MAX_WARNINGS);
  }

  private static int getMaxErrorsOrWarnings(@NotNull final Map<String, String> params, String what) {
    String errorsCount = params.get(what);
    if (StringUtil.isNotEmpty(errorsCount)) {
      if (!StringUtil.isNumber(errorsCount)) {
        return -1;
      }
      try {
        return Integer.parseInt(errorsCount);
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

  @Nullable
  public static String whenNoDataPublished(@NotNull final Map<String, String> params) {
    return params.containsKey(WHEN_NO_DATA_PUBLISHED) ? params.get(WHEN_NO_DATA_PUBLISHED) : "error";
  }

  public static boolean isFailBuildIfParsingFailed(@NotNull final Map<String, String> params) {
    return !params.containsKey(FAIL_BUILD_IF_PARSING_FAILED) || Boolean.parseBoolean(params.get(FAIL_BUILD_IF_PARSING_FAILED));
  }

  public static boolean isLogIsInternal(@NotNull final Map<String, String> params) {
    final String param = params.get(LOG_AS_INTERNAL);
    return param != null ? Boolean.parseBoolean(param) : !isOutputVerbose(params);
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