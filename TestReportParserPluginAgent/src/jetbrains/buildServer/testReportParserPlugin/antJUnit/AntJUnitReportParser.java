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

package jetbrains.buildServer.testReportParserPlugin.antJUnit;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.testReportParserPlugin.TestReportParser;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin.createBuildLogMessage;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.Date;
import java.util.Stack;


public class AntJUnitReportParser extends DefaultHandler implements TestReportParser {
    private static final String TEST_SUITE = "testsuite";
    private static final String TEST_CASE = "testcase";
    private static final String FAILURE = "failure";
    private static final String ERROR = "error";
    private static final String SYSTEM_OUT = "system-out";
    private static final String SYSTEM_ERR = "system-err";

    private static final String NAME_ATTR = "name";
    private static final String TESTS_ATTR = "tests";
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
            myLogger.warning(createBuildLogMessage("Ant JUnit report parser couldn't get default XMLReader"));
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
            myXMLReader.parse(new InputSource(report.toURI().toString()));
        } catch (SAXParseException e) {
            return myLoggedTests;
        } catch (Exception e) {
            myLogger.warning(createBuildLogMessage(e.getClass().getName() + " exception in Ant JUnit report parser."));
        }
        myCurrentSuite = null;
        return -1;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        if (testSkipped()) {
            return;
        }
        if (TEST_SUITE.equals(localName)) {
            suiteStarted(attributes);
        } else if (TEST_CASE.equals(localName)) {
            testStarted(attributes);
        } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
            failureStarted(attributes);
        } else if (SYSTEM_OUT.equals(localName) || SYSTEM_ERR.equals(localName)) {
            myCData = new StringBuffer();
        }
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        if (testSkipped()) {
            if (TEST_CASE.equals(localName)) {
                myLoggedTests = myLoggedTests + 1;
            }
            return;
        }
        if (TEST_SUITE.equals(localName)) {
            suiteFinished();
        } else if (TEST_CASE.equals(localName)) {
            testFinished();
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

    private void suiteStarted(Attributes attributes) {
        if ((myCurrentSuite != null) && myCurrentSuite.isLogged()) {
            return;
        }
        final String name = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
        final long testNumber = getTestNumber(attributes.getValue(DEFAULT_NAMESPACE, TESTS_ATTR));
        final Date startTime = new Date();
        final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

        myCurrentSuite = new SuiteData(name, testNumber, startTime.getTime(), duration);
        myTests = new Stack<TestData>();
        myLogger.logSuiteStarted(name, startTime);
        myCurrentSuite.logged(true);
    }

    private void suiteFinished() {
        myLogger.logSuiteFinished(myCurrentSuite.getName(), new Date(myCurrentSuite.getStartTime() + myCurrentSuite.getDuraion()));
        if (mySystemOut != null) {
            myLogger.message("System out from " + myCurrentSuite.getName() + ":\n" + mySystemOut);
            mySystemOut = null;
        }
        if (mySystemErr != null) {
            myLogger.warning("System error from " + myCurrentSuite.getName() + ":\n" + mySystemErr);
            mySystemErr = null;
        }
        myCurrentSuite = null;
        myTests = null;
    }

    private void testStarted(Attributes attributes) {
        final String className = attributes.getValue(DEFAULT_NAMESPACE, CLASSNAME_ATTR);
        final String testName = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
        final Date startTime = new Date();
        final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

        final TestData test = new TestData(className, testName, startTime.getTime(), duration);
        myTests.push(test);
    }

    private void testFinished() {
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

    private void failureStarted(Attributes attributes) {
        final TestData test = myTests.peek();

        final String failureMessage = attributes.getValue(DEFAULT_NAMESPACE, MESSAGE_ATTR);
        test.setFailureMessage(failureMessage);

        final String failureType = attributes.getValue(DEFAULT_NAMESPACE, TYPE_ATTR);
        test.setFailureType(failureType);

        myCData = new StringBuffer();
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
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

    private boolean testSkipped() {
        return (myLoggedTests < myTestsToSkip);
    }

    private String getTrimmedCData() {
        return myCData.toString().trim();
    }

    private static final class SuiteData {
        private final String myName;
        private final long myTestNumber;
        private final long myStartTime;
        private final long myDuration;
        private boolean myLogged;

        public SuiteData(final String name, long testNumber, long startTime, long duration) {
            myName = name;
            myTestNumber = testNumber;
            myStartTime = startTime;
            myDuration = duration;
            myLogged = false;
        }

        public String getName() {
            return myName;
        }

        public long getTestNumber() {
            return myTestNumber;
        }

        public long getStartTime() {
            return myStartTime;
        }

        public long getDuraion() {
            return myDuration;
        }

        public void logged(boolean logged) {
            myLogged = logged;
        }

        public boolean isLogged() {
            return myLogged;
        }
    }


    private static final class TestData {
        private final String myClassName;
        private final String myTestName;

        private final long myStartTime;
        private final long myDuration;

        private String myFailureType;
        private String myFailureMessage;

        public TestData(final String className,
                        final String testName,
                        final long startTime,
                        final long duration) {
            myClassName = className;
            myTestName = testName;
            myStartTime = startTime;
            myDuration = duration;
        }

        public void setFailureMessage(String message) {
            myFailureMessage = message;
        }

        public void setFailureType(String type) {
            myFailureType = type;
        }

        public String getClassName() {
            return myClassName;
        }

        public String getTestName() {
            return myTestName;
        }

        public long getStartTime() {
            return myStartTime;
        }

        public long getDuration() {
            return myDuration;
        }

        public boolean isFailure() {
            return (myFailureType != null);
        }

        public String getFailureType() {
            return myFailureType;
        }

        public String getFailureMessage() {
            return myFailureMessage;
        }
    }
}