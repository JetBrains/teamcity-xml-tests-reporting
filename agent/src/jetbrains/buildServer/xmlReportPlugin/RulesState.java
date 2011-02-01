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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 20.01.11
 * Time: 16:52
 */
public class RulesState implements FilesState {
  @NotNull
  private final Map<File, ParsingResult> myParsingResults = new HashMap<File, ParsingResult>();

  @NotNull
  public synchronized FileState getFileState(@NotNull File file) {
    if (myParsingResults.containsKey(file)) {
      if (myParsingResults.get(file) != null) {
        return FilesState.FileState.PROCESSED;
      }
      return FilesState.FileState.ON_PROCESSING;
    }
    return FilesState.FileState.UNKNOWN;
  }

  public synchronized void setFileProcessed(@NotNull File file, @NotNull ParsingResult parsingResult) {
    if (!myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is not present");
    }
    if (myParsingResults.get(file) != null) {
      throw new IllegalStateException("File " + file + " is already processed");
    }
    myParsingResults.put(file, parsingResult);
  }

  public synchronized void addFile(@NotNull File file) {
    if (myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is already present");
    }
    myParsingResults.put(file, null);
  }

  public synchronized void removeFile(@NotNull File file) {
    if (!myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is not present");
    }
    myParsingResults.remove(file);
  }

  @NotNull
  public synchronized Map<File, ParsingResult> getFiles() {
    return myParsingResults;
  }
}
