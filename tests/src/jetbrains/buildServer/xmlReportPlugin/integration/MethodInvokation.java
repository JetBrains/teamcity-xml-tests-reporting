/*
 * Copyright 2008 JetBrains s.r.o.
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
package jetbrains.buildServer.xmlReportPlugin.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;


public class MethodInvokation {
  public static final Object ANY_VALUE = new Object();

  private final String myMethodName;
  private final List<Object> myMethodParams;
  private boolean myInvoked;

  public MethodInvokation(@NotNull final String methodName, @NotNull final List<Object> methodParams) {
    myMethodName = methodName;
    myMethodParams = methodParams;
    myInvoked = false;
  }

  public String getMethodName() {
    return myMethodName;
  }

  public List<Object> getMethodParams() {
    return Collections.unmodifiableList(myMethodParams);
  }

  public void setInvoked() {
    myInvoked = true;
  }

  public boolean wasInvoked() {
    return myInvoked;
  }
}
