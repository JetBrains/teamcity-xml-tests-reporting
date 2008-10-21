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
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin;
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
    private StringBuffer myCurrentStackTrace;
    private long myLoggedTests;
    private long myTestsToSkip;


    private static final class SuiteData {
        private final String myName;
        private final long myTestNumber;
        private final long myStartTime;
        private final long myDuration;

        public SuiteData(final String name, long testNumber, long startTime, long duration) {
            myName = name;
            myTestNumber = testNumber;
            myStartTime = startTime;
            myDuration = duration;
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

        public String getFailureType() {
            return myFailureType;
        }

        public String getFailureMessage() {
            return myFailureMessage;
        }
    }


    public AntJUnitReportParser(@NotNull final BaseServerLoggerFacade logger) {
        myLogger = logger;

        try {
            myXMLReader = XMLReaderFactory.createXMLReader();
            myXMLReader.setContentHandler(this);
            myXMLReader.setErrorHandler(this);
        } catch (SAXException e) {
            myLogger.warning("AntJUnitReportParser couldn't get default XMLReader");
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
        TestReportParserPlugin.log("Parser works with FILE: " + report.getName() + " from TEST: " + (testsToSkip + 1));
        myLoggedTests = 0;
        myTestsToSkip = testsToSkip;
        try {
            myXMLReader.parse(new InputSource(report.toURI().toString()));
        } catch (SAXParseException e) {
            return myLoggedTests;
        } catch (Exception e) {
            myLogger.warning("Exception in AntJUnitReportParser: " + e.getClass().getName());
            TestReportParserPlugin.log("ERROR: Exc " + e.getClass());
            StackTraceElement[] s = e.getStackTrace();
            for (int i = 0; i < s.length; ++i) {
                TestReportParserPlugin.log(s[i].getClassName() + ": " + s[i].getLineNumber());
            }
        }
        return -1;
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        if (testSkipped()) {
            return;
        }
        if (TEST_SUITE.equals(localName)) {
            final String name = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
            final long testNumber = Long.parseLong(attributes.getValue(DEFAULT_NAMESPACE, TESTS_ATTR));//catch NumberFormatEx
            final Date startTime = new Date();
            final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

            myCurrentSuite = new SuiteData(name, testNumber, startTime.getTime(), duration);
            myTests = new Stack<TestData>();
            myLogger.logSuiteStarted(name, startTime);
        } else if (TEST_CASE.equals(localName)) {
            final String className = attributes.getValue(DEFAULT_NAMESPACE, CLASSNAME_ATTR);
            final String testName = attributes.getValue(DEFAULT_NAMESPACE, NAME_ATTR);
            final Date startTime = new Date();
            final long duration = getExecutionTime(attributes.getValue(DEFAULT_NAMESPACE, TIME_ATTR));

            final TestData test = new TestData(className, testName, startTime.getTime(), duration);
            myTests.push(test);
            myLogger.logTestStarted(className + "." + testName, startTime);
        } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
            final TestData test = myTests.peek();

            final String failureMessage = attributes.getValue(DEFAULT_NAMESPACE, MESSAGE_ATTR);
            test.setFailureMessage(failureMessage);

            final String failureType = attributes.getValue(DEFAULT_NAMESPACE, TYPE_ATTR);
            test.setFailureType(failureType);

            myCurrentStackTrace = new StringBuffer();
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
            myLogger.logSuiteFinished(myCurrentSuite.getName(), new Date(myCurrentSuite.getStartTime() + myCurrentSuite.getDuraion()));
            myCurrentSuite = null;
            myTests = null;
        } else if (TEST_CASE.equals(localName)) {
            final TestData test = myTests.pop();

            myLoggedTests = myLoggedTests + 1;
            myLogger.logTestFinished(test.getClassName() + "." + test.getTestName(), new Date(test.getStartTime() + test.getDuration()));
        } else if (FAILURE.equals(localName) || ERROR.equals(localName)) {
            final TestData test = myTests.peek();
            final String fullName = test.getClassName() + "." + test.getTestName();

            myLogger.logTestFailed(fullName, test.getFailureType() + ": " + test.getFailureMessage(), myCurrentStackTrace.toString().trim());
            myCurrentStackTrace = null;
        }
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
//TODO: uncomment
//        if (testSkipped()) {
//            return;
//        }
        if (myCurrentStackTrace != null) {
            myCurrentStackTrace.append(ch, start, length);
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
}