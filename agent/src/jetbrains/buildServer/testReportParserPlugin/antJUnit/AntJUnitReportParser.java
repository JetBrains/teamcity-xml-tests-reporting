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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin.createLogMessage;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.Date;
import java.util.Stack;


public class AntJUnitReportParser extends DefaultHandler implements TestReportParser {
  private final static Logger LOG = Logger.getInstance(AntJUnitReportParser.class.getName());
  private static final String TEST_SUITE = "testsuite";
  private static final String TEST_CASE = "testcase";
  private static final String FAILURE = "failure";
  private static final String ERROR = "error";
  private static final String SYSTEM_OUT = "system-out";
  private static final String SYSTEM_ERR = "system-err";

  private static final String NAME_ATTR = "name";
  private static final String CLASSNAME_ATTR = "classname";
  private static final String MESSAGE_ATTR = "message";
  private static final String TYPE_ATTR = "type";
  private static final String TIME_ATTR = "time";

  private static final String DEFAULT_NAMESPACE = "";

  private final BaseServerLoggerFacade myLogger;
  private XMLReader myXMLReader;

  private SuiteData myCurrentSuite;
  private Stack<TestData> myTests;
  private String mySystemOut;
  private String mySystemErr;
  private StringBuffer myCData;

  private long myLoggedTests;
  private long myTestsToSkip;


  public AntJUnitReportParser(@NotNull final BaseServerLoggerFacade logger) {
    myLogger = logger;

    try {
      myXMLReader = XMLReaderFactory.createXMLReader();
      myXMLReader.setContentHandler(this);
      myXMLReader.setErrorHandler(this);
    } catch (SAXException e) {
      myLogger.warning(createLogMessage("Ant JUnit report parser couldn't get default XMLReader"));
    }
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

<!ELEMENT testcase (failure?, error?)>
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
      LOG.debug("Parser got report: " + report.getPath());
      myXMLReader.parse(new InputSource(report.toURI().toString()));

    } catch (SAXParseException e) {
      if (myTests != null) {
        myTests.clear();
      }

      LOG.debug(createLogMessage("Couldn't completely parse " + report.getPath() + " report - SAXParseException occured: " + e.toString()));

      return myLoggedTests;

    } catch (Exception e) {
      String message = "<no message>";
      if (e.getMessage() != null) {
        message = e.getMessage();
      }
      myLogger.warning(createLogMessage("An error occurred in Ant JUnit report parser: " + e + ": " + message));

      LOG.debug(createLogMessage("An error occurred in Ant JUnit report parser: " + e + ": " + message));
      StackTraceElement[] st = e.getStackTrace();
      for (int i = 0; i < st.length; ++i) {
        LOG.debug(st[i].toString());
      }

    }
    myCurrentSuite = null;
    return -1;
  }

  public boolean abnormalEnd() {
    if ((myCurrentSuite != null) && myCurrentSuite.getLogged()) {
      endSuite();
      LOG.debug("Abnormal end called. Log ending of started suite.");
      return true;
    }

    return false;
  }

  public void startElement(String uri, String localName,
                           String qName, Attributes attributes)
    throws SAXException {
    if (testSkipped()) {
      return;
    }

    if (TEST_SUITE.equals(localName)) {
      startSuite(attributes);
    } else if (TEST_CASE.equals(localName)) {
      startTest(attributes);
    } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
      startFailure(attributes);
    } else if (SYSTEM_OUT.equals(localName) || SYSTEM_ERR.equals(localName)) {
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
    }
  }

  private void startSuite(Attributes attributes) {
    if ((myCurrentSuite != null) && myCurrentSuite.getLogged()) {
      return;
    }

    final String name = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
    final Date startTime = new Date();
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

    myCurrentSuite = new SuiteData(name, startTime.getTime(), duration);
    myTests = new Stack<TestData>();
    myLogger.logSuiteStarted(name, startTime);
    myCurrentSuite.setLogged(true);
  }

  private void endSuite() {
    if (myCurrentSuite.isFailure()) {
      myLogger.error(myCurrentSuite.getFailureType() + ": " + myCurrentSuite.getFailureMessage());
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

    myCurrentSuite = null;
    myTests = null;
  }

  private void startTest(Attributes attributes) {
    final String className = attributes.getValue(DEFAULT_NAMESPACE, CLASSNAME_ATTR);
    final String testName = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
    final Date startTime = new Date();
    final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

    final TestData test = new TestData(className, testName, startTime.getTime(), duration);
    myTests.push(test);
  }

  private void endTest() {
    final TestData test = myTests.pop();
    final String testFullName = test.getClassName() + "." + test.getTestName();

    myLogger.logTestStarted(testFullName, new Date(test.getStartTime()));
    if (test.isFailure()) {
      myLogger.logTestFailed(testFullName, test.getFailureType() + ": " + test.getFailureMessage(), myCData.toString().trim());
      myCData = null;
    }
    myLogger.logTestFinished(testFullName, new Date(test.getStartTime() + test.getDuration()));
    myLoggedTests = myLoggedTests + 1;
  }

  private void startFailure(Attributes attributes) {
    final String failureMessage = attributes.getValue(DEFAULT_NAMESPACE, MESSAGE_ATTR);
    final String failureType = attributes.getValue(DEFAULT_NAMESPACE, TYPE_ATTR);

    if (myTests.size() != 0) {
      final TestData test = myTests.peek();
      test.setFailureMessage(failureMessage);
      test.setFailureType(failureType);
    } else {
      myCurrentSuite.setFailureMessage(failureMessage);
      myCurrentSuite.setFailureType(failureType);
    }
    myCData = new StringBuffer();
  }

  public void characters(char ch[], int start, int length) throws SAXException {
    if (testSkipped()) {
      return;
    }
    if (myCData != null) {
      myCData.append(ch, start, length);
    }
  }

//  private long getTestNumber(String testNumStr) {
//    if (testNumStr == null) {
//      return 0L;
//    }
//    try {
//      return (Long.parseLong(testNumStr));
//    } catch (NumberFormatException e) {
//      return 0L;
//    }
//  }

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

  private boolean testSkipped() {
    return (myLoggedTests < myTestsToSkip);
  }

  private String getTrimmedCData() {
    return myCData.toString().trim();
  }
}