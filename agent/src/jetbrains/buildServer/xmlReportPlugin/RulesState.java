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

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 20.01.11
 * Time: 16:52
 */
public class RulesState implements ReportStateHolder {
  @NotNull
  private final Map<File, FileState> myParsingResults = new HashMap<File, FileState>();

  @NotNull
  public synchronized ReportState getReportState(@NotNull final File report) {
    return myParsingResults.containsKey(report) ? myParsingResults.get(report).reportState : ReportState.UNKNOWN;
  }

  @Nullable
  public synchronized Long getLastModified(@NotNull final File report) {
    return myParsingResults.containsKey(report) ? myParsingResults.get(report).lastModified : null;
  }

  @Nullable
  public synchronized Long getLength(@NotNull final File report) {
    return myParsingResults.containsKey(report) ? myParsingResults.get(report).length : null;
  }

  public synchronized void setReportState(@NotNull final File report, @NotNull final ReportState state, @Nullable ParsingResult parsingResult) {
    FileState fileState = myParsingResults.get(report);
    if (fileState == null) {
      fileState = new FileState(report.lastModified(), report.length());
      myParsingResults.put(report, fileState);
    }
    fileState.reportState = state;
    fileState.parsingResult = parsingResult;
  }

  public synchronized void setReportState(@NotNull final File report, @NotNull final ReportState state, final long lastModified, final long length) {
    FileState fileState = myParsingResults.get(report);
    if (fileState == null) {
      fileState = new FileState(lastModified, length);
      myParsingResults.put(report, fileState);
    }
    fileState.reportState = state;
    fileState.lastModified = lastModified;
    fileState.length = length;
  }


  @Nullable
  public synchronized ParsingResult getParsingResult(@NotNull File report) {
    final FileState state = myParsingResults.get(report);
    return state == null ? null : state.parsingResult;
  }

  @NotNull
  public synchronized Map<File, ParsingResult> getProcessedFiles() {
    final Map<File, ParsingResult> res = new HashMap<File, ParsingResult>();
    for (Map.Entry<File, FileState> e : myParsingResults.entrySet()) {
      final File key = e.getKey();
      final FileState value = e.getValue();

      if (value.reportState == ReportState.PROCESSED) {
        res.put(key, value.parsingResult);
      }
    }
    return res;
  }

  @NotNull
  public synchronized Map<File, ParsingResult> getFailedToProcessFiles() {
    final Map<File, ParsingResult> res = new HashMap<File, ParsingResult>();
    for (Map.Entry<File, FileState> e : myParsingResults.entrySet()) {
      final File key = e.getKey();
      final FileState value = e.getValue();

      if (value.reportState == ReportState.ERROR) {
        res.put(key, value.parsingResult);
      }
    }
    return res;
  }

  @NotNull
  public synchronized List<File> getOutOfDateFiles() {
    final List<File> res = new ArrayList<File>();
    for (Map.Entry<File, FileState> e : myParsingResults.entrySet()) {
      final File key = e.getKey();
      final FileState value = e.getValue();

      if (value.reportState == ReportState.OUT_OF_DATE) {
        res.add(key);
      }
    }
    return res;
  }

  private static final class FileState {
    @Nullable private Long lastModified;
    @Nullable private Long length;
    @Nullable private ParsingResult parsingResult;
    private ReportState reportState = ReportState.UNKNOWN;

    private FileState(long lastModified, long length) {
      this.lastModified = lastModified;
      this.length = length;
    }
  }
}
