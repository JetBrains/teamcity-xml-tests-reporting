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

package jetbrains.buildServer.xmlReportPlugin.antJUnit;


final class TestData {
  private final String myClassName;
  private final String myTestName;

  private final long myStartTime;
  private long myDuration;

  private boolean myExecuted;

  private String myFailureType;
  private String myFailureMessage;
  private String myFailureStackTrace;

  public TestData(final String className,
                  final String testName,
                  boolean executed,
                  long startTime,
                  long duration) {
    myClassName = className;
    myTestName = testName;
    myExecuted = executed;
    myStartTime = startTime;
    myDuration = duration;
  }

  public String getClassName() {
    return myClassName;
  }

  public String getTestName() {
    return myTestName;
  }

  public void setExecuted(boolean executed) {
    myExecuted = executed;
  }

  public boolean isExecuted() {
    return myExecuted;
  }

  public long getStartTime() {
    return myStartTime;
  }

  public long getDuration() {
    return myDuration;
  }

  public void setDuration(long duration) {
    myDuration = duration;
  }

  public String getFailureMessage() {
    return myFailureMessage;
  }

  public void setFailureMessage(String message) {
    myFailureMessage = message;
  }

  public String getFailureType() {
    return myFailureType;
  }

  public void setFailureType(String type) {
    myFailureType = type;
  }

  public String getFailureStackTrace() {
    return myFailureStackTrace;
  }

  public void setFailureStackTrace(String stackTrace) {
    myFailureStackTrace = stackTrace;
  }

  public boolean isFailure() {
    return ((myFailureType != null) || (myFailureStackTrace != null));
  }
}