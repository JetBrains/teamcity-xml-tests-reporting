package jetbrains.buildServer.xmlReportPlugin.tests;

import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:33
 */
public class TestsParsingResult implements ParsingResult {
  private int mySuites;
  private int myTests;

  public TestsParsingResult(int suites, int tests) {
    mySuites = suites;
    myTests = tests;
  }

  public int getSuites() {
    return mySuites;
  }

  public int getTests() {
    return myTests;
  }

  public void accumulate(@NotNull ParsingResult parsingResult) {
    final TestsParsingResult testsParsingResult = (TestsParsingResult) parsingResult;
    mySuites += testsParsingResult.getSuites();
    myTests += testsParsingResult.getTests();
  }

  @NotNull
  public static TestsParsingResult createEmptyResult() {
    return new TestsParsingResult(0, 0);
  }
}
