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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.AgentRunningBuild;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Date;
import java.util.Map;


public class TestReportParsingParameters {
  private boolean myParsingEnabled = false;
  private boolean myVerboseOutput = false;
  private boolean myParseOutOfDateFiles = false;
  private String myReportType = "";
  private long myBuildStartTime = 0L;
  private File myRunnerWorkingDir;
  private File myTmpDir;

  public TestReportParsingParameters(@NotNull AgentRunningBuild build) {
    myBuildStartTime = new Date().getTime();
    myRunnerWorkingDir = build.getWorkingDirectory();
    myTmpDir = build.getBuildTempDirectory();

    final Map<String, String> runnerParameters = build.getRunnerParameters();

    myParsingEnabled = TestReportParserPluginUtil.isTestReportParsingEnabled(runnerParameters);
    myVerboseOutput = TestReportParserPluginUtil.isOutputVerbose(runnerParameters);
    myReportType = TestReportParserPluginUtil.getReportType(runnerParameters);
    myParseOutOfDateFiles = TestReportParserPluginUtil.shouldParseOutOfDateReports(build.getBuildParameters().getSystemProperties());
  }

  public boolean isParsingEnabled() {
    return myParsingEnabled;
  }

  public void setParsingEnabled(boolean testReportParsingEnabled) {
    myParsingEnabled = testReportParsingEnabled;
  }

  public boolean isVerboseOutput() {
    return myVerboseOutput;
  }

  public void setVerboseOutput(boolean verboseOutput) {
    myVerboseOutput = verboseOutput;
  }

  public boolean isParseOutOfDateFiles() {
    return myParseOutOfDateFiles;
  }

  public void setParseOutOfDateFiles(boolean parseOutOfDateFiles) {
    myParseOutOfDateFiles = parseOutOfDateFiles;
  }

  public String getReportType() {
    return myReportType;
  }

  public void setReportType(String reportType) {
    myReportType = reportType;
  }

  public long getBuildStartTime() {
    return myBuildStartTime;
  }

  public void setBuildStartTime(long buildStartTime) {
    myBuildStartTime = buildStartTime;
  }

  public File getRunnerWorkingDir() {
    return myRunnerWorkingDir;
  }

  public void setRunnerWorkingDir(File runnerWorkingDir) {
    myRunnerWorkingDir = runnerWorkingDir;
  }

  public File getTmpDir() {
    return myTmpDir;
  }

  public void setTmpDir(File tmpDir) {
    myTmpDir = tmpDir;
  }
}
