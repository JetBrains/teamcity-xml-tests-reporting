

package jetbrains.buildServer.xmlReportPlugin.tests;

import java.util.ArrayDeque;
import java.util.Deque;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.xmlReportPlugin.BaseMessageLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 20:42:56
 */
public class TeamCityTestReporter extends BaseMessageLogger implements TestReporter {
  @NotNull
  private final Deque<String> myTestSuites = new ArrayDeque<String>();
  @NotNull
  private final Deque<String> myTests = new ArrayDeque<String>();

  public TeamCityTestReporter(@NotNull final BuildProgressLogger logger, @NotNull final String buildProblemType, @NotNull final String baseFolder) {
    super(logger, buildProblemType, baseFolder);
  }

  public void openTestSuite(@NotNull final String name) {
    myTestSuites.push(name);
    myLogger.logMessage(DefaultMessagesInfo.createTestSuiteStart(name));
  }

  public void openTest(@NotNull final String name) {
    myTests.push(name);
    myLogger.logMessage(DefaultMessagesInfo.createTestBlockStart(name, false, 0L));
  }

  public void testStdOutput(@NotNull final String text) {
    myLogger.logMessage(DefaultMessagesInfo.createTestStdout(myTests.peek(), text));
  }

  public void testErrOutput(@NotNull final String text) {
    myLogger.logMessage(DefaultMessagesInfo.createTestStderr(myTests.peek(), text));
  }

  public void testFail(@Nullable String error, @Nullable final String stacktrace) {
    if (error == null) error = "";
    myLogger.logTestFailed(myTests.peek(), error, stacktrace);
  }

  public void testIgnored(@NotNull final String message) {
    myLogger.logMessage(DefaultMessagesInfo.createTestIgnoreMessage(myTests.peek(), message));
  }

  public void closeTest(final long duration) {
    myLogger.logMessage(DefaultMessagesInfo.createTestBlockEnd(myTests.pop(), (int) duration, null));
  }

  public void closeTestSuite() {
    myLogger.logMessage(DefaultMessagesInfo.createTestSuiteEnd(myTestSuites.pop()));
  }
}