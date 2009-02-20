package jetbrains.buildServer.xmlReportPlugin.integration;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.ANT_JUNIT_REPORT_TYPE;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.NUNIT_REPORT_TYPE;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class ReportFactory {
  private static String WORKING_DIR;

  public static void setWorkingDir(@NotNull String workingDir) {
    WORKING_DIR = workingDir;
  }

  public static void createDir(String name) {
    (new File(WORKING_DIR + "\\" + name)).mkdir();
  }

  public static void createFile(String name) {
    final File f = new File(WORKING_DIR + "\\" + name);
    try {
      final FileWriter fw = new FileWriter(f);
      fw.write("File content");
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createFile(String name, String content) {
    final File f = new File(WORKING_DIR + "\\" + name);
    try {
      final FileWriter fw = new FileWriter(f);
      fw.write(content);
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static File getFileByName(String name) {
    return new File(WORKING_DIR + "\\" + name);
  }

  public static void createUnfinishedReport(String name, String type) {
    final File f = new File(WORKING_DIR + "\\" + name);
    try {
      final FileWriter fw = new FileWriter(f);
      if (ANT_JUNIT_REPORT_TYPE.equals(type)) {
        fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
          "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"2\" time=\"0.062\"\n" +
          "           timestamp=\"2008-10-30T17:11:25\">\n" +
          "  <properties/>\n" +
          "  <testcase classname=\"TestCase\" name=\"test1\" time=\"0.031\">\n" +
          "    <failure message=\"Assertion message form test\" type=\"junit.framework.AssertionFailedError\">\n" +
          "      junit.framework.AssertionFailedError: Assertion message form test\n" +
          "      at TestCase.test(Unknown Source)\n" +
          "    </failure>\n" +
          "  </testcase>\n" +
          "  <testcase classname=\"TestCase\" name=\"test2\" time=\"0.031\">\n" +
          "    <error message=\"Error messag");
      } else if (NUNIT_REPORT_TYPE.equals(type)) {
        fw.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
          "<test-results name=\"C:\\Program Files\\NUnit-2.4.8-net-2.0\\bin\\NUnitTests.nunit\" total=\"1\" failures=\"0\" not-run=\"0\"\n" +
          "              date=\"2008-12-23\" time=\"18:33:48\">\n" +
          "  <environment nunit-version=\"2.4.8.0\" clr-version=\"2.0.50727.1433\"\n" +
          "               os-version=\"Microsoft Windows NT 5.1.2600 Service Pack 3\" platform=\"Win32NT\"\n" +
          "               cwd=\"C:\\Program Files\\NUnit-2.4.8-net-2.0\\bin\" machine-name=\"RUSPD-STUDENT3\" user=\"vbedrosova\"\n" +
          "               user-domain=\"SWIFTTEAMS\"/>\n" +
          "  <culture-info current-culture=\"ru-RU\" current-uiculture=\"en-US\"/>\n" +
          "  <test-suite name=\"C:\\Program Files\\NUnit-2.4.8-net-2.0\\bin\\NUnitTests.nunit\" success=\"True\" time=\"61.592\" asserts=\"0\">\n" +
          "    <results>\n" +
          "      <test-case name=\"NUnit.Framework.Constraints.Tests.AllItemsTests.AllItemsAreInRange\"\n" +
          "               executed=\"True\" success=\"True\" time=\"0.016\" asserts=\"1\"/>\n" +
          "    </re");
      }
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
