

package jetbrains.buildServer.xmlReportPlugin.parsers;

import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestMessages {
  @NotNull
  public static String getFileContainsUnnamedMessage(@NotNull File file, @NotNull String type) {
    return "File " + file + " contains unnamed " + type;
  }

  @NotNull
  public static String getFailedToLogSuiteMessage(@NotNull String errorType, @Nullable String suiteName) {
    return "Failed to log suite " + errorType + " for not-opened suite " + suiteName;
  }

  @NotNull
  public static String getOutFromSuiteMessage(@NotNull String errorType, @NotNull String suiteName, @Nullable String type, @Nullable String message, @Nullable String trace) {
    String msg = getFailureMessage(type, message);
    if (trace != null) {
      msg += "\n" + trace;
    }
    return getOutFromSuiteMessage(errorType, suiteName, msg);
  }

  @NotNull
  public static String getOutFromSuiteMessage(@NotNull String errorType, @NotNull String suiteName, @NotNull String message) {
    return errorType + " from suite " + suiteName + ": " + message;
  }

  @NotNull
  public static String getFileExpectedFormatMessage(@NotNull File file, @NotNull String msg, @NotNull String testType) {
    return "File " + file + " doesn't match the expected format: " + msg + "\nPlease check " + testType + " binaries for the supported DTD";
  }

  @NotNull
  public static String getCouldNotCompletelyParseMessage(@NotNull File file, @NotNull Exception e, int loggedTests) {
    return "Couldn't completely parse " + file
           + " report, exception occurred: " + e + ", " + loggedTests + " tests logged";
  }

  @NotNull
  public static String getFailureMessage(@Nullable String type, @Nullable String message) {
    String failureMessage = "";
    if (type != null) {
      failureMessage = failureMessage.concat(type);
    }
    if (message != null) {
      if (failureMessage.length() > 0) {
        failureMessage = failureMessage.concat(": ");
      }
      failureMessage = failureMessage.concat(message);
    }
    return failureMessage;
  }

}