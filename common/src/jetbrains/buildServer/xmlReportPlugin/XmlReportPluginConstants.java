

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
  static final String FAIL_BUILD_IF_PARSING_FAILED = "xmlReportParsing.failBuildIfParsingFailed";
  static final String LOG_AS_INTERNAL = "xmlReportParsing.logAsInternal";
  static final String LOG_INTERNAL_SYSTEM_ERROR = "xmlReportParsing.logInternalSystemError";
  static final String REPARSE_UPDATED = "xmlReportParsing.reparse.updated";

  static final String QUIET_MODE = "xmlReportParsing.quietMode";

  static final String SPLIT_REGEX = " *[,\n\r] *";

  static final String BUILD_PROBLEM_TYPE = "xmlReportParsing";
}