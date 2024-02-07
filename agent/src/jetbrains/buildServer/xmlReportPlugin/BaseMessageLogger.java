

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.problems.BuildProblemUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author vbedrosova
 */
public class BaseMessageLogger implements MessageLogger {
  @NotNull
  protected final BuildProgressLogger myLogger;
  @NotNull
  protected final String myBuildProblemType;
  @NotNull
  protected final String myBaseFolder;

  public BaseMessageLogger(@NotNull final BuildProgressLogger logger, @NotNull final String buildProblemType, @NotNull final String baseFolder) {
    myLogger = logger;
    myBuildProblemType = buildProblemType;
    myBaseFolder = baseFolder;
  }

  @Override
  public void info(@NotNull final String message) {
    myLogger.message(makeRelativePaths(message));
  }

  @Override
  public void warning(@NotNull final String message) {
    myLogger.warning(makeRelativePaths(message));
  }

  @Override
  public void error(@NotNull final String message) {
    myLogger.error(makeRelativePaths(message));
  }

  @Override
  public void failure(@NotNull final String message) {
    myLogger.error(message);
    myLogger.logBuildProblem(BuildProblemUtil.createBuildProblem(myBuildProblemType, makeRelativePaths(message), myBaseFolder));
  }

  @NotNull
  protected String makeRelativePaths(@NotNull final String message) {
    return message.replace(myBaseFolder.replace("\\", "/") + "/", "").replace(myBaseFolder.replace("/", "\\") + "\\", "");
  }
}