/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

    public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {
    }

    public void logAsTotalResult(@NotNull ParseParameters parameters) {
    }

    @Override
    public String toString() {
      return "EMPTY_RESULT";
    }

    public Throwable getProblem() {
      return null;
    }
  };

  protected File myBaseFolder;
  protected long myTestStartTime;

  @Override
  @Before
  public void setUp() throws Exception {
    myTestStartTime = new Date().getTime();
    myBaseFolder = FileUtil.createTempDirectory("", "");
  }

  @Override
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
    final String resultStr = result.toString().replace("\\", "/").replace(myBaseFolder.getPath().replace("\\", "/"), "##BASE_DIR##");

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
