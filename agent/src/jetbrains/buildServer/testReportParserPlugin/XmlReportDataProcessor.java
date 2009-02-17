package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.DataProcessor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//"##teamcity[importData type='sometype' file='somedir']"
// service message activates watching "somedir" directory for reports of sometype type
//"##teamcity[importData type='sometype' file='somedir' verbose='true']"
// does the same and sets output verbose
//"##teamcity[importData type='sometype' file='somedir' verbose='true' parseOutOfDate='true']"
// does the same and enables parsing out-of-date reports

//"##teamcity[importData type='fundBugs' file='somedir' errorLimit='100' warningLimit='200']"
//starts watching somedir directory for FindBugs reports, buils will fail if some report has

//more than errorLimit errors or more than warningLimit warnings
public abstract class XmlReportDataProcessor implements DataProcessor {
  public static final String VERBOSE_ARGUMENT = "verbose";
  public static final String PARSE_OUT_OF_DATE_ARGUMENT = "parseOutOfDate";
  public static final String ERRORS_LIMIT_ARGUMENT = "errorLimit";
  public static final String WARNINGS_LIMIT_ARGUMENT = "warningLimit";

  private final TestReportParserPlugin myPlugin;

  public XmlReportDataProcessor(@NotNull TestReportParserPlugin plugin) {
    myPlugin = plugin;
  }

  public void processData(@NotNull File file, @NotNull Map<String, String> arguments) {
    final Map<String, String> params = new HashMap<String, String>();

    params.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_REPORT_TYPE, this.getType());

    String verboseOutput = "false";
    if (arguments.containsKey(VERBOSE_ARGUMENT)) {
      verboseOutput = arguments.get(VERBOSE_ARGUMENT);
    }
    params.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_VERBOSE_OUTPUT, verboseOutput);

    String parseOutOfDate = "false";
    if (arguments.containsKey(PARSE_OUT_OF_DATE_ARGUMENT)) {
      parseOutOfDate = arguments.get(PARSE_OUT_OF_DATE_ARGUMENT);
    }
    params.put(TestReportParserPluginUtil.TEST_REPORT_PARSING_PARSE_OUT_OF_DATE, parseOutOfDate);

    final List<File> reportDirs = new ArrayList<File>();
    reportDirs.add(file);

    myPlugin.processReports(params, reportDirs);
  }

  public static class JUnitDataProcessor extends XmlReportDataProcessor {
    public JUnitDataProcessor(TestReportParserPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "junit";
    }
  }

  public static class NUnitDataProcessor extends XmlReportDataProcessor {
    public NUnitDataProcessor(TestReportParserPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "nunit";
    }
  }

  public static class SurefireDataProcessor extends XmlReportDataProcessor {
    public SurefireDataProcessor(TestReportParserPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "surefire";
    }
  }

  public static class FindBugsDataProcessor extends XmlReportDataProcessor {
    public FindBugsDataProcessor(TestReportParserPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "findBugs";
    }
  }
}