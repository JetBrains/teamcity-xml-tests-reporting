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

package jetbrains.buildServer.xmlReportPlugin;

import java.util.Date;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.BuildMessage1;
import org.jetbrains.annotations.NotNull;

class BuildLoggerForTesting implements BuildProgressLogger {
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
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void activityFinished(final String activityName, final String activityType) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void targetStarted(final String targetName) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void targetFinished(final String targetName) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void buildFailureDescription(final String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void preparationEndMessage() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void internalError(final String type, final String message, final Throwable throwable) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void progressStarted(final String message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void progressFinished() {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logMessage(final BuildMessage1 message) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestStarted(final String name) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestStarted(final String name, final Date timestamp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestFinished(final String name) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestFinished(final String name, final Date timestamp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestIgnored(final String name, final String reason) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logSuiteStarted(final String name) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logSuiteStarted(final String name, final Date timestamp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logSuiteFinished(final String name) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logSuiteFinished(final String name, final Date timestamp) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestStdOut(final String testName, final String out) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestStdErr(final String testName, final String out) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestFailed(final String testName, final Throwable e) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logComparisonFailure(final String testName, final Throwable e, final String expected, final String actual) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void logTestFailed(final String testName, final String message, final String stackTrace) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  
  public void flush() {
  }

  public void ignoreServiceMessages(final Runnable runnable) {
    //To change body of implemented methods use File | Settings | File Templates.
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

  public void flowStarted(final String flowId, final String parentFlowId) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  public void flowFinished(final String flowId) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
