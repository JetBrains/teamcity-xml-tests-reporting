package jetbrains.buildServer.testReportParserPlugin.integration;

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

  public static void createUnfinishedReport(String name) {
    final File f = new File(WORKING_DIR + "\\" + name);
    try {
      final FileWriter fw = new FileWriter(f);
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
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
