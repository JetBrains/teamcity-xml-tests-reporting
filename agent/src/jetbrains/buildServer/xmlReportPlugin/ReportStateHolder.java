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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 19:28
 */

/**
 * Holds information about current state of reports
 */
public interface ReportStateHolder {

  public static enum ReportState {
    /*
    * UNKNOWN brand new report
    * ON_PROCESSING report was added, but not processed yet
    * PROCESSED report was successfully processed
    * ERROR report wasn't fully processed due to some problems
    * OUT_OF_DATE report is out-of-date
    */
    UNKNOWN, ON_PROCESSING, PROCESSED, ERROR, OUT_OF_DATE
  }

  @NotNull ReportState getReportState(@NotNull File report);
  @Nullable Long getLastModified(@NotNull File report);
  @Nullable Long getLength(@NotNull File report);
  void setReportState(@NotNull File report, @NotNull ReportState state, long lastModified, long length);
}
