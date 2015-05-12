/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 18.08.2009 15:33:49
 */
class TestName {
  private final String myTestId;
  private final String myDataRowInfo;

  public TestName(@NotNull final String testId, final String dataRowInfo) {
    myTestId = testId;
    myDataRowInfo = dataRowInfo;
  }

  public TestName(final String testId) {
    this(testId, null);
  }

  @NotNull
  public String getTestId() {
    return myTestId;
  }

  public String presentName(@Nullable final String testName) {
    final String name = testName == null ? myTestId : testName;
    if (myDataRowInfo == null) {
      return name;
    } else {
      return name + "(" + myDataRowInfo + ")";
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TestName testName = (TestName)o;
    return !(myDataRowInfo != null ? !myDataRowInfo.equals(testName.myDataRowInfo) : testName.myDataRowInfo != null) &&
           myTestId.equals(testName.myTestId);
  }

  @Override
  public int hashCode() {
    int result = myTestId.hashCode();
    result = 31 * result + (myDataRowInfo != null ? myDataRowInfo.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    //This method is used in test
    return (myDataRowInfo == null ? "" : "(" + myDataRowInfo + ")") + myTestId;
  }
}
