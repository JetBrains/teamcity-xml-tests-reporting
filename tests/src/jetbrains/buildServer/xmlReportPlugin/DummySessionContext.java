package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;

public class DummySessionContext implements SessionContext {
  @NotNull private final BuildProgressLogger myLogger;

  public DummySessionContext(@NotNull final BuildProgressLogger logger) {
    myLogger = logger;
  }

  @NotNull
  public BuildProgressLogger getLogger() {
    return myLogger;
  }
}
