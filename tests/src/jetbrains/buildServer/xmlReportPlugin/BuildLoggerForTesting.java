/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import java.util.Date;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.impl.BuildMessageTweaker;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.messages.BuildMessage1;
import org.jetbrains.annotations.NotNull;

class BuildLoggerForTesting implements FlowLogger, MessageTweakingSupport {
  private final StringBuilder myText;

  public BuildLoggerForTesting(final StringBuilder text) {
    myText = text;
  }

  
  public void warning(@NotNull final String message) {
    myText.append("WARNING: ");
    myText.append(message);
    myText.append("\n");
  }

  
  public void exception(final Throwable th) {
    myText.append("EXCEPTION: ");
    myText.append(th.toString());
    myText.append("\n");
    th.printStackTrace();
  }

  public void activityStarted(final String activityName, final String activityType) {

  }

  public void activityStarted(final String activityName, final String activityDescription, final String activityType) {
  }

  public void activityFinished(final String activityName, final String activityType) {

  }

  public void targetStarted(final String targetName) {

  }

  public void targetFinished(final String targetName) {

  }

  public void buildFailureDescription(final String message) {

  }

  public void internalError(final String type, final String message, final Throwable throwable) {

  }

  public void progressStarted(final String message) {

  }

  public void progressFinished() {

  }

  public void logMessage(final BuildMessage1 message) {

  }

  public void logTestStarted(final String name) {
    myText.append("TEST STARTED: ");
    myText.append(name);
    myText.append("\n");
  }

  public void logTestStarted(final String name, final Date timestamp) {
    myText.append("TEST STARTED: ");
    myText.append(name);
    myText.append(" ");
    myText.append("##TIMESTAMP##");
    myText.append("\n");
  }

  public void logTestFinished(final String name) {
    myText.append("TEST FINISHED: ");
    myText.append(name);
    myText.append("\n");
  }

  public void logTestFinished(final String name, final Date timestamp) {
    myText.append("TEST FINISHED: ");
    myText.append(name);
    myText.append(" ");
    myText.append("##TIMESTAMP##");
    myText.append("\n");
  }

  public void logTestIgnored(final String name, final String reason) {
    myText.append("TEST IGNORED: ");
    myText.append(name);
    if (reason != null && reason.length() > 0) {
      myText.append(" ");
      myText.append(reason);
    }
    myText.append("\n");
  }

  public void logSuiteStarted(final String name) {
    myText.append("SUITE STARTED: ");
    myText.append(name);
    myText.append("\n");
  }

  public void logSuiteStarted(final String name, final Date timestamp) {
    myText.append("SUITE STARTED: ");
    myText.append(name);
    myText.append(" ");
    myText.append("##TIMESTAMP##");
    myText.append("\n");
  }

  public void logSuiteFinished(final String name) {
    myText.append("SUITE FINISHED: ");
    myText.append(name);
    myText.append("\n");
  }

  public void logSuiteFinished(final String name, final Date timestamp) {
    myText.append("SUITE FINISHED: ");
    myText.append(name);
    myText.append(" ");
    myText.append("##TIMESTAMP##");
    myText.append("\n");
  }

  public void logTestStdOut(final String testName, final String out) {
    myText.append("TEST STDOUT: ");
    myText.append(testName);
    myText.append("\n");
    myText.append(out);
    myText.append("\n");
  }

  public void logTestStdErr(final String testName, final String out) {
    myText.append("TEST STDERR: ");
    myText.append(testName);
    myText.append("\n");
    myText.append(out);
    myText.append("\n");
  }

  public void logTestFailed(final String testName, final Throwable e) {
    myText.append("TEST FAILED: ");
    myText.append(testName);
    myText.append("\n");
    myText.append(e);
    myText.append("\n");
  }

  public void logComparisonFailure(final String testName, final Throwable e, final String expected, final String actual) {
    myText.append("TEST FAILED: ");
    myText.append(testName);
    myText.append("\n");
    myText.append(e);
    myText.append("\n");
    myText.append("EXPECTED: ");
    myText.append(expected);
    myText.append("\n");
    myText.append("ACTUAL: ");
    myText.append(actual);
    myText.append("\n");
  }

  public void logTestFailed(final String testName, final String message, final String stackTrace) {
    myText.append("TEST FAILED: ");
    myText.append(testName);
    myText.append("\n");
    myText.append(message);
    myText.append("\n");
    myText.append(stackTrace);
    myText.append("\n");
  }

  
  public void flush() {
  }

  public void ignoreServiceMessages(final Runnable runnable) {

  }

  public FlowLogger getFlowLogger(final String flowId) {
    return this;
  }

  public FlowLogger getThreadLogger() {
    return this;
  }

  public void startFlow() {
  }

  public String getFlowId() {
    return "flow-id-2";
  }

  public void disposeFlow() {
  }


  protected void log(BuildMessage1 buildMessage1) {
  }

  
  public void progressMessage(final String message) {
    myText.append("PROGRESS: ");
    myText.append(message);
    myText.append("\n");
  }

  
  public void message(final String message) {
    myText.append("MESSAGE: ");
    myText.append(message);
    myText.append("\n");
  }

  
  public void error(@NotNull final String message) {
    myText.append("ERROR: ");
    myText.append(message);
    myText.append("\n");
  }

  public StringBuilder getText() {
    return myText;
  }

  public BuildProgressLogger getTweakedLogger(final BuildMessageTweaker tweaker) {
    return this;
  }

  public void logBuildProblem(final BuildProblemData buildProblem) {
    myText.append("BUILD PROBLEM: ");
    myText.append(buildProblem.toString());
    myText.append("\n");
  }
}
