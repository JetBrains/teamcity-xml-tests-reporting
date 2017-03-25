/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.tests;

import java.io.File;
import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.ProblemParsingResult;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:33
 */
public class TestParsingResult extends ProblemParsingResult {
  private int mySuites;
  private int myTests;

  public TestParsingResult(int suites, int tests) {
    this(suites, tests, null);
  }

  public TestParsingResult(int suites, int tests, @Nullable Throwable problem) {
    super(problem);
    mySuites = suites;
    myTests = tests;
  }

  public int getSuites() {
    return mySuites;
  }

  public int getTests() {
    return myTests;
  }

  public void accumulate(@NotNull ParsingResult parsingResult) {
    final TestParsingResult testParsingResult = (TestParsingResult) parsingResult;
    mySuites += testParsingResult.getSuites();
    myTests += testParsingResult.getTests();
  }

  @NotNull
  public static TestParsingResult createEmptyResult() {
    return new TestParsingResult(0, 0);
  }

  public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {
    final StringBuilder message = new StringBuilder(file.getAbsolutePath()).append(" report processed: ");

    message.append(mySuites).append(" suite").append(getEnding(mySuites));
    if (myTests > 0) {
      message.append(", ").append(myTests).append(" test").append(getEnding(myTests));
    }

    if (parameters.isVerbose()) {
      parameters.getThreadLogger().message(message.toString());
    }

    LoggingUtils.LOG.debug(message.toString());
  }

  @NotNull
  private static String getEnding(int number) {
    return (number == 1 ? "" : "s");
  }

  public void logAsTotalResult(@NotNull ParseParameters parameters) {}
}