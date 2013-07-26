/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginConstants.*;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;
import static org.testng.Assert.*;

@Test
public class XmlReportPluginUtilTest {
  private static final String TRUE = "true";
  private static final String ANT_JUNIT_REPORT_TYPE = "junit";
  private static final String EMPTY_REPORT_TYPE = "";

  private Map<String, String> myRunParams;

  @BeforeMethod
  public void setUp() {
    myRunParams = new HashMap<String, String>();
  }

  @Test
  public void testIsEnabledOnEmptyParams() {
    assertFalse(isParsingEnabled(myRunParams), "Test report parsing must be disabled");
  }

  @Test
  public void testIsEnabledAfterPuttingTrueToParams() {
    myRunParams.put(REPORT_TYPE, TRUE);
    assertTrue(isParsingEnabled(myRunParams), "Test report parsing must be enabled");
  }

  @Test
  public void testParamsContainTrueAfterEnabling() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    assertEquals(myRunParams.get(REPORT_TYPE), ANT_JUNIT_REPORT_TYPE, "Params must contain true");
  }

  @Test
  public void testParamsDoNotContainTrueAfterDisabling() {
    myRunParams.put(REPORT_TYPE, ANT_JUNIT_REPORT_TYPE);
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    assertNull(myRunParams.get(REPORT_TYPE), "Params must not contain any value for this key");
  }

  @Test
  public void testIsEnabledAfterEnabling() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    assertTrue(isParsingEnabled(myRunParams), "Test report parsing must be enabled");
  }

  @Test
  public void testIsEnabledAfterDisabling() {
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    assertFalse(isParsingEnabled(myRunParams), "Test report parsing must be disabled");
  }

  @Test
  public void testSetReportDirsAfterPuttingToParams() {
    final String reportDirs = "reportDirs";
    myRunParams.put(REPORT_TYPE, ANT_JUNIT_REPORT_TYPE);
    setXmlReportPaths(myRunParams, reportDirs);
    assertEquals(myRunParams.get(REPORT_DIRS), reportDirs, "Unexpected value in parameters");
  }

  @Test
  public void testSetReportDirsAfterDisabling() {
    final String reportDirs = "reportDirs";
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    setXmlReportPaths(myRunParams, reportDirs);
    assertNull(myRunParams.get(REPORT_DIRS), "ReportDirs parameter must be null");
  }

  @Test
  public void testVerboseOnEmptyParams() {
    assertFalse(isOutputVerbose(myRunParams), "Verbose output must be disabled");
  }

  @Test
  public void testVerboseAfterPuttingTrueToParams() {
    myRunParams.put(VERBOSE_OUTPUT, TRUE);
    assertTrue(isOutputVerbose(myRunParams), "Verbose output must be enabled");
  }

  @Test
  public void testParamsContainTrueAfterEnablingVerbose() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setVerboseOutput(myRunParams, true);
    assertEquals(myRunParams.get(VERBOSE_OUTPUT), TRUE, "Params must contain true");
  }

  @Test
  public void testParamsContainNothingAfterDisablingVerbose() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setVerboseOutput(myRunParams, false);
    assertNull(myRunParams.get(VERBOSE_OUTPUT), "Params mustn't contain anything");
  }

  @Test
  public void testParamsNotContainTrueAfterEnablingVerboseWhenParsingDisabled() {
    setVerboseOutput(myRunParams, true);
    assertNull(myRunParams.get(VERBOSE_OUTPUT), "Params mustn't contain anything");
  }

  @Test
  public void testParseOutOfDateOnEmptyParams() {
    assertFalse(isParseOutOfDateReports(myRunParams), "Parse outofdate must be disabled");
  }

  @Test
  public void testParseOutOfDateAfterPuttingTrueToParams() {
    myRunParams.put(PARSE_OUT_OF_DATE, TRUE);
    assertTrue(isParseOutOfDateReports(myRunParams), "Parse outofdate must be enabled");
  }

  @Test
  public void testParamsContainTrueAfterEnablingParseOutOfDate() {
    setParseOutOfDateReports(myRunParams, true);
    assertEquals(myRunParams.get(PARSE_OUT_OF_DATE), TRUE, "Params must contain true");
  }

  @Test
  public void testParamsContainNothingAfterDisablingParseOutOfDate() {
    setParseOutOfDateReports(myRunParams, false);
    assertNull(myRunParams.get(PARSE_OUT_OF_DATE), "Params must contain anything");
  }

  @Test
  public void testGetMaxErrorsOnEmptyParams() {
    assertEquals(getMaxErrors(myRunParams), -1);
  }

  @Test
  public void testGetMaxErrorsAfterPuttingToParams() {
    myRunParams.put(MAX_ERRORS, "10");
    assertEquals(getMaxErrors(myRunParams), 10);
  }

  @Test
  public void testParamsContainsValueAfterSettingMaxErrors() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setMaxErrors(myRunParams, 10);
    assertEquals(myRunParams.get(MAX_ERRORS), "10");
  }

  @Test
  public void testGetMaxWarningsOnEmptyParams() {
    assertEquals(getMaxWarnings(myRunParams), -1);
  }

  @Test
  public void testGetMaxWarningsAfterPuttingToParams() {
    myRunParams.put(MAX_WARNINGS, "100");
    assertEquals(getMaxWarnings(myRunParams), 100);
  }

  @Test
  public void testParamsContainsValueAfterSettingMaxWarnings() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setMaxWarnings(myRunParams, 100);
    assertEquals(myRunParams.get(MAX_WARNINGS), "100");
  }
}