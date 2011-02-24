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

package jetbrains.buildServer.xmlReportPlugin.nUnit;

import java.io.IOException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserUtils;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.tests.TestResultsWriter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestsParsingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.XMLReader;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;


public class NUnitReportParser implements Parser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(NUnitReportParser.class);

  @NotNull
  private final TestResultsWriter myTestResultsWriter;

  private int myTestsToSkip;
  private int myLoggedTests;

  private int myLoggedSuites;

  private String mySuite;

  public NUnitReportParser(@NotNull TestResultsWriter testResultsWriter) {
    myTestResultsWriter = testResultsWriter;
  }

  public boolean parse(@NotNull final File file, @Nullable final ParsingResult prevResult) throws ParsingException {
    if (prevResult != null) {
      myTestsToSkip = ((TestsParsingResult) prevResult).getTests();
    }
    try {
      new NUnitXmlReportParser(new NUnitXmlReportParser.Callback() {
        public void suiteFound(@Nullable final String suiteName) {
          if (mySuite != null) {
            LOG.error("Suite " + mySuite + " was not closed");
          }

          if (suiteName == null) {
            myTestResultsWriter.warning("File " + file + " contains unnamed suite");
            return;
          }

          myTestResultsWriter.openTestSuite(suiteName);
          ++myLoggedSuites;
          mySuite = suiteName;
        }

        public void suiteFailureFound(@Nullable final String suiteName, @Nullable final String message, @Nullable final String trace) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite failure for not-opened suite " + suiteName);
            return;
          }
          myTestResultsWriter.error("Failure from suite " + suiteName + ": " + (message == null ? "" : message)  + "\n" + trace);
        }

        public void suiteFinished(@Nullable final String suiteName) {
          if (mySuite == null || !mySuite.equals(suiteName)) {
            LOG.error("Failed to log suite finish for not-opened suite " + suiteName);
            return;
          }
          myTestResultsWriter.closeTestSuite();
          mySuite = null;
        }

        public void testFound(@NotNull final TestData testData) {
          try {
            if (testSkipped()) return;

            final String testName = testData.getName();

            if (testName == null) {
              myTestResultsWriter.warning("File " + file + " contains unnamed test");
              return;
            }

            myTestResultsWriter.openTest(testName);
            if (!testData.isExecuted()) myTestResultsWriter.testIgnored("");
            if (testData.getFailureMessage() != null) {
              myTestResultsWriter
                .testFail(testData.getFailureMessage(), testData.getFailureStackTrace());
            }
            myTestResultsWriter.closeTest(testData.getDuration());
          } finally {
            ++myLoggedTests;
          }
        }
    }).parse(file);
      return true;
    } catch (IOException e) {
      if (mySuite != null) myTestResultsWriter.closeTestSuite();
      LOG.debug("Couldn't completely parse " + file
                + " report, exception occurred: " + e + ", " + myLoggedTests + " tests logged");
    }

    return false;
  }

  public ParsingResult getParsingResult() {
    return new TestsParsingResult(myLoggedSuites, myLoggedTests);
  }

  private boolean testSkipped() {
    return myLoggedTests < myTestsToSkip;
  }

  //private static final String TMP_REPORT_DIRECTORY = "junit_reports";
  //
  //@NotNull
  //private final String myNUnitSchemaPath;
  //
  //@NotNull
  //private final File myTempDirectory;
  //
  //public NUnitReportParser(@NotNull TestResultsWriter testResultsWriter,
  //                         @NotNull String NUnitSchemaPath,
  //                         @NotNull File tempDirectory) {
  //  super(testResultsWriter);
  //  myNUnitSchemaPath = NUnitSchemaPath;
  //  myTempDirectory = tempDirectory;
  //}
  //
  //@Override
  //public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
  //  if (!ParserUtils.isReportComplete(file, "test-results")) {
  //    return false;
  //  }
  //  final NUnitToJUnitReportTransformer reportTransformer;
  //  try {
  //    reportTransformer = new NUnitToJUnitReportTransformer(myNUnitSchemaPath);
  //  } catch (TransformerConfigurationException e) {
  //    throw new ParsingException(e);
  //  }
  //
  //  final File jUnitReport = getJUnitReport(file.getName());
  //  try {
  //    reportTransformer.transform(file, jUnitReport);
  //  } catch (Exception e) {
  //    FileUtil.delete(jUnitReport);
  //    throw new ParsingException(e);
  //  }
  //  return super.parse(jUnitReport, null);
  //}
  //
  //@NotNull
  //private File getJUnitReport(@NotNull String fileName) {
  //  final File tempReportDir = new File(myTempDirectory, TMP_REPORT_DIRECTORY);
  //  //noinspection ResultOfMethodCallIgnored
  //  tempReportDir.mkdirs();
  //  return new File(tempReportDir, fileName);
  //}
}