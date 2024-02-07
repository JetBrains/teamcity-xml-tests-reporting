

package jetbrains.buildServer.xmlReportPlugin;

import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
  public void testVerboseOnEmptyParams() {
    assertFalse(isOutputVerbose(myRunParams), "Verbose output must be disabled");
  }

  @Test
  public void testVerboseAfterPuttingTrueToParams() {
    myRunParams.put(VERBOSE_OUTPUT, TRUE);
    assertTrue(isOutputVerbose(myRunParams), "Verbose output must be enabled");
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
  public void testGetMaxErrorsOnEmptyParams() {
    assertEquals(getMaxErrors(myRunParams), -1);
  }

  @Test
  public void testGetMaxErrorsAfterPuttingToParams() {
    myRunParams.put(MAX_ERRORS, "10");
    assertEquals(getMaxErrors(myRunParams), 10);
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
}