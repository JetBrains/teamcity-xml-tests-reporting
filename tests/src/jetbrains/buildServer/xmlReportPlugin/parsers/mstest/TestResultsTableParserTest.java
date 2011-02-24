package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import com.intellij.util.Function;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import junit.framework.TestCase;
import org.junit.Test;

public class TestResultsTableParserTest extends TestCase {
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

  public static File getTestData(final String path) throws FileNotFoundException {
    return MSTestBaseTest.getTestData(path);
  }

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
        new InvocationHandler() {
          public Object invoke(final Object proxy,
                               final Method method,
                               final Object[] args)
            throws Throwable {
            sb.append(method.getName())
              .append("[ ")
              .append(StringUtil.join(args, new Function<Object, String>() {
                public String fun(final Object o) {
                  return StringUtil.truncateStringValueWithDotsAtEnd(o.toString().replaceAll("\\r?\\n", " "), 30);
                }
              }, ","))
              .append(" ]\n");
            //noinspection ConstantConditions
            return null;
          }
        }
      ));

    ps.parse(xml);

    final String actual = StringUtil.convertLineSeparators(sb.toString());
    final String gold = StringUtil.convertLineSeparators(goldFile.exists() ? new String(FileUtil.loadFileText(goldFile)) : "");

    try {
      assertEquals(actual, gold);
    } catch (Throwable t) {
      System.out.println("actual = " + actual);
      FileUtil.writeFile(tmp, actual);
      throw new RuntimeException(t);
    }
  }
}
