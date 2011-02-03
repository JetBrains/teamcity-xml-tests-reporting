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

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:33
 */
public class TestsParsingResult implements ParsingResult {
  private int mySuites;
  private int myTests;

  public TestsParsingResult(int suites, int tests) {
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
    final TestsParsingResult testsParsingResult = (TestsParsingResult) parsingResult;
    mySuites += testsParsingResult.getSuites();
    myTests += testsParsingResult.getTests();
  }

  @NotNull
  public static TestsParsingResult createEmptyResult() {
    return new TestsParsingResult(0, 0);
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