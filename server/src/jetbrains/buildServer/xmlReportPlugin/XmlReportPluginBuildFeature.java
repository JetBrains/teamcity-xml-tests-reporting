/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
