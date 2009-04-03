/*
 * Copyright 2000-2009 JetBrains s.r.o.
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


public final class ReportData {
  private final File myFile;
  private int myProcessedEvents;
  private String myType;

  public ReportData(@NotNull final File file, String type) {
    myFile = file;
    myProcessedEvents = 0;
    myType = type;
  }

  public File getFile() {
    return myFile;
  }

  public int getProcessedEvents() {
    return myProcessedEvents;
  }

  public void setProcessedEvents(int tests) {
    myProcessedEvents = tests;
  }

  public String getType() {
    return myType;
  }
}
