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

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 13:10
 */
public class InspectionResult {
  @Nullable
  private final String myFilePath, myInspectionId, myMessage;
  private final int myLine;
  private final int myPriority;

  public InspectionResult(@Nullable String filePath,
                          @Nullable String inspectionId,
                          @Nullable String message,
                          int line, int priority) {
    myFilePath = filePath;
    myInspectionId = inspectionId;
    myMessage = message;
    myLine = line;
    myPriority = priority;
  }

  @Nullable
  public String getFilePath() {
    return myFilePath;
  }

  @Nullable
  public String getInspectionId() {
    return myInspectionId;
  }

  @Nullable
  public String getMessage() {
    return myMessage;
  }

  public int getLine() {
    return myLine;
  }

  public int getPriority() {
    return myPriority;
  }

  @Override
  public String toString() {
    return "InspectionInstance{" +
           "myInspectionId='" + myInspectionId + '\'' +
           ", myMessage='" + myMessage + '\'' +
           ", myFilePath='" + myFilePath + '\'' +
           ", myLine=" + myLine +
           ", myPriority=" + myPriority +
           '}';
  }
}
