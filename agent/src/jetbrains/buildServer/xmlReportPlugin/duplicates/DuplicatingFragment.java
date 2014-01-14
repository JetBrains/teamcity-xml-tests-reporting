/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.duplicates;

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 14:54
 */
public class DuplicatingFragment {
  @Nullable
  private final String myPath;
  private final int myLine;

  public DuplicatingFragment(@Nullable String path, int line) {
    myPath = path;
    myLine = line;
  }

  @Nullable
  public String getPath() {
    return myPath;
  }

  public int getLine() {
    return myLine;
  }
}
