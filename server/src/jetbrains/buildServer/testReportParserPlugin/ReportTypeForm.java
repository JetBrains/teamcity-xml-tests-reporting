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
    myAvailableReportTypes = new ArrayList(2);
    myAvailableReportTypes.add(new ReportTypeInfo("junit", "JUnit", "JUnit tests reports"));
    myAvailableReportTypes.add(new ReportTypeInfo("nunit", "NUnit", "NUnit tests reports"));
    myAvailableReportTypes.add(new ReportTypeInfo("surefire", "Surefire", "Surefire tests reports"));
  }

  public List<ReportTypeInfo> getAvailableReportTypes() {
    return myAvailableReportTypes;
  }

  public void setAvailableReportTypes(List<ReportTypeInfo> availableReportTypes) {
    myAvailableReportTypes = availableReportTypes;
  }

  /**
   * Represents information about a build runner in the form suitable for JSP
   */
  public class ReportTypeInfo {
    @StateField
    private String myType;
    @StateField
    private String myDisplayName;
    @StateField
    private String myDescription;

    public ReportTypeInfo(final String type,
                          final String typeDisplayName,
                          final String typeDescription) {
      myType = type;
      myDisplayName = typeDisplayName;
      myDescription = typeDescription;
    }

    public String getType() {
      return myType;
    }

    public String getDisplayName() {
      return myDisplayName;
    }

    public String getDescription() {
      return myDescription;
    }
  }
}
