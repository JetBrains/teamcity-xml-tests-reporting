/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 23.10.2008 22:47:48
 */
class TRXParser implements Parser {
  private static final Logger LOG = Logger.getLogger(TRXParser.class);

  private final TestNamesTableParser myNamesParser;
  private final TestResultsTableParser myResultsParser;

  private final Map<String,String> myTestIdToName = new HashMap<String,String>();

  @NotNull
  private final TestReporter myLogger;

  @NotNull
  private final String myDefaultSuiteName;

  @SuppressWarnings("FieldMayBeFinal") private int myReportedTestsCount = 0;

  public TRXParser(@NotNull final TestReporter logger, @NotNull final String defaultSuiteName) {
    myLogger = logger;
    myDefaultSuiteName = defaultSuiteName;
    myNamesParser = new TestNamesTableParser(new TestNamesTableParser.Callback() {
      public void testMethodFound(@NotNull final String id, @NotNull final String testName) {
        myTestIdToName.put(id, testName);
      }
    });

    myResultsParser = new TestResultsTableParser(new TestResultsTableParser.Callback() {
      private TestName myTestName;

      private String testName() {
        return testName(myTestName);
      }

      private String testName(final TestName testName) {
        return testName.presentName(myTestIdToName.get(testName.getTestId()));
      }

      public void testFound(@NotNull final TestName testId) {
        if (myTestName != null) {
          LOG.warn("Test " + myTestName + " was not closed");
        }
        myTestName = testId;
        logger.openTest(testName());
        myReportedTestsCount++;
      }

      public void testOutput(@NotNull final TestName testId, @NotNull final String text) {
        if (myTestName == null || !myTestName.equals(testId)) {
          LOG.warn("Failed to log testOutput for not-opened test");
          return;
        }

        logger.testStdOutput(text);
      }

      public void testError(@NotNull final TestName testId, @NotNull final String text) {
        if (myTestName == null || !myTestName.equals(testId)) {
          LOG.warn("Failed to log testError for not-opened test");
          return;
        }
        logger.testErrOutput(text);
      }

      public void testException(@NotNull final TestName testId, @Nullable final String message, @Nullable final String error) {
        if (myTestName == null || !myTestName.equals(testId)) {
          LOG.warn("Failed to log testException for not-opened test");
          return;
        }
        logger.testFail(message, error);
      }

      public void testIgnored(@NotNull final TestName testId, @Nullable final String message, @Nullable final String error) {
        if (myTestName == null || !myTestName.equals(testId)) {
          LOG.warn("Failed to log testException for not-opened test");
          return;
        }
        final String toLog = (message == null ? "" : message) + (error == null ? "" : " " + error);
        logger.testIgnored(toLog);
      }

      public void warning(@Nullable final TestName testId, @NotNull final String message) {
        String name = "<NA>";
        if (testId != null) {
          name = testName(testId);
        }
        logger.warning("Test '" + name + "': " + message);
      }

      public void warning(@Nullable final String message, @Nullable final String exception) {
        logger.error("Runner error: " + message + "\r\n" + exception);
      }

      public void testFinished(@NotNull final TestName testId, @NotNull final TestOutcome outcome, final long duration) {
        if (myTestName == null) {
          LOG.warn("Test " + myTestName + " was not opened");
        }
        logger.closeTest(duration);
        myTestName = null;
      }
    });
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    myLogger.openTestSuite(myDefaultSuiteName);

    if (!file.isFile() || file.length() == 0) {
      myLogger.error("File " + file + " does not exist. Please check if report producer process has failed.");
      return false;
    }

    myTestIdToName.clear();
    try {
      myNamesParser.parse(file);

      if (myTestIdToName.size() == 0) {
        myLogger.error("There were no test definitions found. Wrong or broken .trx file?");
      } else {
        myLogger.info("Found " + myTestIdToName.size() + " test definitions.");
      }

      myResultsParser.parse(file);
    } catch (IOException e) {
      throw new ParsingException(e);
    }

    if (myReportedTestsCount == 0) {
      myLogger.error("There were no tests reported. Wrong or broken .trx file?");
    } else {
      myLogger.info(myReportedTestsCount + " test(s) were reported");
    }

    myLogger.closeTestSuite();

    return true;
  }

  public ParsingResult getParsingResult() {
    return new TestParsingResult(1, myReportedTestsCount);
  }
}
