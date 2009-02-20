package jetbrains.buildServer.xmlReportPlugin;

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

  private final XmlReportPlugin myPlugin;

  public XmlReportDataProcessor(@NotNull XmlReportPlugin plugin) {
    myPlugin = plugin;
  }

  public void processData(@NotNull File file, @NotNull Map<String, String> arguments) {
    final Map<String, String> params = new HashMap<String, String>();

    params.put(XmlReportPluginUtil.REPORT_TYPE, this.getType());

    String verboseOutput = "false";
    if (arguments.containsKey(VERBOSE_ARGUMENT)) {
      verboseOutput = arguments.get(VERBOSE_ARGUMENT);
    }
    params.put(XmlReportPluginUtil.VERBOSE_OUTPUT, verboseOutput);

    String parseOutOfDate = "false";
    if (arguments.containsKey(PARSE_OUT_OF_DATE_ARGUMENT)) {
      parseOutOfDate = arguments.get(PARSE_OUT_OF_DATE_ARGUMENT);
    }
    params.put(XmlReportPluginUtil.PARSE_OUT_OF_DATE, parseOutOfDate);

    if (arguments.containsKey(ERRORS_LIMIT_ARGUMENT)) {
      params.put(XmlReportPluginUtil.MAX_ERRORS, arguments.get(ERRORS_LIMIT_ARGUMENT));
    }
    if (arguments.containsKey(WARNINGS_LIMIT_ARGUMENT)) {
      params.put(XmlReportPluginUtil.MAX_WARNINGS, arguments.get(WARNINGS_LIMIT_ARGUMENT));
    }

    final List<File> reportDirs = new ArrayList<File>();
    reportDirs.add(file);

    myPlugin.processReports(params, reportDirs);
  }

  public static class JUnitDataProcessor extends XmlReportDataProcessor {
    public JUnitDataProcessor(XmlReportPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "junit";
    }
  }

  public static class NUnitDataProcessor extends XmlReportDataProcessor {
    public NUnitDataProcessor(XmlReportPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "nunit";
    }
  }

  public static class SurefireDataProcessor extends XmlReportDataProcessor {
    public SurefireDataProcessor(XmlReportPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "surefire";
    }
  }

  public static class FindBugsDataProcessor extends XmlReportDataProcessor {
    public FindBugsDataProcessor(XmlReportPlugin plugin) {
      super(plugin);
    }

    @NotNull
    public String getType() {
      return "findBugs";
    }
  }
}