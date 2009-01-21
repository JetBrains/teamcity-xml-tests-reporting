/*
 * Copyright 2008 JetBrains s.r.o.
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
package jetbrains.buildServer.testReportParserPlugin;


import jetbrains.buildServer.controllers.RememberState;
import jetbrains.buildServer.controllers.StateField;

import java.util.ArrayList;
import java.util.List;


public class ReportTypeForm extends RememberState {
  @StateField
  private List<ReportTypeInfo> myAvailableReportTypes;

  public ReportTypeForm() {
//    myAvailableReportTypes = new ArrayList<ReportTypeInfo>(TestReportParserPluginUtil.SUPPORTED_REPORT_TYPES.size());
//
//    for (Map.Entry<String, String> reportInfo: TestReportParserPluginUtil.SUPPORTED_REPORT_TYPES.entrySet()) {
//      myAvailableReportTypes.add(new ReportTypeInfo(reportInfo.getKey(), reportInfo.getValue()));
//    }

    myAvailableReportTypes = new ArrayList<ReportTypeInfo>();
    myAvailableReportTypes.add(new ReportTypeInfo("junit", "Ant JUnit reports"));
    myAvailableReportTypes.add(new ReportTypeInfo("nunit", "NUnit reports"));
    myAvailableReportTypes.add(new ReportTypeInfo("surefire", "Surefire reports"));
//    myAvailableReportTypes.add(new ReportTypeInfo("findBugs", "FindBugs reports"));
  }

  public List<ReportTypeInfo> getAvailableReportTypes() {
    return myAvailableReportTypes;
  }

  public static class ReportTypeInfo {
    @StateField
    private String myType;
    @StateField
    private String myDisplayName;

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
  }
}
