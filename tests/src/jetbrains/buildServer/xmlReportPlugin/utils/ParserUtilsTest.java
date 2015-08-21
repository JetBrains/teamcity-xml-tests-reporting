package jetbrains.buildServer.xmlReportPlugin.utils;

import java.io.File;
import java.io.FileNotFoundException;
import jetbrains.buildServer.xmlReportPlugin.TestUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ParserUtilsTest {

  private File getReport(final String name) throws FileNotFoundException {
    return TestUtil.getTestDataFile(name, "pmd");
  }

  private void doTestReportComplete(final String name, final boolean expected) throws FileNotFoundException {
    final File report = getReport(name);
    Assert.assertTrue(report.exists());
    Assert.assertEquals(ParserUtils.isReportComplete(report, null), expected);
  }

  @Test
  public void testIsReportComplete_Simple() throws Exception {
    doTestReportComplete("simple.xml", true);
  }

  @Test
  public void testIsReportComplete_XXE_File() throws Exception {
    doTestReportComplete("xml-xxe-file.xml", true);
  }

  @Test
  public void testIsReportComplete_XXE_URL() throws Exception {
    doTestReportComplete("xml-xxe-url.xml", true);
  }

  @Test
  public void testIsReportComplete_XXE_File_First() throws Exception {
    doTestReportComplete("xml-xxe-file-first.xml", true);
  }

  @Test
  public void testIsReportComplete_XML_Bomb() throws Exception {
    doTestReportComplete("xml-bomb.xml", false);
  }
}