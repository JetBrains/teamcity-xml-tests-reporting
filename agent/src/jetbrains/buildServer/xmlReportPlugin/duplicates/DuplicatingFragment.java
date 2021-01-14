/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 14:54
 */
public class DuplicatingFragment {
  @NotNull
  private final String myPath;
  private final int myLine;
  private int myHash;

  public DuplicatingFragment(@NotNull String path, int line) {
    myPath = path;
    myLine = line;
  }

  @NotNull
  public String getPath() {
    return myPath;
  }

  public int getLine() {
    return myLine;
  }

  public void setHash(int hash) {
    myHash = hash;
  }

  /**
   * Note: hash is set after all fragments withing one {@link jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult} collected
   */
  public int getHash() {
    return myHash;
  }
}
