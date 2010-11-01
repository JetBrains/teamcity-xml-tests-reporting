package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;

public class DummyImportRequestContext implements ImportRequestContext {
  @NotNull private final File myPath;
  @NotNull private final BuildProgressLogger myLogger;

  public DummyImportRequestContext(@NotNull final File path, @NotNull final BuildProgressLogger logger) {
    myPath = path;
    myLogger = logger;
  }

  @NotNull
  public File getPath() {
    return myPath;
  }

  @NotNull
  public BuildProgressLogger getLogger() {
    return myLogger;
  }
}
