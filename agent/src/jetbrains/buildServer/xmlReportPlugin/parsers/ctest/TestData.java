/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;


import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Rassokhin
 */
@SuppressWarnings("UnusedDeclaration")
final class TestData {
  @Nullable
  private String myName;
  @Nullable
  private String myPath;
  @Nullable
  private String myFullName;
  @Nullable
  private String myFullCommandLine;

  @NotNull
  final private Status myStatus;
  private long myDuration;
  private String myExitCode;
  private String myCompletionStatus;
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  @NotNull
  private final List<NamedMeasurement> myNamedMeasurements = new ArrayList<NamedMeasurement>();
  private int myExitValue;
  private String myReason;
  private String myLog;

  public TestData(@Nullable String status) {
    myStatus = Status.fromAttributeValue(status);
  }

  public long getDuration() {
    return myDuration;
  }

  public void setExitCode(String code) {
    myExitCode = code;
  }

  public void setCompletionStatus(String s) {
    myCompletionStatus = s;
  }

  public void addNamedMeasurement(@NotNull String name, @NotNull String type, @NotNull String value) {
    myNamedMeasurements.add(new NamedMeasurement(name, type, value));
  }

  @NotNull
  public String getFailureMessage() {
    final StringBuilder sb = new StringBuilder();
    if ("Failed to start".equals(myCompletionStatus)) {
      sb.append(myCompletionStatus);
      if (myExitCode != null) {
        sb.append(" (\"").append(myExitCode).append("\")");
      }
    } else if ("Not run".equals(myCompletionStatus)) {
      sb.append(myCompletionStatus);
    } else if ("Completed".equals(myCompletionStatus)) {
      if (myExitCode != null) {
        sb.append("\"").append(myExitCode).append("\" ");
      }
      sb.append("(exit code: ").append(myExitValue).append(")");
    } else if (myCompletionStatus != null) {
      sb.append("Unknown status: ").append(myCompletionStatus).append(' ');
      if (myExitCode != null) {
        sb.append(myExitCode).append(' ');
      }
      sb.append("(exit code: ").append(myExitValue).append(")");
    }

    if (myReason != null) {
      sb.append(' ').append(myReason);
    }

    return sb.toString();
  }

  @Nullable
  public String getLog() {
    return myLog;
  }

  public void setExitValue(int exitValue) {
    myExitValue = exitValue;
  }

  public void setReason(String reason) {
    myReason = reason;
  }

  public void setLog(@NotNull String log, @Nullable String compression) {
    // TODO: decompress log
    myLog = log;
  }

  enum Status {
    COMPLETED,
    FAILED,
    NOT_RUN;

    public static Status fromAttributeValue(@Nullable String status) {
      if ("passed".equals(status)) {
        return Status.COMPLETED;
      } else if ("notrun".equals(status)) {
        return Status.NOT_RUN;
      }
      return Status.FAILED;
    }
  }

  static class NamedMeasurement {
    final String myName;
    final String myType;
    final String myValue;

    public NamedMeasurement(String name, String type, String value) {
      myName = name;
      myType = type;
      myValue = value;

    }
  }

  @Nullable
  public String getName() {
    return myName;
  }

  public void setName(@NotNull final String name) {
    myName = name.replaceAll("\\.", "_");
  }

  @Nullable
  public String getFullCommandLine() {
    return myFullCommandLine;
  }

  public void setFullCommandLine(@NotNull String fullCommandLine) {
    this.myFullCommandLine = fullCommandLine;
  }

  @Nullable
  public String getFullName() {
    return myFullName;
  }

  public void setFullName(@NotNull String fullName) {
    this.myFullName = fullName;
  }

  @Nullable
  public String getMyPath() {
    return myPath;
  }

  public void setPath(@NotNull String path) {
    this.myPath = path;
  }

  @NotNull
  public Status getStatus() {
    return myStatus;
  }

  public void setDuration(long duration) {
    this.myDuration = duration;
  }
}