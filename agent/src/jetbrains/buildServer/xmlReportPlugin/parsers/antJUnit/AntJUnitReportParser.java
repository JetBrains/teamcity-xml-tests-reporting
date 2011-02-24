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

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import java.io.IOException;
import jetbrains.buildServer.xmlReportPlugin.*;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class AntJUnitReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(AntJUnitReportParser.class);

  @NotNull
  private final TestReporter myTestReporter;

  private int myTestsToSkip;
  private int myLoggedTests;

  private int myLoggedSuites;

  @Nullable
  private String mySuite;

  public AntJUnitReportParser(@NotNull TestReporter testReporter) {
    myTestReporter = testReporter;
  }

  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (prevResult != null) {
      myTestsToSkip = ((TestParsingResult) prevResult).getTests();
    }
    try {
      new AntJUnitXmlReportParser(new AntJUnitXmlReportParser.Callback() {

        public void suiteFound(@Nullable final String suiteName) {
          if (mySuite != null) {
            LOG.error("Suite " + mySuite + " was not closed");
          }

          if (suiteName == null) {
            myTestReporter.warning("File " + file + " contains unnamed suite");
            return;
          }

          myTestReporter.openTestSuite(suiteName);
          ++myLoggedSuites;
          mySuite = suiteName;
        }

        public void suiteFailureFound(@Nullable final String suiteName,
                                      @Nullable final String type,
                                      @Nullable final String message,
                                      @Nullable final String trace) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite failure for not-opened suite " + suiteName);
            return;
          }
          myTestReporter.error("Failure from suite " + suiteName + ": " + getFailureMessage(type, message) + "\n" + trace);
        }

        public void suiteErrorFound(@Nullable final String suiteName,
                                    @Nullable final String type,
                                    @Nullable final String message,
                                    @Nullable final String trace) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite error for not-opened suite " + suiteName);
            return;
          }
          myTestReporter.error("Error from suite " + suiteName + ": " + getFailureMessage(type, message) + "\n" + trace);
        }

        public void suiteSystemOutFound(@Nullable final String suiteName, @Nullable final String message) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite system out for not-opened suite " + suiteName);
            return;
          }
          if (message != null && message.length() > 0) {
            myTestReporter.info("System out from suite " + suiteName + ": " + message);
          }
        }

        public void suiteSystemErrFound(@Nullable final String suiteName, @Nullable final String message) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite system err for not-opened suite " + suiteName);
            return;
          }
          if (message != null && message.length() > 0) {
            myTestReporter.warning("System error from suite " + suiteName + ": " + message);
          }
        }

        public void suiteFinished(@Nullable final String suiteName) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite finish for not-opened suite " + suiteName);
            return;
          }
          myTestReporter.closeTestSuite();
          mySuite = null;
        }

        @SuppressWarnings({"ConstantConditions"})
        public void testFound(@NotNull TestData testData) {
          try {
            if (testSkipped()) return;

            final String testName = testData.getName();

            if (testName == null) {
              myTestReporter.warning("File " + file + " contains unnamed test");
              return;
            }

            myTestReporter.openTest(testName);
            if (!testData.isExecuted()) myTestReporter.testIgnored("");
            if (testData.getFailureType() != null || testData.getFailureMessage() != null) {
              myTestReporter
                .testFail(getFailureMessage(testData.getFailureType(), testData.getFailureMessage()), testData.getFailureStackTrace());
            }
            //noinspection ConstantConditions
            if (testData.getStdErr() != null && testData.getStdErr().length() > 0) {
              myTestReporter.warning("System error from test " + testName + ": " + testData.getStdErr());
            }
            if (testData.getStdOut() != null && testData.getStdOut().length() > 0) {
              myTestReporter.info("System out from test " + testName + ": " + testData.getStdOut());
            }
            myTestReporter.closeTest(testData.getDuration());
          } finally {
            ++myLoggedTests;
          }
        }
      }).parse(file);
      return true;
    } catch (IOException e) {
      if (mySuite != null) myTestReporter.closeTestSuite();
      LOG.debug("Couldn't completely parse " + file
                + " report, exception occurred: " + e + ", " + myLoggedTests + " tests logged");
    }

    return false;
  }

  public ParsingResult getParsingResult() {
    return new TestParsingResult(myLoggedSuites, (myLoggedTests > myTestsToSkip) ? myLoggedTests : myTestsToSkip);
  }

  @NotNull
  private String getFailureMessage(@Nullable String type, @Nullable String message) {
    String failureMessage = "";
    if (type != null) {
      failureMessage = failureMessage.concat(type);
    }
    if (message != null) {
      if (failureMessage.length() > 0) {
        failureMessage = failureMessage.concat(": ");
      }
      failureMessage = failureMessage.concat(message);
    }
    return failureMessage;
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
