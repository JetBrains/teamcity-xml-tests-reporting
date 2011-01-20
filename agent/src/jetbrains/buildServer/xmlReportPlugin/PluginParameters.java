/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 16.11.10
 * Time: 18:27
 */
public interface PluginParameters {
  @NotNull
  String getCheckoutDir();

  @Nullable
  String getFindBugsHome();

  @NotNull
  String getTmpDir();

  @NotNull
  String getNUnitSchema();

  boolean checkReportComplete();

  boolean checkReportGrows();

  boolean isVerbose();

  long getBuildStartTime();

  @NotNull
  FlowLogger getThreadLogger();

  @Nullable
  InspectionReporter getInspectionReporter();

  @Nullable
  DuplicatesReporter getDuplicatesReporter();
}
