

package jetbrains.buildServer.xmlReportPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.serverSide.BuildFeature;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 15.12.10
 * Time: 21:14
 */
public class XmlReportPluginBuildFeature extends BuildFeature {
  public static final String FEATURE_TYPE = "xml-report-plugin";
  private final String myEditUrl;

  public XmlReportPluginBuildFeature(@NotNull final PluginDescriptor descriptor) {
    myEditUrl = descriptor.getPluginResourcesPath("xmlReportParserSettings.jsp");
  }

  @NotNull
  @Override
  public String getType() {
    return FEATURE_TYPE;
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return "XML report processing";
  }

  @Override
  public String getEditParametersUrl() {
    return myEditUrl;
  }

  @Override
  public boolean isMultipleFeaturesPerBuildTypeAllowed() {
    return true;
  }

  @NotNull
  @Override
  public String describeParameters(@NotNull final Map<String, String> params) {
    StringBuilder result = new StringBuilder();
    if (XmlReportPluginUtil.isParsingEnabled(params)) {
     result.append("Import ").append(XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(XmlReportPluginUtil.getReportType(params)))
       .append(" reports ").append(" from\n").append(XmlReportPluginUtil.getXmlReportPaths(params));
    }
    return result.toString();
  }

  @Override
  public PropertiesProcessor getParametersProcessor() {
      return new PropertiesProcessor() {
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final List<InvalidProperty> invalids = new ArrayList<InvalidProperty>();

        if (!XmlReportPluginUtil.isParsingEnabled(properties)) {
          invalids.add(new InvalidProperty(XmlReportPluginConstants.REPORT_TYPE,
            "Report type must be specified"));

        } else {
          String prop;
          prop = properties.get(XmlReportPluginConstants.REPORT_DIRS);
          if (prop == null || ("".equals(prop))) {
            invalids.add(new InvalidProperty(XmlReportPluginConstants.REPORT_DIRS,
              "Monitoring rules must be specified"));
          }
        }

        return invalids;
      }
    };
  }
}