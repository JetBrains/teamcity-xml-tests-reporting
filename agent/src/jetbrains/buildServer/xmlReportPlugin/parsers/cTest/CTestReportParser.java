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

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestParsingResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author Vladislav.Rassokhin
 */
public class CTestReportParser implements Parser {
    public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(CTestReportParser.class);

    public static final String TESTS_REPORT_FILE_NAME = "Test.xml";

    @NotNull
    private final TestReporter myTestReporter;
    @NotNull
    private final File myBaseDir;

    private int myLoggedTests;

    @Nullable
    private ParsingException myParsingException;
    private static final String ROOT_SUITE = "";
    private static final String DEFAULT_SUITE = ROOT_SUITE;
    //  private Stack<String> mySuites = new Stack<String>();
    private String myLastSuite;

    public CTestReportParser(@NotNull final TestReporter testReporter, @NotNull final File baseDir) {
        myTestReporter = testReporter;
        myBaseDir = baseDir;
    }

    public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
        try {
            XmlXppAbstractParser suitableParser = null;

            // Determine report type & create parser
            if (file.getName().equalsIgnoreCase(TESTS_REPORT_FILE_NAME)) {
                suitableParser = createTestXmlParser(file);
            }
            // TODO: support for Coverage.xml & others

            if (suitableParser != null) {
                suitableParser.parse(file);
            }
            return true;
        } catch (IOException e) {
            myParsingException = new ParsingException(e);
            LOG.debug("Couldn't completely parse " + file
                    + " report, exception occurred: " + e + ", " + myLoggedTests + " tests logged");
        }

        return false;
    }

    private TestXmlReportParser createTestXmlParser(final File file) {
        return new TestXmlReportParser(new TestXmlReportParser.Callback() {
            public void testFound(@NotNull final TestData testData) {
                try {
                    final String testName = testData.getName();
                    final TestData.Status status = testData.getStatus();
                    // Currently I cannot know what to say about log. is it from StdErr or StdOut.
                    // Assuming as StdErr if build failed and StdOut otherwise.
                    final String log = testData.getLog();

                    String suite = getSuiteFromFile(file);
                    checkOpenSuite(suite);

                    if (testName == null) {
                        myTestReporter.warning("File " + file + " contains unnamed test");
                        return;
                    }

                    myTestReporter.openTest(testName);
                    switch (status) {
                        case NOT_RUN:
                            myTestReporter.testIgnored("");
                            break;
                        case COMPLETED:
                            if (log != null && !StringUtil.isEmptyOrSpaces(log)) {
                                myTestReporter.testStdOutput(log);
                            }
                            break;
                        case FAILED:
                            if (log != null && !StringUtil.isEmptyOrSpaces(log)) {
                                myTestReporter.testErrOutput(log);
                            }
                            myTestReporter.testFail(testData.getFailureMessage(), null);
                            break;
                    }
                    myTestReporter.closeTest(testData.getDuration());

                } finally {
                    ++myLoggedTests;
                }
            }
        });
    }

    private void checkOpenSuite(@NotNull String suite) {
        if (!suite.equals(myLastSuite)) {
            closeSuite();
            myTestReporter.openTestSuite(suite);
            myLastSuite = suite;
        }
    }

    private void closeSuite() {
        if (myLastSuite != null) {
            myLastSuite = null;
            myTestReporter.closeTestSuite();
        }
    }

    private String getSuiteFromFile(File file) {
        // removing "Testing/DATE_STAMP/" from suite name
        File suiteDir;

        File dateDir = file.getParentFile();
        if (dateDir.getName().matches("[0-9]{8}-[0-9]{4}")) {
            File testingDir = dateDir.getParentFile();
            if (testingDir.getName().equalsIgnoreCase("Testing")) {
                suiteDir = testingDir.getParentFile();
            } else {
                suiteDir = testingDir;
            }
        } else {
            suiteDir = dateDir;
        }

        if (myBaseDir.equals(suiteDir)) {
            return ROOT_SUITE;
        }

        String relativePath = FileUtil.getRelativePath(myBaseDir, suiteDir);
        if (relativePath == null) {
            try {
                return suiteDir.getAbsolutePath();
            } catch (SecurityException e) {
                return DEFAULT_SUITE;
            }
        } else {
            return relativePath;
        }
    }

    public ParsingResult getParsingResult() {
        closeAllSuites();
        return new TestParsingResult(1, myLoggedTests, myParsingException);
    }

    private void closeAllSuites() {
        closeSuite();
    }
}
