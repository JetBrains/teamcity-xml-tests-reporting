/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/**
 * User: vbedrosova
 * Date: 16.11.10
 * Time: 12:28
 */
public interface XmlReportPluginConstants {
  static final String REPORT_TYPE = "xmlReportParsing.reportType";
  static final String REPORT_DIRS = "xmlReportParsing.reportDirs";

  static final String VERBOSE_OUTPUT = "xmlReportParsing.verboseOutput";

  static final String MAX_ERRORS = "xmlReportParsing.max.errors";
  static final String MAX_WARNINGS = "xmlReportParsing.max.warnings";

  static final String FINDBUGS_HOME = "xmlReportParsing.findBugs.home";
  static final String FINDBUGS_LOOKUP_FILES = "xmlReportParsing.findBugs.lookup.files";

  static final String PARSE_OUT_OF_DATE = "xmlReportParsing.parse.outofdate";
  static final String WHEN_NO_DATA_PUBLISHED = "xmlReportParsing.whenNoDataPublished";
  static final String LOG_AS_INTERNAL = "xmlReportParsing.logAsInternal";
  static final String LOG_INTERNAL_SYSTEM_ERROR = "xmlReportParsing.logInternalSystemError";

//  static final String CHECK_REPORT_GROWS = "xmlReportParsing.check.report.grows";
//  static final String CHECK_REPORT_COMPLETE = "xmlReportParsing.check.report.complete";

  static final String SPLIT_REGEX = " *[,\n\r] *";

  static final String BUILD_PROBLEM_TYPE = "xmlReportParsing";
}
