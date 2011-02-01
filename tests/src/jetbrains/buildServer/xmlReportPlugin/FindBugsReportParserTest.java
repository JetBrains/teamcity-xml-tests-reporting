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
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getTestDataFile;


public class FindBugsReportParserTest extends BaseParserTestCase {
  private static final String FINDBUGS_HOME = System.getProperty("findbugs");
  private static final String TYPE = "findBugs";

  @NotNull
  @Override
  protected Parser getParser() {
    try {
      return new FindBugsReportParser(getXMLReader(), getInspectionReporter(), getTestDataFile(null, TYPE),
        FINDBUGS_HOME != null && new File(FINDBUGS_HOME).exists() ? FINDBUGS_HOME : TestUtil.getTestDataPath(TYPE, null), getLogger());
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return TYPE;
  }

  private void runTest(final String sampleName) throws Exception {
    final String reportName = sampleName.replace(".sample", "");

    final File sample = getReport(sampleName);
    final File report = new File(getBaseDir(), reportName);

    FileUtil.writeFile(report,
      FileUtil.readText(sample).replace("##BASE_DIR##", getBaseDir().getAbsolutePath().replace("\\", "/")));

    parse(reportName);

    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.sample.xml");
  }

  @Test
  public void testNoSrcSimple() throws Exception {
    runTest("noSrcSimple.sample.xml");
  }

  @Test
  public void testNoSrcJar() throws Exception {
    runTest("jar.sample.xml");
  }

  @Test
  public void testComplexSrc() throws Exception {
    runTest("complexSrc.sample.xml");
  }

  @Test
  public void testNoSrcDir() throws Exception {
    runTest("dir.sample.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.sample.xml");
  }

  @Test
  public void testPattern() throws Exception {
    runTest("pattern.sample.xml");
  }

  @Test
  public void testCategory() throws Exception {
    runTest("category.sample.xml");
  }

  @Test
  public void testBuildFailsErrors() throws Exception {
    runTest("failureErr.sample.xml");
  }

  @Test
  public void testBuildFailsWarnings() throws Exception {
    runTest("failureWarn.sample.xml");
  }

  @Test
  public void testUnfinished() throws Exception {
    runTest("unfinished_simple.sample.xml");
  }
}