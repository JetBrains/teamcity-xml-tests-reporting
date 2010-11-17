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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * User: vbedrosova
 * Date: 16.11.10
 * Time: 16:33
 */
public interface XmlReportPluginParameters extends PluginParameters {
  @NotNull
  Collection<String> getTypes();
  @NotNull
  Collection<File> getPaths(@NotNull String type);
  @NotNull
  PathParameters getPathParameters(@NotNull File path);

  @NotNull
  Map<String, String> getRunnerParameters(); //TODO: use only for getting warning and error limits in inspections


  void updateParameters(@NotNull Set<File> paths,
                        @NotNull Map<String, String> parameters);

  public static interface ParametersListener {
    void pathsAdded(@NotNull String type, @NotNull Set<File> paths);
    void pathsSkipped(@NotNull String type, @NotNull Set<File> paths);
  }

  void setListener(@NotNull ParametersListener listener);
}
