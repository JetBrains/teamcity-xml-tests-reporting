package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.DataProcessor;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.SUPPORTED_REPORT_TYPES;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//"##teamcity[importData type='XmlReport' reportType='sometype' file='somedir']"
// service messsage activates watching "somedir" directory for reports of sometype type
//"##teamcity[importData type='XmlReport' reportType='sometype' file='somedir' verbose='true']"
// does the same and sets output verbose

public class XmlReportDataProcessor implements DataProcessor {
  private static final String DATA_PROCESSOR_ID = "XmlReport";
  private static final String DATA_PROCESSOR_VERBOSE_ARGUMENT = "verbose";
  private static final String DATA_PROCESSOR_REPORT_TYPE_ARGUMENT = "reportType";

  private TestReportParserPlugin myPlugin;

  public XmlReportDataProcessor(TestReportParserPlugin plugin) {
    myPlugin = plugin;
  }

  public void processData(@NotNull File file, Map<String, String> arguments) throws Exception {
    String reportType = arguments.get(DATA_PROCESSOR_REPORT_TYPE_ARGUMENT);

    if (reportType == null) {
      reportType = myPlugin.getParameters().getReportType();
    } else if (!SUPPORTED_REPORT_TYPES.containsKey(reportType)) {
      myPlugin.getLogger().error("Wrong report type specified in service message arguments: " + reportType);
      return;
    }

    boolean verboseOutput = false;
    if (arguments.containsKey(DATA_PROCESSOR_VERBOSE_ARGUMENT)) {
      verboseOutput = Boolean.parseBoolean(arguments.get(DATA_PROCESSOR_VERBOSE_ARGUMENT));
    }
    myPlugin.getParameters().setVerboseOutput(verboseOutput);
    myPlugin.getLogger().setVerboseOutput(verboseOutput);

    final List<File> reportDirs = new ArrayList<File>();
    reportDirs.add(file);

    myPlugin.processReports(reportType, reportDirs);
  }

  @NotNull
  public String getType() {
    return DATA_PROCESSOR_ID;
  }
}
