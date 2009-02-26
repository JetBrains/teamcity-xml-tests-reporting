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
package jetbrains.buildServer.xmlReportPlugin;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;
import static junit.framework.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class XmlReportPluginUtilTest {
  private static final String TRUE = "true";
  private static final String ANT_JUNIT_REPORT_TYPE = "junit";
  private static final String EMPTY_REPORT_TYPE = "";

  private Map<String, String> myRunParams;

  @Before
  public void setUp() {
    myRunParams = new HashMap<String, String>();
  }

  @Test
  public void testIsEnabledOnEmptyParams() {
    assertFalse("Test report parsing must be disabled", isParsingEnabled(myRunParams));
  }

  @Test
  public void testIsEnabledAfterPuttingTrueToParams() {
    myRunParams.put(REPORT_TYPE, TRUE);
    assertTrue("Test report parsing must be enabled", isParsingEnabled(myRunParams));
  }

  @Test
  public void testParamsContainTrueAfterEnabling() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    assertEquals("Params must contain true", ANT_JUNIT_REPORT_TYPE, myRunParams.get(REPORT_TYPE));
  }

  @Test
  public void testParamsDoNotContainTrueAfterDisabling() {
    myRunParams.put(REPORT_TYPE, ANT_JUNIT_REPORT_TYPE);
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    assertNull("Params must not contain any value for this key", myRunParams.get(REPORT_TYPE));
  }

  @Test
  public void testIsEnabledAfterEnabling() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    assertTrue("Test report parsing must be enabled", isParsingEnabled(myRunParams));
  }

  @Test
  public void testIsEnabledAfterDisabling() {
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    assertFalse("Test report parsing must be disabled", isParsingEnabled(myRunParams));
  }

  @Test
  public void testSetReportDirsAfterPuttingToParams() {
    final String reportDirs = "reportDirs";
    myRunParams.put(REPORT_TYPE, ANT_JUNIT_REPORT_TYPE);
    setXmlReportDirs(myRunParams, reportDirs);
    assertEquals("Unexpected value in parameters", myRunParams.get(REPORT_DIRS), reportDirs);
  }

  @Test
  public void testSetReportDirsAfterDisabling() {
    final String reportDirs = "reportDirs";
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    setXmlReportDirs(myRunParams, reportDirs);
    assertNull("ReportDirs parameter must be null", myRunParams.get(REPORT_DIRS));
  }

  @Test
  public void testVerboseOnEmptyParams() {
    assertFalse("Verbose output must be disabled", isOutputVerbose(myRunParams));
  }

  @Test
  public void testVerboseAfterPuttingTrueToParams() {
    myRunParams.put(VERBOSE_OUTPUT, TRUE);
    assertTrue("Verbose output must be enabled", isOutputVerbose(myRunParams));
  }

  @Test
  public void testParamsContainTrueAfterEnablingVerbose() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setVerboseOutput(myRunParams, true);
    assertEquals("Params must contain true", TRUE, myRunParams.get(VERBOSE_OUTPUT));
  }

  @Test
  public void testParamsContainNothingAfterDisablingVerbose() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setVerboseOutput(myRunParams, false);
    assertNull("Params mustn't contain anything", myRunParams.get(VERBOSE_OUTPUT));
  }

  @Test
  public void testParamsNotContainTrueAfterEnablingVerboseWhenParsingDisabled() {
    setVerboseOutput(myRunParams, true);
    assertNull("Params mustn't contain anything", myRunParams.get(VERBOSE_OUTPUT));
  }

  @Test
  public void testParseOutOfDateOnEmptyParams() {
    assertFalse("Parse outofdate must be disabled", shouldParseOutOfDateReports(myRunParams));
  }

  @Test
  public void testParseOutOfDateAfterPuttingTrueToParams() {
    myRunParams.put(PARSE_OUT_OF_DATE, TRUE);
    assertTrue("Parse outofdate must be enabled", shouldParseOutOfDateReports(myRunParams));
  }

  @Test
  public void testParamsContainTrueAfterEnablingParseOutOfDate() {
    setParseOutOfDateReports(myRunParams, true);
    assertEquals("Params must contain true", TRUE, myRunParams.get(PARSE_OUT_OF_DATE));
  }

  @Test
  public void testParamsContainNothingAfterDisablingParseOutOfDate() {
    setParseOutOfDateReports(myRunParams, false);
    assertNull("Params must contain anything", myRunParams.get(PARSE_OUT_OF_DATE));
  }

  @Test
  public void testGetMaxErrorsOnEmptyParams() {
    assertEquals("Max errors value must be -1", -1, getMaxErrors(myRunParams));
  }

  @Test
  public void testGetMaxErrorsAfterPuttingToParams() {
    myRunParams.put(MAX_ERRORS, "10");
    assertEquals("Max errors value must be 10", 10, getMaxErrors(myRunParams));
  }

  @Test
  public void testParamsContainsValueAfterSettingMaxErrors() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setMaxErrors(myRunParams, 10);
    assertEquals("Max errors value must be 10", "10", myRunParams.get(MAX_ERRORS));
  }

  @Test
  public void testGetMaxWarningsOnEmptyParams() {
    assertEquals("Max warnings value must be -1", -1, getMaxWarnings(myRunParams));
  }

  @Test
  public void testGetMaxWarningsAfterPuttingToParams() {
    myRunParams.put(MAX_WARNINGS, "100");
    assertEquals("Max warnings value must be 100", 100, getMaxWarnings(myRunParams));
  }

  @Test
  public void testParamsContainsValueAfterSettingMaxWarnings() {
    enableXmlReportParsing(myRunParams, ANT_JUNIT_REPORT_TYPE);
    setMaxWarnings(myRunParams, 100);
    assertEquals("Max warnings value must be 100", "100", myRunParams.get(MAX_WARNINGS));
  }
}