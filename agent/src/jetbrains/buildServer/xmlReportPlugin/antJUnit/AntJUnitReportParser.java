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

package jetbrains.buildServer.xmlReportPlugin.antJUnit;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.Date;
import java.util.Stack;

public class AntJUnitReportParser extends XmlReportParser {
  private static final String COMMA = ",";
  private static final String DOT = ".";
  private static final String MARK = "'";
  private static final String NBSP = "\u00A0";

  private static final String TEST_SUITE = "testsuite";
  private static final String TEST_CASE = "testcase";
  private static final String FAILURE = "failure";
  private static final String ERROR = "error";
  private static final String TIME = "time";
  private static final String SYSTEM_OUT = "system-out";
  private static final String SYSTEM_ERR = "system-err";
  private static final String SKIPPED = "skipped";

  private static final String NAME_ATTR = "name";
  private static final String CLASSNAME_ATTR = "classname";
  private static final String PACKAGE_ATTR = "package";
  private static final String TESTS_ATTR = "tests";
  private static final String MESSAGE_ATTR = "message";
  private static final String TYPE_ATTR = "type";
  private static final String TIME_ATTR = "time";
  private static final String TIMESTAMP_ATTR = "timestamp";
  private static final String EXECUTED_ATTR = "executed";

  private static final String DEFAULT_NAMESPACE = "";

  private SuiteData myCurrentSuite;

  private final Stack<TestData> myTests = new Stack<TestData>();

  private String mySystemOut;
  private String mySystemErr;

  private int myLoggedSuites;
  private int myLoggedTests;
  private int myTestsToSkip;

  @NotNull
  private final BuildProgressLogger myLogger;

  public AntJUnitReportParser(@NotNull XMLReader reader,
                              @NotNull BuildProgressLogger logger) {
    super(reader, true);
    myLogger = logger;
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

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) {
    if (prevResult != null) {
      myTestsToSkip = ((TestsParsingResult) prevResult).getTests();
    }
    try {
      parse(file);
      return true;
    } catch (ParsingException e) {
      finishOnException(file, e);
    }

    return false;
  }

  private void finishOnException(@NotNull File file, @NotNull Exception e) {
    endSuite();

    LoggingUtils.LOG.debug("Couldn't completely parse " + file.getPath()
      + " report, exception occurred: " + e + ", " + myLoggedTests + " events logged");
  }

  public ParsingResult getParsingResult() {
    return new TestsParsingResult(myLoggedSuites,
      (myLoggedTests > myTestsToSkip) ? myLoggedTests : myTestsToSkip);
  }

  //  Handler methods

