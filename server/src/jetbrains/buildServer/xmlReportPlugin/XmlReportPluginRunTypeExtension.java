/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunTypeExtension;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * User: vbedrosova
 * Date: 30.09.10
 * Time: 16:34
 */
public class XmlReportPluginRunTypeExtension extends RunTypeExtension {
  private static final String SETTINGS = "xmlReportParserSettings.jsp";
  private static final String VIEW_SETTINGS = "viewXmlReportParserSettings.jsp";

  private List<String> mySupportedRunTypes = new ArrayList<String>();

  public final String myEditUrl;
  public final String myViewUrl;

  public XmlReportPluginRunTypeExtension(@NotNull final PluginDescriptor descriptor) {
    myEditUrl = descriptor.getPluginResourcesPath(SETTINGS);
    myViewUrl = descriptor.getPluginResourcesPath(VIEW_SETTINGS);
  }

  @Override
  public Collection<String> getRunTypes() {
    return mySupportedRunTypes;
  }

  @Override
  public PropertiesProcessor getRunnerPropertiesProcessor() {
    return new PropertiesProcessor() {
      public Collection<InvalidProperty> process(Map<String, String> properties) {
        final List<InvalidProperty> invalids = new ArrayList<InvalidProperty>();
        String prop;
        prop = properties.get(XmlReportPluginUtil.REPORT_DIRS);
        if (XmlReportPluginUtil.isParsingEnabled(properties) && (prop == null) || ("".equals(prop))) {
          invalids.add(new InvalidProperty(XmlReportPluginUtil.REPORT_DIRS,
            "Report paths must be specified"));
        }
        return invalids;
      }
    };
  }

  @Override
  public String getEditRunnerParamsJspFilePath() {
    return myEditUrl;
  }

  @Override
  public String getViewRunnerParamsJspFilePath() {
    return myViewUrl;
  }

  @Override
  public Map<String, String> getDefaultRunnerProperties() {
    return Collections.emptyMap();
  }

  //used in spring
  public void setSupportedRunTypes(List<String> supportedRunTypes) {
    mySupportedRunTypes = supportedRunTypes;
  }
}
