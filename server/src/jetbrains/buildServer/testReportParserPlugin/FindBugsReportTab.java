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

import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;


public class FindBugsReportTab extends ViewLogTab {
  private static String TAB_TITLE = "FindBugs";
  private static String TAB_CODE = "findBugsReportTab";

  public FindBugsReportTab(final PagePlaces pagePlaces, final SBuildServer server) {
    super(TAB_TITLE, TAB_CODE, pagePlaces, server);
    setPluginName("xml-report-plugin");
    setIncludeUrl("findBugsReportTab.jsp");
  }

  protected void fillModel(Map model, HttpServletRequest request, @Nullable SBuild build) {
  }

  public boolean isAvailable(@NotNull final HttpServletRequest request) {
    final SBuild build = getBuild(request);
//    return build != null;
    return false;
  }
}