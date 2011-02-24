package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.xmlReportPlugin.parsers.mstest.TestOutcome.Status.*;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 18:20:53
 */
enum TestOutcome {
  ABORTED(3, "Aborted", Failed),
  COMPLETED(11, "Completed", Passed),
  DISCONNECTED(8, "Disconnected", Passed),
  ERROR(0, "Error", Failed),
  FAILED(1, "Failed", Failed),
  INCONCLUSIVE(4, "Inconclusive", Ignored),
  IN_PROGRESS(12, "InProgress", Ignored),
  NOT_EXCLUDED(7, "NotExecuted", Ignored),
  NOT_RUNNABLE(6, "NotRunnable", Ignored),
  PASSED(10, "Passed", Passed),
  PASSED_BUT_RUN_ABOURTED(5, "PassedButRunAborted", Passed),
  PENDING(13, "Pending", Ignored),
  TIMEOUT(2, "Timeout", Failed),
  WARNING(9, "Warning", Passed),
  UNKNOWN(null, "__UNKNOWN__", Passed);

  private final Integer myValue8;
  private final String myName;
  private final Status myStatus;

  TestOutcome(final Integer value8, final String name, final Status status) {
    myValue8 = value8;
    myName = name;
    myStatus = status;
  }

  public boolean isIgnored() {
    return myStatus == Ignored;
  }

  public boolean isSuccessful() {
    return myStatus == Passed;
  }

  public boolean isFailed() {
    return myStatus == Failed;
  }

  public String getOutcomeName() {
    return myName;
  }

  @NotNull
  public static TestOutcome parse9(@Nullable final String name) {
    if (name == null) {
      return UNKNOWN;
    }
    for (TestOutcome outcome : values()) {
      if (outcome.myName.equalsIgnoreCase(name)) {
        return outcome;
      }
    }
    return UNKNOWN;
  }

  @NotNull
  public static TestOutcome parse8(final String value) {
    if (value == null) {
      return UNKNOWN;
    }

    try {
      int val = Integer.parseInt(value);
      for (TestOutcome outcome : values()) {
        if (outcome.myValue8 != null && val == outcome.myValue8) {
          return outcome;
        }
      }
    } catch (NumberFormatException e) {
      return UNKNOWN;
    }
    return UNKNOWN;
  }
 
  static enum Status {
    Ignored,
    Passed,
    Failed
  }
}
