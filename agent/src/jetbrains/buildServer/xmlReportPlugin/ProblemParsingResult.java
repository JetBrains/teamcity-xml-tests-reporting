

package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 04.03.11
 * Time: 20:47
 */
public abstract class ProblemParsingResult implements ParsingResult {
  @Nullable
  private Throwable myProblem;

  public ProblemParsingResult() {
    this(null);
  }

  public ProblemParsingResult(@Nullable Throwable problem) {
    myProblem = problem;
  }

  @Nullable
  public Throwable getProblem() {
    return myProblem;
  }

  public void setProblem(@NotNull final Throwable problem) {
    myProblem = problem;
  }
}