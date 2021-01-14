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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmd;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class PmdComplexReportParserTest extends BaseParserTestCase {
  private static final String TYPE = "pmd";
  protected StringBuilder myNullLineResults;
  protected List<InspectionResult> myNullLineInspections;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();
    myNullLineResults = new StringBuilder();
    myNullLineInspections = new ArrayList<InspectionResult>();
  }

  @NotNull
  @Override
  protected Parser getParser() {
    return new PmdReportParser(getInspectionReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return TYPE;
  }

  @Override
  protected InspectionReporter getInspectionReporter() {
    return new InspectionReporter() {
      public void reportInspection(@NotNull final InspectionResult inspection) {
        myResult.append(inspection.toString()).append("\n");
        if (inspection.getLine() == 0) {
          myNullLineResults.append(inspection.toString()).append("\n");
          myNullLineInspections.add(inspection);
        }
      }

      public void reportInspectionType(@NotNull final InspectionTypeResult inspectionType) {
        myResult.append(inspectionType.toString()).append("\n");
      }

      public void info(@NotNull final String message) {
        myResult.append("MESSAGE: ").append(message).append("\n");
      }

      public void warning(@NotNull final String message) {
        myResult.append("WARNING: ").append(message).append("\n");
      }

      public void error(@NotNull final String message) {
        myResult.append("ERROR: ").append(message).append("\n");
      }

      @Override
      public void failure(@NotNull final String message) {
        myResult.append("PROBLEM: ").append(message).append("\n");
      }
    };
  }

  @Test
  @TestFor(issues = "TW-42260")
  public void testComplexParsingIsSuccessful() throws Exception {
    myNullLineResults = new StringBuilder();
    myNullLineInspections = new ArrayList<InspectionResult>();
    final ParsingResult first = parse("complex-1.xml");
    Assert.assertEquals(myNullLineInspections, new ArrayList<InspectionResult>(), "Inspections with incorrectly parsed line expected to be empty, actual " + myNullLineInspections);
  }

  @Test
  @TestFor(issues = "TW-42260")
  public void testComplexParsingIsSuccessful2() throws Exception {
    myNullLineResults = new StringBuilder();
    myNullLineInspections = new ArrayList<InspectionResult>();
    final ParsingResult first = parse("complex-2.xml");
    Assert.assertEquals(myNullLineInspections, new ArrayList<InspectionResult>(), "Inspections with incorrectly parsed line expected to be empty, actual " + myNullLineInspections);
  }

  private void runTest(final String reportName) throws Exception {
    parse(reportName);
    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }
}
