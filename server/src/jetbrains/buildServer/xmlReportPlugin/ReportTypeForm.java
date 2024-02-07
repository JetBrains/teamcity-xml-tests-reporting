

package jetbrains.buildServer.xmlReportPlugin;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.controllers.RememberState;
import jetbrains.buildServer.controllers.StateField;


public class ReportTypeForm extends RememberState {
  @StateField
  private final List<ReportTypeInfo> myAvailableReportTypes;

  public ReportTypeForm() {
    myAvailableReportTypes = new ArrayList<ReportTypeInfo>(XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.size());

    for (Map.Entry<String, String> reportInfo : XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.entrySet()) {
      myAvailableReportTypes.add(new ReportTypeInfo(reportInfo.getKey(), reportInfo.getValue()));
    }

    Collections.sort(myAvailableReportTypes);
  }

  public List<ReportTypeInfo> getAvailableReportTypes() {
    return myAvailableReportTypes;
  }

  public static class ReportTypeInfo implements Comparable<ReportTypeInfo> {
    @StateField
    private final String myType;
    @StateField
    private final String myDisplayName;

    public ReportTypeInfo(final String type,
                          final String typeDisplayName) {
      myType = type;
      myDisplayName = typeDisplayName;
    }

    public String getType() {
      return myType;
    }

    public String getDisplayName() {
      return myDisplayName;
    }

    public int compareTo(final ReportTypeInfo o) {
     return myDisplayName.compareTo(o.getDisplayName());
    }
  }
}