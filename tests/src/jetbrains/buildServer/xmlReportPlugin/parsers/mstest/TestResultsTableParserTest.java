package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Proxy;

@Test
public class TestResultsTableParserTest {

  @Test
  public void test_tw_15210() throws IOException {
    doTest("tw-15210");
  }

  @Test
  public void test_tw_13034() throws IOException {
    doTest("tw-13034");
  }

  @Test
  public void test_Hyung_Choi_autodesk_com() throws IOException {
    doTest("hyung-choi-autodesk-com");
  }

  @Test
  public void test_dataDriven() throws IOException {
    doTest("dataDrivenTests");
  }

  @Test
  public void test_dataDriven2() throws IOException {
    doTest("dataDrivenTests2");
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
  public void test_tw_50428_results() throws IOException {
    doTest("tw-50428");
  }

  private static File getTestData(final String path) throws FileNotFoundException {
    return MSTestBaseTest.getTestData(path);
  }

  @SuppressWarnings("Duplicates")
  private void doTest(String testName) throws IOException {
    final File xml = getTestData(testName + ".trx");
    final File goldFile = getTestData(testName + ".results.gold");
    final File tmp = getTestData(testName + ".results.gold.tmp");

    FileUtil.delete(tmp);
    final StringBuilder sb = new StringBuilder();
    TestResultsTableParser ps = new TestResultsTableParser(
        (TestResultsTableParser.Callback)Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class[]{TestResultsTableParser.Callback.class},
            (proxy, method, args) -> {
              sb.append(method.getName())
                  .append("[ ")
                  .append(StringUtil.join(args, o -> {
                    if (o == null) {
                      return "<null>";
                    }
                    return StringUtil.truncateStringValueWithDotsAtEnd(o.toString().replaceAll("\\r?\\n", " "), 30);
                  }, ","))
                  .append(" ]\n");
              //noinspection ConstantConditions
              return null;
            }
        ));

    ps.parse(xml);

    final String actual = StringUtil.convertLineSeparators(sb.toString());
    final String gold = StringUtil.convertLineSeparators(goldFile.exists() ? new String(FileUtil.loadFileText(goldFile)) : "");

    try {
      Assert.assertEquals(actual, gold);
    } catch (Throwable t) {
      System.out.println("actual = " + actual);
      FileUtil.writeFileAndReportErrors(tmp, actual);
      throw new RuntimeException(t);
    }
  }
}
