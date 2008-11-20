/*
 * Copyright 2008 JetBrains s.r.o.
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
package jetbrains.buildServer.testReportParserPlugin.antJUnit;


public class SuiteData {
  private final String myName;
  private final long myTestNumber;
  private final long myStartTime;
  private final long myDuration;
  private boolean myLogged;

  private String myFailureType;
  private String myFailureMessage;

  public SuiteData(final String name, long testNumber, long startTime, long duration) {
    myName = name;
    myTestNumber = testNumber;
    myStartTime = startTime;
    myDuration = duration;
    myLogged = false;
  }

  public String getName() {
    return myName;
  }

  public long getTestNumber() {
    return myTestNumber;
  }

  public long getStartTime() {
    return myStartTime;
  }

  public long getDuraion() {
    return myDuration;
  }

  public void logged(boolean logged) {
    myLogged = logged;
  }

  public boolean isLogged() {
    return myLogged;
  }

  public void setFailureMessage(String message) {
    myFailureMessage = message;
  }

  public void setFailureType(String type) {
    myFailureType = type;
  }

  public String getFailureType() {
    return myFailureType;
  }

  public String getFailureMessage() {
    return myFailureMessage;
  }

  public boolean isFailure() {
    return (myFailureType != null);
  }
}
