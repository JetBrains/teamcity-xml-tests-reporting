/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import jetbrains.buildServer.serverSide.ProjectManager;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.buildType.EditBuildRunnerSettingsExtension;
import jetbrains.buildServer.web.openapi.buildType.ViewBuildRunnerSettingsExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class TestReportParserPluginSettings {
    public TestReportParserPluginSettings(@NotNull final PagePlaces pagePlaces, @NotNull final ProjectManager projectManager) {
        List<String> supportedRunTypes = Arrays.asList("Ant");

        EditBuildRunnerSettingsExtension editSettingsExtension =
                new EditBuildRunnerSettingsExtension(pagePlaces, supportedRunTypes);
        editSettingsExtension.setPluginName("TestReportParserPlugin");
        editSettingsExtension.setIncludeUrl("testReportParserSettings.jsp");
        editSettingsExtension.register();

        ViewBuildRunnerSettingsExtension viewSettingsExtension =
                new ViewBuildRunnerSettingsExtension(projectManager, pagePlaces, supportedRunTypes);
        viewSettingsExtension.setPluginName("TestReportParserPlugin");
        viewSettingsExtension.setIncludeUrl("viewTestReportParserSettings.jsp");
        viewSettingsExtension.register();
    }
}
//public CoverageSettingsPageExtension(@NotNull final PagePlaces pagePlaces, @NotNull final ProjectManager projectManager) {
//  List<String> supportedRunTypes = Arrays.asList("Ant", "Ipr");
//
//  EditBuildRunnerSettingsExtension editSettingsExtension =
//    new EditBuildRunnerSettingsExtension(pagePlaces, supportedRunTypes);
//  editSettingsExtension.setPluginName("coverage");
//  editSettingsExtension.setIncludeUrl("coverageParams.jsp");
//  editSettingsExtension.register();
//
//  ViewBuildRunnerSettingsExtension viewSettingsExtension =
//    new ViewBuildRunnerSettingsExtension(projectManager, pagePlaces, supportedRunTypes);
//  viewSettingsExtension.setPluginName("coverage");
//  viewSettingsExtension.setIncludeUrl("viewCoverageParams.jsp");
//  viewSettingsExtension.register();
//}
