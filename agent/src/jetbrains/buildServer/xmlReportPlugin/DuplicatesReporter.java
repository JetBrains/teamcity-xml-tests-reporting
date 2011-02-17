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

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 08.02.11
 * Time: 15:24
 */
public interface DuplicatesReporter {
  /**
   * Indicates the beginning of a duplicates block
   */
  void startDuplicates();

  /**
   * Reports duplicate within block
   *
   * @param duplicate Duplicate info
   */
  void reportDuplicate(@NotNull DuplicationInfo duplicate);

  /**
   * Indicates the end of a duplicates block
   */
  void finishDuplicates();

  static final class FragmentInfo {
    @Nullable
    private final String myPath;
    private final int myLine;

    public FragmentInfo(@Nullable String path, int line) {
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

  static final class DuplicationInfo {
    private final int myLines;
    private final int myTokens;
    private int myHash;

    @NotNull
    private final List<FragmentInfo> myFragments = new ArrayList<FragmentInfo>();

    public DuplicationInfo(int lines, int tokens) {
      myLines = lines;
      myTokens = tokens;
    }

    public int getLines() {
      return myLines;
    }

    public int getTokens() {
      return myTokens;
    }

    public void addFragment(@NotNull FragmentInfo fragment) {
      myFragments.add(fragment);
    }

    @NotNull
    public List<FragmentInfo> getFragments() {
      return myFragments;
    }

    public int getHash() {
      return myHash;
    }

    public void setHash(int hash) {
      myHash = hash;
    }
  }
}

