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

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.ANT_JUNIT_REPORT_TYPE;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.EMPTY_REPORT_TYPE;
import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;
import static junit.framework.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;


public class XmlReportPluginUtilTest {
  private static final String TRUE = "true";

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
  public void testSetReportDirsAfterPuttingTrueToParams() {
    final String reportDirs = "reportDirs";
    myRunParams.put(REPORT_TYPE, ANT_JUNIT_REPORT_TYPE);
    setXmlReportDirs(myRunParams, reportDirs);
    assertEquals("Unexpected vulue in parameters", myRunParams.get(REPORT_DIRS), reportDirs);
  }

  @Test
  public void testSetReportDirsAfterDisabling() {
    final String reportDirs = "reportDirs";
    enableXmlReportParsing(myRunParams, EMPTY_REPORT_TYPE);
    setXmlReportDirs(myRunParams, reportDirs);
    assertNull("ReportDirs parameter must be null", myRunParams.get(REPORT_DIRS));
  }
}