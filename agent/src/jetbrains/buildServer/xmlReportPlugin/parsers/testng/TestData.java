/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.testng;


import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class TestData {
  private final static Object EMPTY_PARAM = new Object();
  @NotNull
  private final List<String> myTestMessage = new ArrayList<String>();
  @NotNull
  private final List<Object> myParams = new ArrayList<Object>();
  @NotNull
  private final List<String> myParamsWithoutIndex = new ArrayList<String>();
  @Nullable
  private String myClassName;
  @Nullable
  private String myMethodName;
  @Nullable
  private String mySuite;
  private long myDuration;
  @NotNull
  private Status myStatus = Status.SKIP;
  @Nullable
  private String myFailureType;
  @Nullable
  private String myFailureMessage;
  @Nullable
  private String myFailureStackTrace;
  private boolean myConfig;


  @NotNull
  public Status getStatus() {
    return myStatus;
  }

  public void setStatus(@Nullable final String status) {
    myStatus = Status.of(status);
  }

  @NotNull
  public String getTestMessage() {
    return StringUtil.join(myTestMessage, "\n");
  }

  public void appendMessageLine(@NotNull final String line) {
    myTestMessage.add(line);
  }

  @Nullable
  public String getClassName() {
    return myClassName;
  }

  public void setClassName(@Nullable final String className) {
    myClassName = className;
  }

  @Nullable
  public String getMethodName() {
    return myMethodName;
  }

  public void setMethodName(@Nullable final String methodName) {
    myMethodName = methodName;
  }

  @Nullable
  public String getSuite() {
    return mySuite;
  }

  public void setSuite(@Nullable final String suite) {
    mySuite = suite;
  }

  public List<String> getParams() {
    // merge params with index and without index
    List<String> result = new ArrayList<String>(myParams.size() + myParamsWithoutIndex.size());
    int idx = 0;
    for (int i = 0; i < myParams.size(); i++) {
      if (myParams.get(i) == EMPTY_PARAM) {
        if (myParamsWithoutIndex.size() > idx) {
          result.add(myParamsWithoutIndex.get(idx++));
        } else {
          result.add("null");
        }
      } else {
        result.add(i, (String)myParams.get(i));
      }
    }
    for (int i = idx; i < myParamsWithoutIndex.size(); i++) {
      result.add(myParamsWithoutIndex.get(i));
    }
    return result;
  }

  public void addParam(@Nullable final String index, @Nullable final String value) {
    String trimValue = value == null ? "" : '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    if (!ParserUtils.isNumber(index)) {
      myParamsWithoutIndex.add(trimValue);
    } else {
      try {
        int idx = Integer.parseInt(index);
        while (myParams.size() <= idx) {
          myParams.add(EMPTY_PARAM);
        }
        myParams.set(idx, trimValue);
      } catch (NumberFormatException e) {
        myParamsWithoutIndex.add(trimValue);
      }
    }
  }

  public long getDuration() {
    return myDuration;
  }

  public void setDuration(final long duration) {
    myDuration = duration;
  }

  @Nullable
  public String getFailureType() {
    return myFailureType;
  }

  public void setFailureType(@Nullable final String failureType) {
    myFailureType = failureType;
  }

  @NotNull
  public String getFailureMessage() {
    return myFailureMessage == null ? "" : myFailureMessage;
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

  public boolean isConfig() {
    return myConfig;
  }

  public void setConfig(@Nullable String isConfig) {
    myConfig = isConfig != null && isConfig.equalsIgnoreCase("true");
  }

  public enum Status {
    PASS, FAIL, SKIP;

    public static Status of(@Nullable String status) {
      if (status == null) {
        return SKIP;
      }
      status = status.toUpperCase();
      for (Status val: values()) {
        if (val.name().equals(status)) {
          return val;
        }
      }
      return SKIP;
    }
  }
}