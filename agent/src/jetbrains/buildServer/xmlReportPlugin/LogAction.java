

package jetbrains.buildServer.xmlReportPlugin;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 14:28
 */
public enum LogAction {
  DO_NOTHING("nothing") {
    @Override
    public void doLogAction(@NotNull String message, @NotNull BuildProgressLogger logger) {}
  },
  INFO("info") {
    @Override
    public void doLogAction(@NotNull String message, @NotNull BuildProgressLogger logger) {
      LoggingUtils.LOG.info(message);
      logger.message(message);
    }
  },
  WARNING("warning") {
    @Override
    public void doLogAction(@NotNull String message, @NotNull BuildProgressLogger logger) {
      LoggingUtils.LOG.warn(message);
      logger.warning(message);
    }
  },
  ERROR("error") {
    @Override
    public void doLogAction(@NotNull String message, @NotNull BuildProgressLogger logger) {
      LoggingUtils.LOG.warn(message);
      logger.error(message);
    }
  };

  private static final List<LogAction> ourActions = Arrays.asList(DO_NOTHING, WARNING, ERROR);

  @NotNull
  private final String myName;

  private LogAction(@NotNull String name) {
    myName = name;
  }

  @NotNull
  private String getName() {
    return myName;
  }

  public abstract void doLogAction(@NotNull String message, @NotNull BuildProgressLogger logger);

  public static LogAction getAction(@Nullable String name) {
    if (name == null) return INFO;
    for (final LogAction action : ourActions) {
      if (action.getName().equals(name)) return action;
    }
    return INFO;
  }
}