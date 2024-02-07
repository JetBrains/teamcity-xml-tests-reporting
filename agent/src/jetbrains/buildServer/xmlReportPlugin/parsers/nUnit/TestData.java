

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;


import org.jetbrains.annotations.Nullable;

final class TestData {
  @Nullable
  private String myName;
  private long myDuration;
  private boolean myIgnored;

  @Nullable
  private String myMessage;
  @Nullable
  private String myOutput;
  @Nullable
  private String myFailureStackTrace;
  private boolean mySuccess;

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

  public boolean isIgnored() {
    return myIgnored;
  }

  public void setIgnored(final boolean executed) {
    myIgnored = executed;
  }

  @Nullable
  public String getMessage() {
    return myMessage;
  }

  public void setMessage(@Nullable final String message) {
    myMessage = message;
  }

  @Nullable
  public String getOutput() {
    return myOutput;
  }

  public void setOutput(@Nullable final String output) {
    myOutput = output;
  }

  @Nullable
  public String getFailureStackTrace() {
    return myFailureStackTrace;
  }

  public void setFailureStackTrace(@Nullable final String failureStackTrace) {
    myFailureStackTrace = failureStackTrace;
  }

  public void setSuccess(final boolean success) {
    mySuccess = success;
  }

  public boolean isSuccess() {
    return mySuccess;
  }
}