/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import java.io.File;
import java.io.IOException;
import java.util.Stack;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.TestMessages;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class AntJUnitReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AntJUnitReportParser.class);

  @NotNull
  private final TestReporter myTestReporter;
  @NotNull
  private final DurationParser myDurationParser;

  private int myTestsToSkip;
  private int myLoggedTests;

  private int myLoggedSuites;

  @Nullable
  private ParsingException myParsingException;

  @NotNull
  private final Stack<String> mySuites = new Stack<String>();

  private final boolean myLogInternalSystemError;

  public AntJUnitReportParser(@NotNull TestReporter testReporter, @NotNull DurationParser durationParser, final boolean logInternalSystemError) {
    myTestReporter = testReporter;
    myDurationParser = durationParser;
    myLogInternalSystemError = logInternalSystemError;
  }

  @Override
  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (prevResult != null) {
      myTestsToSkip = ((TestParsingResult) prevResult).getTests();
    }
    try {
      new AntJUnitXmlReportParser(new AntJUnitXmlReportParser.Callback() {

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
        public void suiteFailureFound(@Nullable final String suiteName,
                                      @Nullable final String type,
                                      @Nullable final String message,
                                      @Nullable final String trace) {
          if (mySuites.isEmpty() || !mySuites.peek().equals(suiteName)) {
            LOG.warn(TestMessages.getFailedToLogSuiteMessage("failure", suiteName));
            return;
          }
          myTestReporter.error(TestMessages.getOutFromSuiteMessage("Failure", suiteName, type, message, trace));
        }

        @Override
        public void suiteErrorFound(@Nullable final String suiteName,
                                    @Nullable final String type,
                                    @Nullable final String message,
                                    @Nullable final String trace) {
          if (mySuites.isEmpty() || !mySuites.peek().equals(suiteName)) {
            LOG.warn(TestMessages.getFailedToLogSuiteMessage("error", suiteName));
            return;
          }
          myTestReporter.error(TestMessages.getOutFromSuiteMessage("Error", suiteName, type, message, trace));
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
        public void suiteSystemErrFound(@Nullable final String suiteName, @Nullable final String message) {
          if (mySuites.isEmpty() || !mySuites.peek().equals(suiteName)) {
            LOG.warn(TestMessages.getFailedToLogSuiteMessage("system err", suiteName));
            return;
          }
          if (message != null && message.length() > 0) {
            final String msg = TestMessages.getOutFromSuiteMessage("System error", suiteName, message);
            if (myLogInternalSystemError) {
              myTestReporter.info(msg);
            } else {
              myTestReporter.warning(msg);
            }
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

            final String testName = testData.getName();

            if (testName == null) {
              myTestReporter.warning(TestMessages.getFileContainsUnnamedMessage(file, "test"));
              return;
            }

            myTestReporter.openTest(testName);
            if (!testData.isExecuted()) myTestReporter.testIgnored("");
            if (testData.getStdOut() != null && testData.getStdOut().length() > 0) {
              myTestReporter.testStdOutput(testData.getStdOut());
            }
            if (testData.getStdErr() != null && testData.getStdErr().length() > 0) {
              if (myLogInternalSystemError) {
                myTestReporter.info(testData.getStdErr());
              } else {
                myTestReporter.testErrOutput(testData.getStdErr());
              }
            }
            if (testData.getFailureType() != null || testData.getFailureMessage() != null || testData.getFailureStackTrace() != null) {
              myTestReporter
                .testFail(TestMessages.getFailureMessage(testData.getFailureType(), testData.getFailureMessage()), testData.getFailureStackTrace());
            }
            myTestReporter.closeTest(testData.getDuration());
          } finally {
            ++myLoggedTests;
          }
        }

        @Override
        public void unexpectedFormat(@NotNull final String msg) {
          myTestReporter.error(TestMessages.getFileExpectedFormatMessage(file, msg, "Ant JUnit Task"));
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

  @Override
  public ParsingResult getParsingResult() {
    return new TestParsingResult(myLoggedSuites, (myLoggedTests > myTestsToSkip) ? myLoggedTests : myTestsToSkip, myParsingException);
  }


  private boolean testSkipped() {
    return myLoggedTests < myTestsToSkip;
  }

  /*  As of now the DTD is:

<!ELEMENT testsuites (testsuite*)>

<!ELEMENT testsuite (properties, testcase*,
                 failure?, error?,
                  system-out?, system-err?)>
<!ATTLIST testsuite name      CDATA #REQUIRED>
<!ATTLIST testsuite tests     CDATA #REQUIRED>
<!ATTLIST testsuite failures  CDATA #REQUIRED>
<!ATTLIST testsuite errors    CDATA #REQUIRED>
<!ATTLIST testsuite time      CDATA #REQUIRED>
<!ATTLIST testsuite package   CDATA #IMPLIED>
<!ATTLIST testsuite id        CDATA #IMPLIED>


<!ELEMENT properties (property*)>

<!ELEMENT property EMPTY>
<!ATTLIST property name  CDATA #REQUIRED>
<!ATTLIST property value CDATA #REQUIRED>

<!ELEMENT testcase (failure?, error?, system-out?, system-err?)>
<!ATTLIST testcase name       CDATA #REQUIRED>
<!ATTLIST testcase classname  CDATA #IMPLIED>
<!ATTLIST testcase time       CDATA #REQUIRED>

<!ELEMENT failure (#PCDATA)>
<!ATTLIST failure message CDATA #IMPLIED>
<!ATTLIST failure type    CDATA #REQUIRED>

<!ELEMENT error (#PCDATA)>
<!ATTLIST error message CDATA #IMPLIED>
<!ATTLIST error type    CDATA #REQUIRED>

<!ELEMENT system-err (#PCDATA)>

<!ELEMENT system-out (#PCDATA)> */
}
