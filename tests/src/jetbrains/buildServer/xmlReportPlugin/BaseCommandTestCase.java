package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.util.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 25.01.11
 * Time: 13:32
 */
public abstract class BaseCommandTestCase extends TestCase {
  @NotNull
  protected static final ParsingResult EMPTY_RESULT = new ParsingResult() {
    public void accumulate(@NotNull ParsingResult parsingResult) {
    }

    @Override
    public String toString() {
      return "EMPTY_RESULT";
    }
  };

  protected File myBaseFolder;
  protected long myTestStartTime;

  @Before
  public void setUp() throws Exception {
    myTestStartTime = new Date().getTime();
    myBaseFolder = FileUtil.createTempDirectory("", "");
  }

  @After
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
    final String resultStr = result.toString().replace(myBaseFolder.getAbsolutePath(), "##BASE_DIR##").replace("\\", "/");

    final List<String> actualLines = Arrays.asList(resultStr.split("\\n"));
    for (String line : Arrays.asList(lines)) {
      assertTrue("Text must" + (shouldContain ? "" : " not") + " contain: " + line + "\nActual text is: " + result,
        shouldContain ? actualLines.contains(line) : !actualLines.contains(line) );
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
    FileUtil.writeFile(file, "some data");
    if (withDelay) {
      Thread.sleep(1000L); // to make sure time in seconds changes
    }
    return file;
  }
}
