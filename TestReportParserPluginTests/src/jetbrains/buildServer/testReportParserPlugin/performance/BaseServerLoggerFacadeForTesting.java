/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

package jetbrains.buildServer.testReportParserPlugin.performance;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.messages.BuildMessage1;

public class BaseServerLoggerFacadeForTesting extends BaseServerLoggerFacade {
    public BaseServerLoggerFacadeForTesting() {
        super();
    }

    public void flush() {
    }

    public void log(BuildMessage1 buildMessage1) {
    }

    public void logTestStarted(java.lang.String s) {
    }

    public void logTestStarted(java.lang.String s, java.util.Date date) {
    }

    public void logTestFinished(java.lang.String s) {
    }

    public void logTestFinished(java.lang.String s, java.util.Date date) {
    }

    public void logTestIgnored(java.lang.String s, java.lang.String s1) {
    }

    public void logSuiteStarted(java.lang.String s) {
    }

    public void logSuiteStarted(java.lang.String s, java.util.Date date) {
    }

    public void logSuiteFinished(java.lang.String s) {
    }

    public void logSuiteFinished(java.lang.String s, java.util.Date date) {
    }

    public void logTestStdOut(java.lang.String s, java.lang.String s1) {
    }

    public void logTestStdErr(java.lang.String s, java.lang.String s1) {
    }

    public void logComparisonFailure(java.lang.String s, java.lang.Throwable throwable, java.lang.String s1, java.lang.String s2) {
    }

    public void logTestFailed(java.lang.String s, java.lang.Throwable throwable) {
    }

    public void logTestFailed(java.lang.String s, java.lang.String s1, java.lang.String s2) {
    }

    public void activityStarted(java.lang.String s, java.lang.String s1) {
    }

    public void activityFinished(java.lang.String s, java.lang.String s1) {
    }

    public void targetStarted(java.lang.String s) {
    }

    public void targetFinished(java.lang.String s) {
    }

    public void progressStarted(java.lang.String s) {
    }

    public void progressFinished() {
    }

    public void message(java.lang.String s) {
    }

    public void progressMessage(java.lang.String s) {
    }

    public void error(java.lang.String s) {
    }

    public void buildFailureDescription(java.lang.String s) {
    }

    public void warning(java.lang.String s) {
    }

    public void exception(java.lang.Throwable throwable) {
    }

    public void flowStarted(java.lang.String s, java.lang.String s1) {
    }

    public void flowFinished(java.lang.String s) {
    }

    public void ensureFlow() {
    }

    public void buildFinished() {
    }

    public void logMessage(jetbrains.buildServer.messages.BuildMessage1 buildMessage1) {
    }
}