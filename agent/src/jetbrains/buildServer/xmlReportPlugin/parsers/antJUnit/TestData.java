

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;


import org.jetbrains.annotations.Nullable;

final class TestData {
  @Nullable
  private String myName;
  private long myDuration;
  private boolean myExecuted;

  @Nullable
  private String myFailureType;
  @Nullable
  private String myFailureMessage;
  @Nullable
  private String myFailureStackTrace;

  @Nullable
  private String myStdOut;
  @Nullable
  private String myStdErr;

  @Nullable
  public String getName() {
    return myName;
  }

  public void setName(@Nullable final String name) {
    myName = name;
  }

  public long getDuration() {
    return myDuration;
  }

  public void setDuration(final long duration) {
    myDuration = duration;
  }

  public boolean isExecuted() {
    return myExecuted;
  }

  public void setExecuted(final boolean executed) {
    myExecuted = executed;
  }

  @Nullable
  public String getFailureType() {
    return myFailureType;
  }

  public void setFailureType(@Nullable final String failureType) {
    myFailureType = failureType;
  }

  @Nullable
  public String getFailureMessage() {
    return myFailureMessage;
  }

  public void setFailureMessage(@Nullable final String failureMessage) {
    myFailureMessage = failureMessage;
  }

  @Nullable
  public String getFailureStackTrace() {
    return myFailureStackTrace;
  }

  public void setFailureStackTrace(@Nullable final String failureStackTrace) {
    myFailureStackTrace = failureStackTrace;
  }

  @Nullable
  public String getStdOut() {
    return myStdOut;
  }

  public void setStdOut(@Nullable final String stdOut) {
    myStdOut = stdOut;
  }

  @Nullable
  public String getStdErr() {
    return myStdErr;
  }

  public void setStdErr(@Nullable final String stdErr) {
    myStdErr = stdErr;
  }
}