  @Override
  public void startElement(String uri, final String localName,
                           String qName, final Attributes attributes)
    throws SAXException {
    if (TEST_SUITE.equals(localName)) {
      startSuite(attributes);
    } else if (!testSkipped()) {
      if (TEST_CASE.equals(localName)) {
        startTest(attributes);
      } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
        startFailure(attributes);
      } else if (SKIPPED.equals(localName)) {
        if (insideTest()) {
          myTests.peek().setExecuted(false);
        }
      }
    }
  }

  @Override
  public void endElement(String uri, final String localName, String qName) throws SAXException {
    try {
      if (testSkipped()) {
        if (TEST_CASE.equals(localName)) {
          myLoggedTests = myLoggedTests + 1;
        }
        return;
      }
      if (TEST_SUITE.equals(localName)) {
        endSuite();
      } else if (TEST_CASE.equals(localName)) {
        endTest();
      } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
        endFailure();
      } else if (SYSTEM_OUT.equals(localName)) {
        final String trimmedCData = getCData().toString().trim();
        if (trimmedCData.length() > 0) {
          mySystemOut = trimmedCData;
        }
      } else if (SYSTEM_ERR.equals(localName)) {
        final String trimmedCData = getCData().toString().trim();
        if (trimmedCData.length() > 0) {
          mySystemErr = trimmedCData;
        }
      } else if (TIME.equals(localName)) {
        if (insideTest()) {
          myTests.peek().setDuration(getExecutionTime(ParserUtils.formatText(getCData())));
        }
      }
    } finally {
      clearCData();
    }
  }

  // Auxiliary methods

  private void startSuite(Attributes attributes) {
    if (myCurrentSuite != null) return;

    String name = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
    final String pack = attributes.getValue(DEFAULT_NAMESPACE, PACKAGE_ATTR);
    final Date startTime = new Date();
    final String timestamp = getTimestamp(attributes.getValue(DEFAULT_NAMESPACE, TIMESTAMP_ATTR));
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

    if ((pack != null) && (!name.startsWith(pack))) {
      name = pack + "." + name;
    }

    myCurrentSuite = new SuiteData(name, startTime.getTime(), duration, timestamp);
    myLogger.logSuiteStarted(name, startTime);
  }

  private void endSuite() {
    if (myCurrentSuite == null) return;

    if (myCurrentSuite.isFailure()) {
      myLogger.error(getFailureMessage(myCurrentSuite.getFailureType(), myCurrentSuite.getFailureMessage()));
    }

    if (mySystemOut != null) {
      myLogger.message("[System out]\n" + mySystemOut);
      mySystemOut = null;
    }

    if (mySystemErr != null) {
      myLogger.warning("[System error]\n" + mySystemErr);
      mySystemErr = null;
    }

    myLogger.logSuiteFinished(myCurrentSuite.getName(), new Date(myCurrentSuite.getStartTime() + myCurrentSuite.getDuraion()));
    myLoggedSuites = myLoggedSuites + 1;

    if (insideTest()) {
//      myFlowLogger.debugToAgentLog("Some tests were not logged in suite " + myCurrentSuite);
      myTests.clear();
    }
    myCurrentSuite = null;
  }

  private void startTest(Attributes attributes) {
    final String reportClassName = attributes.getValue(DEFAULT_NAMESPACE, CLASSNAME_ATTR);

    final String testName = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);

    final String reportedTestName;

    if (reportClassName != null && !reportClassName.equals(testName)) {
      reportedTestName = reportClassName + "." + testName;
    } else {
      reportedTestName = testName;
    }

    final Date startTime = new Date();
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));
    final boolean executed = getBoolean(attributes.getValue(DEFAULT_NAMESPACE, EXECUTED_ATTR));

    final TestData test = new TestData(reportedTestName, executed, startTime.getTime(), duration);
    myTests.push(test);
  }

  private void endTest() {
    final TestData test = myTests.pop();
    final String testName = test.getName();

    myLogger.logTestStarted(testName, new Date(test.getStartTime()));
    if (!test.isExecuted()) {
      myLogger.logTestIgnored(testName, "");
    } else {
      if (test.isFailure()) {
        String failureMessage = getFailureMessage(test.getFailureType(), test.getFailureMessage());
        myLogger.logTestFailed(testName, failureMessage, test.getFailureStackTrace());
      }
      if (mySystemOut != null) {
        myLogger.logTestStdOut(testName, mySystemOut);
        mySystemOut = null;
      }
      if (mySystemErr != null) {
        myLogger.logTestStdErr(testName, mySystemErr);
        mySystemErr = null;
      }
    }
    myLogger.logTestFinished(testName, new Date(test.getStartTime() + test.getDuration()));
    myLoggedTests = myLoggedTests + 1;
  }

  private String getFailureMessage(String type, String message) {
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

  private void startFailure(Attributes attributes) {
    String failureMessage = attributes.getValue(DEFAULT_NAMESPACE, MESSAGE_ATTR);
    if (failureMessage != null)
      failureMessage = failureMessage.trim();

    final String failureType = attributes.getValue(DEFAULT_NAMESPACE, TYPE_ATTR);

    if (insideTest()) {
      final TestData test = myTests.peek();
      test.setFailureMessage(failureMessage);
      test.setFailureType(failureType);
    } else if (myCurrentSuite != null) {
      myCurrentSuite.setFailureMessage(failureMessage);
      myCurrentSuite.setFailureType(failureType);
    }
  }

  private void endFailure() {
    if (insideTest()) {
      myTests.peek().setFailureStackTrace(getCData().toString().trim());
    }
  }

  private boolean testSkipped() {
    return (myLoggedTests < myTestsToSkip);
  }

  private boolean insideTest() {
    return !myTests.isEmpty();
  }

  private static long getExecutionTime(@Nullable String timeStr) {
    if (timeStr == null || "".equals(timeStr)) {
      return 0L;
    }
    try {
      return (long) (Double.parseDouble(getUniformTimeStr(timeStr)) * 1000.0);
    } catch (NumberFormatException e) {
      LoggingUtils.LOG.warn("Unable to parse execution time string " + timeStr, e);
      return 0L;
    }
  }

  private static String getUniformTimeStr(@NotNull String str) {
    final int commaIndex = str.lastIndexOf(COMMA);
    final int dotIndex = str.lastIndexOf(DOT);
    String result;
    if (commaIndex > dotIndex) {
      result = str.replace(DOT, "").replace(COMMA, DOT);
    } else if (commaIndex < dotIndex) {
      result = str.replace(COMMA, "");
    } else {
      result = str;
    }
    return result.replace(MARK, "").replace(NBSP, "");
  }

  private static String getTimestamp(@Nullable String timestampStr) {
    if (timestampStr == null) {
      return "";
    }
    return timestampStr;
  }

  private static boolean getBoolean(@Nullable String str) {
    return (str == null) || Boolean.parseBoolean(str);
  }
}
