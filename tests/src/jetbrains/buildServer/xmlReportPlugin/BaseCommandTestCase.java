

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;

import static org.testng.Assert.*;

/**
 * User: vbedrosova
 * Date: 25.01.11
 * Time: 13:32
 */
public abstract class BaseCommandTestCase {
  @NotNull
  protected static final ParsingResult EMPTY_RESULT = new ProblemParsingResult() {
    public void accumulate(@NotNull ParsingResult parsingResult) {}
    public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {}
    public void logAsTotalResult(@NotNull ParseParameters parameters) {}
    @Override
    public String toString() { return "EMPTY_RESULT"; }
  };

  protected File myBaseFolder;
  protected long myTestStartTime;

  @BeforeMethod
  public void setUp() throws Exception {
    myTestStartTime = new Date().getTime();
    myBaseFolder = FileUtil.createTempDirectory("", "");
  }

  @AfterTest
  public void tearDown() throws Exception {
    FileUtil.delete(myBaseFolder);
  }

  protected void assertNotContains(@NotNull StringBuilder result, @NotNull String ... lines) {
    assertContains(result, false, lines);
  }

  protected void assertContains(@NotNull StringBuilder result, @NotNull String ... lines) {
    assertContains(result, true, lines);
  }

  private void assertContains(@NotNull StringBuilder result, boolean shouldContain, @NotNull String ... lines) {
    final String resultStr = result.toString().replace("\\", "/").replace(myBaseFolder.getPath().replace("\\", "/"), "##BASE_DIR##");

    final List<String> actualLines = Arrays.asList(resultStr.split("\\n"));
    for (String line : Arrays.asList(lines)) {
      assertTrue(shouldContain ? actualLines.contains(line) : !actualLines.contains(line),
        "Text must" + (shouldContain ? "" : " not") + " contain: " + line + "\nActual text is: " + result);
    }
  }

  @NotNull
  protected File writeFile(@NotNull String baseDirRelativePath, boolean withDelay) throws Exception {
    final File file = new File(myBaseFolder, baseDirRelativePath);
    //noinspection ResultOfMethodCallIgnored
    file.getParentFile().mkdirs();
    return writeFile(file, withDelay);
  }

  @NotNull
  protected File writeFile(@NotNull File file, boolean withDelay) throws Exception {
    if (withDelay) {
      Thread.sleep(1000L); // to make sure time in seconds changes
    }
    FileUtil.writeFileAndReportErrors(file, "some data");
    if (withDelay) {
      Thread.sleep(1000L); // to make sure time in seconds changes
    }
    return file;
  }
}