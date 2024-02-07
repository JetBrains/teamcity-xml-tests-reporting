

package jetbrains.buildServer.xmlReportPlugin.tests;

import jetbrains.buildServer.xmlReportPlugin.MessageLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 18:27:35
 */
public interface TestReporter extends MessageLogger {
  void openTestSuite(@NotNull String name);
  void openTest(@NotNull String name);

  void testStdOutput(@NotNull String text);
  void testErrOutput(@NotNull String text);
  void testFail(@Nullable String error, @Nullable String stacktrace);
  void testIgnored(@NotNull String message);

  void closeTest(long duration);
  void closeTestSuite();
}