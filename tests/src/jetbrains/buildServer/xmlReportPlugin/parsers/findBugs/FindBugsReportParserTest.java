

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.io.File;
import java.io.FileNotFoundException;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.TestUtil;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

@Test
public class FindBugsReportParserTest extends BaseParserTestCase {
  private static final String FINDBUGS_HOME = System.getProperty("findbugs");
  private static final String TYPE = "findBugs";

  @NotNull
  @Override
  protected Parser getParser() {
    try {
      return new FindBugsReportParser(getInspectionReporter(), FINDBUGS_HOME != null && new File(FINDBUGS_HOME).exists() ? FINDBUGS_HOME : TestUtil
        .getTestDataPath(TYPE, null), getBaseDir());
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

    FileUtil.writeFileAndReportErrors(report, FileUtil.readText(sample).replace("##BASE_DIR##", getBaseDir().getAbsolutePath().replace("\\", "/")));

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

  @Test
  public void testMultipleSourceLine() throws Exception {
    runTest("sourceLine.sample.xml");
  }
}