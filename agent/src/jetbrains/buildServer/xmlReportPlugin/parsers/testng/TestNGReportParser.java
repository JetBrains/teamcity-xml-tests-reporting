/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.testng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.TestMessages;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestNGReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(TestNGReportParser.class);

  @NotNull
  private final TestReporter myTestReporter;
  @NotNull
  private final DurationParser myDurationParser;
  private final boolean myLogInternalSystemError;
  @NotNull
  private final Deque<String> mySuites = new ArrayDeque<String>();
  private int myTestsToSkip;
  private int myLoggedTests;
  private int myLoggedSuites;
  @Nullable
  private ParsingException myParsingException;

  public TestNGReportParser(@NotNull final TestReporter testReporter, @NotNull final DurationParser durationParser, final boolean logInternalSystemError) {
    myTestReporter = testReporter;
    myDurationParser = durationParser;
    myLogInternalSystemError = logInternalSystemError;
  }


  @Override
  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (prevResult != null) {
      myTestsToSkip = ((TestParsingResult)prevResult).getTests();
    }

    try {
      new TestNGXmlReportParser(new TestNGXmlReportParser.Callback() {

        @Override
        public void suiteFound(@Nullable final String suiteName) {
          if (suiteName == null) {
            myTestReporter.warning(TestMessages.getFileContainsUnnamedMessage(file, "suite"));
            return;
          }

          myTestReporter.openTestSuite(suiteName);
          ++myLoggedSuites;
          mySuites.push(suiteName);
        }

        @Override
        public void suiteSystemOutFound(@Nullable final String suiteName, @Nullable final String message) {
          if (mySuites.isEmpty() || !mySuites.peek().equals(suiteName)) {
            LOG.warn(TestMessages.getFailedToLogSuiteMessage("system out", suiteName));
            return;
          }
          if (message != null && message.length() > 0) {
            myTestReporter.info(TestMessages.getOutFromSuiteMessage("System out", suiteName, message));
          }
        }

        @Override
        public void suiteFinished(@Nullable final String suiteName) {
          if (mySuites.isEmpty() || !mySuites.peek().equals(suiteName)) {
            LOG.warn(TestMessages.getFailedToLogSuiteMessage("finish", suiteName));
            return;
          }
          myTestReporter.closeTestSuite();
          mySuites.pop();
        }

        @Override
        public void testFound(@NotNull TestData testData) {
          try {
            if (testSkipped()) return;
            String methodNameWithClass = (testData.getClassName() == null || testData.getMethodName() != null && testData.getMethodName().startsWith(testData.getClassName())
                                          ? ""
                                          : testData.getClassName() + ".") + testData.getMethodName();
            String methodParams = "";
            if (testData.getParams().size() > 0) {
              methodParams = "(" + StringUtil.join(testData.getParams(), ", ") + ")";
            }

            final String testName = methodNameWithClass + methodParams;
            myTestReporter.openTest(testName);

            switch (testData.getStatus()) {
              case PASS:
                break;
              case FAIL:
                if (testData.getFailureType() != null || testData.getFailureMessage() != null || testData.getFailureStackTrace() != null) {
                  myTestReporter
                    .testFail(TestMessages.getFailureMessage(testData.getFailureType(), testData.getFailureMessage()), testData.getFailureStackTrace());
                }
                break;
              case SKIP:
                String message = testData.getFailureMessage();
                myTestReporter.testIgnored(message == null ? "" : message);
                break;
            }

            String msg = testData.getTestMessage();
            if (msg.length() > 0) {
              myTestReporter.testStdOutput(msg);
            }

            myTestReporter.closeTest(testData.getDuration());
          } finally {
            ++myLoggedTests;
          }
        }

        @Override
        public void unexpectedFormat(@NotNull final String msg) {
          myTestReporter.error(TestMessages.getFileExpectedFormatMessage(file, msg, "Ant TestNG Task"));
        }
      }, myDurationParser).parse(file);
      return true;
    } catch (IOException e) {
      myParsingException = new ParsingException(e);

      while (!mySuites.isEmpty()) {
        myTestReporter.closeTestSuite();
        mySuites.pop();
      }

      LOG.debug(TestMessages.getCouldNotCompletelyParseMessage(file, e, myLoggedTests));
    }
    return false;
  }

  @Nullable
  @Override
  public ParsingResult getParsingResult() {
    return new TestParsingResult(myLoggedSuites, (myLoggedTests > myTestsToSkip) ? myLoggedTests : myTestsToSkip, myParsingException);
  }

  private boolean testSkipped() {
    return myLoggedTests < myTestsToSkip;
  }

}
