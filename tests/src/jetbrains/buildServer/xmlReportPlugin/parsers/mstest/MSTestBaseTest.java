/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.TestUtil;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 19:06:09
 */

@Test
public class MSTestBaseTest {

  @Test
  public void test_stofl() throws IOException {
    doTest("stofl.trx", "stofl.trx.gold");
  }

  @Test
  public void test_TW_15210() throws IOException {
    doTest("tw-15210.trx","tw-15210.trx.gold");
  }

  @Test
  public void test_TW_14132() throws IOException {
    doTest("tw-14132.trx", "tw-14132.trx.gold");
  }

  @Test
  public void test_TW_13364() throws IOException {
    doTest("tw-13364.trx", "tw-13364.trx.gold");
  }

  @Test
  public void test_TW_12876() throws IOException {
    doTest("tw-12876.trx", "tw-12876.trx.gold");
  }

  @Test
  public void test_TW_13034() throws IOException {
    doTest("tw-13034.trx", "tw-13034.trx.gold");
  }

  @Test
  public void test_TW_11804_1() throws IOException {
    doTest("235-62failed-Vulcan.Tests.dll.trx", "235-62failed-Vulcan.Tests.dll.trx.gold");
  }

  @Test
  public void test_TW_11804_2() throws IOException {
    doTest("237-5failed-Vulcan.Tests.dll.trx", "237-5failed-Vulcan.Tests.dll.trx.gold");
  }

  @Test
  public void test_TW_11148() throws IOException {
    doTest("not-existsing.trx", "not-existsing.trx.gold");
  }

  @Test
  public void test_TW_11002() throws IOException {
    doTest("tw-11002.trx", "tw-11002.trx.gold");
  }

  @Test
  public void test_VS2010() throws IOException {
    doTest("vs2010.trx", "vs2010.trx.gold");
  }

  @Test
  public void test_TW_10011() throws IOException {
    doTest("tw-10011.trx", "tw-10011.trx.gold");
  }
                                                                                                  
  @Test
  public void test_TW_9376_ordered() throws IOException{
    doTest("orderedTests.trx", "orderedTests.trx.gold");
  }

  @Test
  public void test_TW_8578() throws IOException {
    doTest("notExecuted.trx", "notExecuted.trx.gold");
  }

  @Test
  public void test_TW_8291() throws IOException {
    doTest("test-vs2005-TW-8291.trx", "test-vs2005-TW-8291.trx.gold");
  }

  @Test
  public void test_dataDriven() throws IOException {
    doTest("dataDrivenTests.trx", "dataDrivenTests.trx.gold");
  }

  @Test
  public void test_dataDriven2() throws IOException {
    doTest("dataDrivenTests2.trx", "dataDrivenTests2.trx.gold");
  }

  @Test
  public void test_TW_8291_2() throws IOException {
    doTest("test-vs2005-TW-8291-2.trx", "test-vs2005-TW-8291-2.trx.gold");
  }

  @Test
  public void test_Simple() throws IOException {
    doTest("tests.simple.q.dll.trx", "tests.simple.q.dll.trx.gold");
  }

  @Test
  public void test_negative() throws IOException {
    doTest("tests-negative.trx", "tests-negative.trx.gold");
  }

  @Test
  public void test_TW6771_vs2005() throws IOException {
    doTest("tests-forums.trx", "tests-forums.trx.gold");
  }

  @Test
  public void test_vs2005() throws IOException {
    doTest("tests-vs2005.trx", "tests-vs2005.trx.gold");
  }

  @Test
  public void test_TW_357245() throws IOException {
    doTest("tw35724.trx", "tw35724.trx.gold");
  }

  @Test//(dependsOnGroups = "implement TW-7907")
  public void test_Hyung_Choi_autodesk_com() throws IOException {
    doTest("hyung-choi-autodesk-com.trx", "hyung-choi-autodesk-com.trx.gold");
  }

  public static File getTestData(final String path) throws FileNotFoundException {
    return new File(TestUtil.getTestDataPath(path, "mstest").replace("\\", "/"));
  }

  private void doTest(String file, String gold) throws IOException {
    final StringBuilder sb = new StringBuilder();
    final TRXParser ps = new TRXParser(new TestReporter() {
      public void openTestSuite(@NotNull final String name) {
        sb.append("TestSuite:").append(name).append("\n");
      }

      public void openTest(@NotNull final String name) {
        sb.append("  Test:").append(name).append("\n");
      }

      public void testStdOutput(@NotNull final String text) {
        sb.append("    StdOutput:").append(text).append("\n");
      }

      public void testErrOutput(@NotNull final String text) {
        sb.append("    ErrOutput:").append(text).append("\n");
      }

      public void testFail(final String error, @Nullable final String stacktrace) {
        sb.append("    Fail:").append(error).append(" Message: ").append(stacktrace).append("\n");
      }

      public void testIgnored(@NotNull final String message) {
        sb.append("    Ignored:").append(message).append("\n");
      }

      public void closeTest(final long duration) {
        sb.append("  EndTest:").append(duration).append("\n------------------------\n");
      }

      public void closeTestSuite() {
        sb.append("EndSuite").append("\n");
      }

      public void warning(@NotNull final String s) {
        sb.append("-->Warning: ").append(s).append("\r\n");
      }

      public void error(@NotNull final String s) {
        sb.append("-->Error: ").append(s).append("\r\n");
      }

      public void info(@NotNull final String message) {
        sb.append("-->Info: ").append(message).append("\r\n");
      }

      @Override
      public void failure(@NotNull final String message) {
        sb.append("-->Problem: ").append(message).append("\r\n");
      }
    }, "MSTest");

    ps.parse(getTestData(file), null);

    String actual = sb.toString().replace(getTestData("").getPath(), "#PATH#").replace("#PATH#/", "#PATH#\\");
    compareFiles(gold, actual);
  }

  private void compareFiles(final String gold, final String actual) throws IOException {
    final File fGold = getTestData(gold);
    final File tempFile = new File(fGold.getPath() + ".tmp");
    FileUtil.writeFileAndReportErrors(tempFile, actual);

    final String sGold = StringUtil.convertLineSeparators(FileUtil.readText(fGold, "UTF-8"));
    final String sActual = StringUtil.convertLineSeparators(actual);

    assertEquals(sActual, sGold, "Actual: " + sActual);
    FileUtil.delete(tempFile);
  }
}


