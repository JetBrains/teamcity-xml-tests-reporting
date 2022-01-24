/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Petrenko
*         Created: 05.02.2009 20:15:05
*/
class TestResult {
  private String myTestId;
  private String myDataRowInfo;
  private String myDuration;
  private String myError;
  private String myStacktrace;
  private String myOutcome;
  private String myStdOutput;
  private String myStdError;
  private String myStartTime;
  private String myEndTime;

  private final List<String> myTrace = new ArrayList<String>(1);
  private final VS_Version myVersion;
  private boolean myHasInnerResults;

  public TestResult(final VS_Version version) {
    myVersion = version;
  }

  public List<String> getTraces() {
    return myTrace;
  }

  public void addTrace(final String trace) {
    if (trace != null) {
      myTrace.add(trace);
    }
  }

  public boolean hasInnerResults() {
    return myHasInnerResults;
  }

  public void setHasInnerResults(final boolean hasInnerResults) {
    myHasInnerResults = hasInnerResults;
  }

  public void setDataRowInfo(final String dataRowInfo) {
    myDataRowInfo = dataRowInfo;
  }

  public String getEndTime() {
    return myEndTime;
  }

  public void setEndTime(final String endTime) {
    myEndTime = endTime;
  }

  public String getStartTime() {
    return myStartTime;
  }

  public void setStartTime(final String startTime) {
    myStartTime = startTime;
  }

  public TestName getTestName() {
    return myTestId == null ? null : new TestName(myTestId, myDataRowInfo);
  }

  public void setTestId(final String testId) {
    myTestId = testId;
  }

  public String getDuration() {
    return myDuration;
  }

  public void setDuration(final String duration) {
    myDuration = duration;
  }

  public String getError() {
    return myError;
  }

  public void setError(final String error) {
    myError = error;
  }

  public String getStacktrace() {
    return myStacktrace;
  }

  public void setStacktrace(final String stacktrace) {
    myStacktrace = stacktrace;
  }

  public String getOutcome() {
    return myOutcome;
  }

  public void setOutcome(final String outcome) {
    myOutcome = outcome;
  }

  public String getStdOutput() {
    return myStdOutput;
  }

  public void setStdOutput(final String stdOutput) {
    myStdOutput = stdOutput;
  }

  public String getStdError() {
    return myStdError;
  }

  public void setStdError(final String stdError) {
    myStdError = stdError;
  }

  public static enum VS_Version {VS_8, VS_9}


  public VS_Version getVersion() {
    return myVersion;
  }
}
