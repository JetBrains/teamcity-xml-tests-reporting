

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * @author vbedrosova
 */
@Test
public class NUnit3ReportParserTest extends BaseParserTestCase {
  @Test
  public void test_NUnitSendsTeamCitySServiceMessagesWhenIRunItForDifferentTypesOfTests() throws Exception {
    parse("NUnitSendsTeamCitySServiceMessagesWhenIRunItForDifferentTypesOfTests.xml");
    assertResultEquals(
      getExpectedResult("NUnitSendsTeamCitySServiceMessagesWhenIRunItForDifferentTypesOfTests.gold"));
  }

  @Test
  public void test_NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedOneTimeSetup() throws Exception {
    parse("NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedOneTimeSetup.xml");
    assertResultEquals(
      getExpectedResult("NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedOneTimeSetup.gold"));
  }

  @Test
  public void test_NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedSetup() throws Exception {
    parse("NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedSetup.xml");
    assertResultEquals(
      getExpectedResult("NUnitSendsTeamCitySServiceMessagesWhenIRunItForFailedSetup.gold"));
  }

  @Test
  public void test_NUnitSendsTeamCitySServiceMessagesWhenIRunItForParallelizableTests() throws Exception {
    parse("NUnitSendsTeamCitySServiceMessagesWhenIRunItForParallelizableTests.xml");
    assertResultEquals(
      getExpectedResult("NUnitSendsTeamCitySServiceMessagesWhenIRunItForParallelizableTests.gold"));
  }

  @Test
  public void test_UserRunsTestsForSeveralAssemblies() throws Exception {
    parse("UserRunsTestsForSeveralAssemblies.xml");
    assertResultEquals(
      getExpectedResult("UserRunsTestsForSeveralAssemblies.gold"));
  }

  @Test
  public void test_AssertPass() throws Exception {
    parse("Assert.Pass.xml");
    assertResultEquals(
      getExpectedResult("Assert.Pass.gold"));
  }

  @NotNull
  @Override
  protected Parser getParser() {
    return new NUnitReportParser(getTestReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return "nunit/nunit3";
  }
}