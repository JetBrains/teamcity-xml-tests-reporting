package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 17:51
 */
public interface MessageLogger {
  void info(@NotNull String message);
  void warning(@NotNull String message);
  void error(@NotNull String message);
}
