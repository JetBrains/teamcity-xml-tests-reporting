package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

public class TestNamesTableParserTest extends TestCase {
  @Test
  public void test_TW_9376_ordered() throws IOException{
    doTest("orderedTests");
  }

  @Test//(dependsOnGroups = "implement TW-7907")
  public void test_Hyung_Choi_autodesk_com() throws IOException {
    doTest("hyung-choi-autodesk-com");
  }

  @Test
  public void test_vs2005() throws IOException {
    doTest("tests-vs2005");
  }

  @Test
  public void test_vs2008() throws IOException {
    doTest("tests.simple.q.dll");
  }

  @Test
  public void test_tw_24766() throws IOException {
    doTest("tmpC349.tmp.teamcity-TW24766");
  }

  public static File getTestData(final String path) throws FileNotFoundException {
    return MSTestBaseTest.getTestData(path);
  }

  private void doTest(String testName) throws IOException {
    final File xml = getTestData(testName + ".trx");
    final File goldFile = getTestData(testName + ".names.gold");
    final File tmp = getTestData(testName + ".names.gold.tmp");

    FileUtil.delete(tmp);

    final List<String> myTests = new ArrayList<String>();
    TestNamesTableParser ps = new TestNamesTableParser(new TestNamesTableParser.Callback() {
      public void testMethodFound(@NotNull final String id, @NotNull final String testName) {
        myTests.add(testName + " " + id);
      }
    });
    ps.parse(xml);
    Collections.sort(myTests);

    final String actual = StringUtil.convertLineSeparators(StringUtil.join(myTests, "\n"));
    final String gold = StringUtil.convertLineSeparators(goldFile.exists() ? FileUtil.readText(goldFile, "UTF-8") : "");

    try {
      assertEquals(gold, actual);
    } catch (Throwable t) {
      System.out.println("actual = " + actual);
      FileUtil.writeFile(tmp, actual);
      throw new RuntimeException(t);
    }
  }
}


