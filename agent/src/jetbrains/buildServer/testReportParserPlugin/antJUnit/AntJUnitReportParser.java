/*
 * Copyright 2008 JetBrains s.r.o.
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

package jetbrains.buildServer.testReportParserPlugin.antJUnit;

import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.*;


public class AntJUnitReportParser extends DefaultHandler implements TestReportParser {
  public static final String TYPE = "junit";

  private static final String TEST_SUITE = "testsuite";
  private static final String TEST_CASE = "testcase";
  private static final String FAILURE = "failure";
  private static final String ERROR = "error";
  private static final String TIME = "time";
  private static final String SYSTEM_OUT = "system-out";
  private static final String SYSTEM_ERR = "system-err";

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

  private final TestReportLogger myLogger;
  private XMLReader myXMLReader;

  private SuiteData myCurrentSuite;
  private Stack<TestData> myTests;
  private String mySystemOut;
  private String mySystemErr;
  private StringBuffer myCData;

  private long myLoggedSuites;
  private long mySkippedSuites;
  private long myLoggedTests;
  private long myTestsToSkip;

  private Set<String> myPreviouslyLoggedSuits;


  public AntJUnitReportParser(@NotNull final TestReportLogger logger) {
    myLogger = logger;

    try {
      myXMLReader = XMLReaderFactory.createXMLReader();
      myXMLReader.setContentHandler(this);
      myXMLReader.setErrorHandler(this);
      myXMLReader.setFeature("http://xml.org/sax/features/validation", false);
    } catch (SAXException e) {
      myLogger.warning("Ant JUnit report parser couldn't get default XMLReader");
    }

    myPreviouslyLoggedSuits = new HashSet<String>();
    myLoggedSuites = 0;
    mySkippedSuites = 0;
  }

  public static boolean isReportFileComplete(@NotNull final File report) {
    return (report.length() > 0);
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

  public long parse(@NotNull final File report, long testsToSkip) {
    myLoggedTests = 0;
    myTestsToSkip = testsToSkip;

    try {
      myXMLReader.parse(new InputSource(report.toURI().toString()));
    } catch (SAXParseException e) {
      if (myTests != null) {
        myTests.clear();
      }

      myLogger.debugToAgentLog("Couldn't completely parse " + report.getPath() + " report - SAXParseException occured: " + e.toString());

      return myLoggedTests;

    } catch (Exception e) {
      myLogger.exception(e);
    }
    myCurrentSuite = null;
    myLoggedSuites = 0;
    mySkippedSuites = 0;
    return -1;
  }

  public void logReportTotals(File report, Map<String, String> params) {
    String message = report.getPath() + " report processed";
    if (myLoggedSuites != 0) {
      message = message.concat(": " + myLoggedSuites + " suite(s)");
      if (myLoggedTests != 0) {
        message = message.concat(", " + myLoggedTests + " test(s)");
      } else {
        myLogger.debugToAgentLog(report.getPath() + " contains no suits, but " + myLoggedTests + " tests");
      }
    } else if (mySkippedSuites != 0) {
      message = message.concat(", " + mySkippedSuites + " suite(s) skipped");
    }
    myLogger.message(message);
  }

  public boolean abnormalEnd() {
    if (myCurrentSuite != null) {
      endSuite();
      myLogger.debugToAgentLog("Abnormal end called. Log ending of started suite.");
      return true;
    }

    return false;
  }

  public TestReportLogger getLogger() {
    return myLogger;
  }

  public void startElement(String uri, String localName,
                           String qName, Attributes attributes)
    throws SAXException {
    if (testSkipped()) {
      return;
    } else if (TEST_SUITE.equals(localName)) {
      startSuite(attributes);
    } else if (TEST_CASE.equals(localName)) {
      startTest(attributes);
    } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
      startFailure(attributes);
    } else if (SYSTEM_OUT.equals(localName) || SYSTEM_ERR.equals(localName)) {
      myCData = new StringBuffer();
    } else if (TIME.equals(localName)) {
      myCData = new StringBuffer();
    }
  }

  public void endElement(String uri, String localName, String qName) throws SAXException {
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
    } else if (FAILURE.equals(localName)) {
      endFailure();
    } else if (SYSTEM_OUT.equals(localName)) {
      final String trimmedCData = getTrimmedCData();
      if (trimmedCData.length() > 0) {
        mySystemOut = trimmedCData;
      }
      myCData = null;
    } else if (SYSTEM_ERR.equals(localName)) {
      final String trimmedCData = getTrimmedCData();
      if (trimmedCData.length() > 0) {
        mySystemErr = trimmedCData;
      }
      myCData = null;
    } else if (TIME.equals(localName)) {
      if (myTests.size() != 0) {
        final TestData test = myTests.peek();
        test.setDuration(getExecutionTime(getTrimmedCData()));
      }
      myCData = null;
    }
  }

  private void startSuite(Attributes attributes) {
    if (myCurrentSuite != null) {
      return;
    }

    String name = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
    final String pack = attributes.getValue(DEFAULT_NAMESPACE, PACKAGE_ATTR);
    final long testNumber = getTestNumber(attributes.getValue(DEFAULT_NAMESPACE, TESTS_ATTR));
    final Date startTime = new Date();
    final String timestamp = getTimetamp(attributes.getValue(DEFAULT_NAMESPACE, TIMESTAMP_ATTR));
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

    if ((pack != null) && (!name.startsWith(pack))) {
      name = pack + "." + name;
    }

    if (myPreviouslyLoggedSuits.contains(name + timestamp)) {
      myLogger.debugToAgentLog(name + " suite has been already logged from other report");
      mySkippedSuites = mySkippedSuites + 1;
      myTestsToSkip = myLoggedTests + testNumber;
      return;
    }

    myCurrentSuite = new SuiteData(name, startTime.getTime(), duration);
    myTests = new Stack<TestData>();
    myLogger.getBuildLogger().logSuiteStarted(name, startTime);
    myPreviouslyLoggedSuits.add(name + timestamp);
  }

  private void endSuite() {
    if (myCurrentSuite == null) {
      return;
    }

    if (myCurrentSuite.isFailure()) {
      myLogger.error(myCurrentSuite.getFailureType() + ": " + myCurrentSuite.getFailureMessage());
    }
    if (mySystemOut != null) {
      myLogger.logSystemOut(mySystemOut);
      mySystemOut = null;
    }
    if (mySystemErr != null) {
      myLogger.logSystemError(mySystemErr);
      mySystemErr = null;
    }
    myLogger.getBuildLogger().logSuiteFinished(myCurrentSuite.getName(), new Date(myCurrentSuite.getStartTime() + myCurrentSuite.getDuraion()));
    myLoggedSuites = myLoggedSuites + 1;

    myCurrentSuite = null;
    myTests = null;
  }

  private void startTest(Attributes attributes) {
    String className = "<unknown name>";
    final String reportClassName = attributes.getValue(DEFAULT_NAMESPACE, CLASSNAME_ATTR);
    if (reportClassName != null) {
      className = reportClassName;
    } else if (myCurrentSuite != null) {
      className = myCurrentSuite.getName();
    }
    final String testName = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
    final Date startTime = new Date();
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));
    final boolean executed = getBoolean(attributes.getValue(DEFAULT_NAMESPACE, EXECUTED_ATTR));

    final TestData test = new TestData(className, testName, executed, startTime.getTime(), duration);
    myTests.push(test);
  }

  private void endTest() {
    final TestData test = myTests.pop();
    final String testFullName = test.getClassName() + "." + test.getTestName();

    if (!test.isExecuted()) {
      myLogger.getBuildLogger().logTestIgnored(testFullName, "");
    } else {
      myLogger.getBuildLogger().logTestStarted(testFullName, new Date(test.getStartTime()));

      if (test.isFailure()) {
        String failureMessage = "";
        if (test.getFailureType() != null) {
          failureMessage = failureMessage.concat(test.getFailureType());
          if (test.getFailureMessage() != null) {
            failureMessage = failureMessage.concat(": " + test.getFailureMessage());
          }
        }

        myLogger.getBuildLogger().logTestFailed(testFullName, failureMessage, test.getFailureStackTrace());
      }
      if (mySystemOut != null) {
        myLogger.getBuildLogger().logTestStdOut(testFullName, mySystemOut);
        mySystemOut = null;
      }
      if (mySystemErr != null) {
        myLogger.getBuildLogger().logTestStdErr(testFullName, mySystemErr);
        mySystemErr = null;
      }
      myLogger.getBuildLogger().logTestFinished(testFullName, new Date(test.getStartTime() + test.getDuration()));
    }
//    myLogger.getBuildLogger().flush();
    myLoggedTests = myLoggedTests + 1;
  }

  private void startFailure(Attributes attributes) {
    final String failureMessage = attributes.getValue(DEFAULT_NAMESPACE, MESSAGE_ATTR);
    final String failureType = attributes.getValue(DEFAULT_NAMESPACE, TYPE_ATTR);

    if (myTests.size() != 0) {
      final TestData test = myTests.peek();
      test.setFailureMessage(failureMessage);
      test.setFailureType(failureType);
    } else if (myCurrentSuite != null) {
      myCurrentSuite.setFailureMessage(failureMessage);
      myCurrentSuite.setFailureType(failureType);
    }
    myCData = new StringBuffer();
  }

  private void endFailure() {
    if (myTests.size() != 0) {
      final TestData test = myTests.peek();
      test.setFailureStackTrace(getTrimmedCData());
    }
    myCData = null;
  }

  public void characters(char ch[], int start, int length) throws SAXException {
    if (testSkipped()) {
      return;
    }
    if (myCData != null) {
      myCData.append(ch, start, length);
    }
  }

  private long getTestNumber(String testNumStr) {
    if (testNumStr == null) {
      return 0L;
    }
    try {
      return (Long.parseLong(testNumStr));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private long getExecutionTime(String timeStr) {
    if (timeStr == null) {
      return 0L;
    }
    try {
      return (long) (Double.parseDouble(timeStr) * 1000.0);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private String getTimetamp(String timestampStr) {
    if (timestampStr == null) {
      return "";
    }
    return timestampStr;
  }

  private boolean getBoolean(String str) {
    if (str == null) {
      return true;
    }
    return Boolean.parseBoolean(str);
  }

  private boolean testSkipped() {
    return (myLoggedTests < myTestsToSkip);
  }

  private String getTrimmedCData() {
    return myCData.toString().trim();
  }
}