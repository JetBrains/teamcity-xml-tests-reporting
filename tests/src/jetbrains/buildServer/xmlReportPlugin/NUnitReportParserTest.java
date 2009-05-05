/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.util.Date;


@RunWith(JMock.class)
public class NUnitReportParserTest extends TestCase {
  private static final String REPORT_DIR = "nunit";
  private static final String SUITE_NAME = "TestCase";
  private static final String CASE_CLASSNAME = "TestCase";
  private static final String CASE_NAME = CASE_CLASSNAME + ".test";

  private XmlReportParser myParser;
  private BaseServerLoggerFacade myLogger;

  private Mockery myContext;
  private Sequence mySequence;

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private ReportData report(String fileName) throws FileNotFoundException {
    return new ReportData(TestUtil.getTestDataFile(fileName, REPORT_DIR), "nunit");
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    myLogger = createBaseServerLoggerFacade();
    myParser = new NUnitReportParser(myLogger, "workingDirForTesting");
    mySequence = myContext.sequence("Log Sequence");
  }

  @Test
  public void testEmptyReport() throws Exception {
    final ReportData data = report("empty.xml");
    myParser.parse(data);
    final int testsLogged = data.getProcessedEvents();
    Assert.assertTrue("Empty report contains 0 tests, but " + testsLogged + " tests logged", testsLogged == 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testSingleCaseSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseFailure() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(any(String.class)), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseFailure.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("twoCasesSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFirstSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(any(String.class)), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("twoCasesFirstSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesSecondSuccess() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(any(String.class)), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("twoCasesSecondSuccess.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test2CasesFailed() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "1"), with(any(String.class)), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "1"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME + "2"), with(any(String.class)), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME + "2"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("twoCasesFailed.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIgnored() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with(CASE_NAME), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseIgnored.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseIgnoredFailed() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with(CASE_NAME), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseIgnoredFailure.xml"));
    myContext.assertIsSatisfied();
  }

  //TW-7573: XML Report plugin not reporting correct results for NUnit results
  @Test
  public void testNegativePassedTestNumberObserved() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_A_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_A_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_B_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_B_For_Basic_Testing"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_B_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_C_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_C_For_Basic_Testing"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Trade_C_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Update_A_Trade_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Update_A_Trade_For_Basic_Testing"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Update_A_Trade_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Delete_A_Trade_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Delete_A_Trade_For_Basic_Testing"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Delete_A_Trade_For_Basic_Testing"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_Staff_Member_And_Try_To_Delete_The_Trade"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_Staff_Member_And_Try_To_Delete_The_Trade"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_Staff_Member_And_Try_To_Delete_The_Trade"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_SectionStaff_Member_And_Try_To_Make_The_Trade_Inactive"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_SectionStaff_Member_And_Try_To_Make_The_Trade_Inactive"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_A_Trade_Link_It_To_A_SectionStaff_Member_And_Try_To_Make_The_Trade_Inactive"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Two_Trades_With_The_Same_Code_To_Display_The_Message_Cannot_Insert_Duplicate"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Two_Trades_With_The_Same_Code_To_Display_The_Message_Cannot_Insert_Duplicate"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_Two_Trades_With_The_Same_Code_To_Display_The_Message_Cannot_Insert_Duplicate"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_New_Trade_With_No_Value_In_Rates"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_New_Trade_With_No_Value_In_Rates"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests.Insert_New_Trade_With_No_Value_In_Rates"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.TradesCrudTests"), with(any(Date.class)));
        inSequence(mySequence);

        oneOf(myLogger).logSuiteStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Load_Existing_Staff_Member_To_Check_Edit_Grid_Functionality_And_Cancel_Changes"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Load_Existing_Staff_Member_To_Check_Edit_Grid_Functionality_And_Cancel_Changes"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Load_Existing_Staff_Member_To_Check_Edit_Grid_Functionality_And_Cancel_Changes"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Insert_And_Validate_New_Staff_Member"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Insert_And_Validate_New_Staff_Member"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Insert_And_Validate_New_Staff_Member"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Delete_New_Staff_Member"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestIgnored(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Delete_New_Staff_Member"), with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Delete_New_Staff_Member"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Click_The_Navigation_Buttons_On_The_Staff_Members_Browse_Screen"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests.Click_The_Navigation_Buttons_On_The_Staff_Members_Browse_Screen"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("Pragma.OnKey.Tests.OCL.Functional.Staff.StaffMembersDemoTests"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("Pragma.OnKey5.Tests.OCL.dll.TestResult.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseFailureWithMultiline() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with(CASE_NAME), with(any(String.class)), with("Assertion message form testjunit.framework.AssertionFailedError: Assertion message form test\n" +
          "            at TestCase.test(Unknown Source)\n" +
          "            at TestCase1.test(Unknown Source)\n" +
          "            at TestCase2.test(Unknown Source)"));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with(CASE_NAME), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with(SUITE_NAME), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("singleCaseFailureWithMultiline.xml"));
    myContext.assertIsSatisfied();
  }

  @Test
  public void test1CaseFailureinManyLines() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("MCNTest"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("MCNTest.Test1 - REMADV_9903214000009_4038777000004_20090421_4.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with("MCNTest.Test1 - REMADV_9903214000009_4038777000004_20090421_4.txt"), with(any(String.class)), with("Input and Output of test case are not equal. See .\\testCases\\EDI\\Invalid\\Input:\n" +
          "UNA:+.? '\n" +
          "UNB+UNOC:3+9903214000009:500+4038777000004:500+090421:1134+00000002154600'\n" +
          "UNH+00000002154600+REMADV:D:06A:UN:2.2'\n" +
          "BGM+239+DH0005631+9'\n" +
          "DTM+137:20090401:102'\n" +
          "FII+PB+984362uzt:Werthelsmann+90874:25:131::::Landesbank'\n" +
          "NAD+MS+9903214000009::293'\n" +
          "NAD+MR+4038777000004::293'\n" +
          "CUX+2:EUR:11'\n" +
          "DOC+380+DH7965542'\n" +
          "MOA+9:50'\n" +
          "MOA+12:0'\n" +
          "DTM+137:20090101:102'\n" +
          "RFF+IT:dh675543'\n" +
          "AJT+9'\n" +
          "FTX+ABO+1++Falscher Abrechnungszeitraum Grund 1:'\n" +
          "DOC+380+DH5437867'\n" +
          "MOA+9:50'\n" +
          "MOA+12:0'\n" +
          "DTM+137:20090101:102'\n" +
          "RFF+IT:dh650987'\n" +
          "AJT+Z06'\n" +
          "FTX+ABO+1++Artikel unbekannt Grund 1'\n" +
          "UNS+S'\n" +
          "MOA+9:100'\n" +
          "MOA+12:0'\n" +
          "UNT+25+00000002154600'\n" +
          "UNZ+1+00000002154600'\n" +
          "\n" +
          "\n" +
          "Output:\n" +
          "UNA:+.? '\n" +
          "UNB+UNOC:3+9903214000009:500+4038777000004:500+090421:1134+00000002154600'\n" +
          "UNH+00000002154600+REMADV:D:06A:UN:2.2'\n" +
          "BGM+239+DH0005631+9'\n" +
          "DTM+137:20090401:102'\n" +
          "FII+PB+984362uzt:Werthelsmann+90874:25:131::::Landesbank'\n" +
          "NAD+MS+9903214000009::293'\n" +
          "NAD+MR+4038777000004::293'\n" +
          "CUX+2:EUR:11'\n" +
          "DOC+380+DH7965542'\n" +
          "MOA+9:50'\n" +
          "MOA+12:0'\n" +
          "DTM+137:20090101:102'\n" +
          "RFF+IT:dh675543'\n" +
          "DOC+380+DH5437867'\n" +
          "MOA+9:50'\n" +
          "MOA+12:0'\n" +
          "DTM+137:20090101:102'\n" +
          "RFF+IT:dh650987'\n" +
          "UNS+S'\n" +
          "MOA+9:100'\n" +
          "MOA+12:0'\n" +
          "UNT+21+00000002154600'\n" +
          "UNZ+1+00000002154600'"));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("MCNTest.Test1 - REMADV_9903214000009_4038777000004_20090421_4.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("MCNTest.Test2 - REMADV_9903214000009_9907027000008_20090417_1.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("MCNTest.Test2 - REMADV_9903214000009_9907027000008_20090417_1.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestStarted(with("MCNTest.Test3 - UTILMD_9907027000008_4029684000003_20090427_1.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logTestFailed(with("MCNTest.Test3 - UTILMD_9907027000008_4029684000003_20090427_1.txt"), with(any(String.class)), with("Could not export testcase"));
        inSequence(mySequence);
        oneOf(myLogger).logTestFinished(with("MCNTest.Test3 - UTILMD_9907027000008_4029684000003_20090427_1.txt"), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("MCNTest"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("TestResults.xml"));
    myContext.assertIsSatisfied();
  }

  //TW-8140 (TW-8120)
  @Test
  public void testReportWithPrematureEndOfFileFull() throws Exception {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).logSuiteStarted(with("MCNTest"), with(any(Date.class)));
        inSequence(mySequence);
        exactly(15).of(myLogger).logTestStarted(with(any(String.class)), with(any(Date.class)));
        exactly(15).of(myLogger).logTestFinished(with(any(String.class)), with(any(Date.class)));
        inSequence(mySequence);
        oneOf(myLogger).logSuiteFinished(with("MCNTest"), with(any(Date.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("TestResults_TW8120.xml"));
    myContext.assertIsSatisfied();
  }

  //TW-8140 (TW-8120)
  @Test
  public void testReportWithPrematureEndOfFilePart() throws Exception {
    myParser.parse(report("TestResults_TW8120_part.xml"));
    myContext.assertIsSatisfied();
  }
}