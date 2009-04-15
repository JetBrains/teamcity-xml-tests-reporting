/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;

public class BaseServerLoggerFacadeForTesting extends BaseServerLoggerFacade {
  private List<MethodInvokation> myMethodSequence = null;
  private List<String> myNotControlledMethods = new LinkedList<String>();


  private Iterator<MethodInvokation> myCurrent = null;
  private final List<UnexpectedInvokationException> myFailures;

  public BaseServerLoggerFacadeForTesting(List<UnexpectedInvokationException> failures) {
    myFailures = failures;
  }

  public void setExpectedSequence(List<MethodInvokation> sequence) {
    myMethodSequence = new LinkedList<MethodInvokation>(sequence);
    myCurrent = myMethodSequence.iterator();
  }

  public void addNotControlledMethod(String method) {
    myNotControlledMethods.add(method);
  }

  public static String currentMethod() {
    final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
//        for (int i = 0; i < ste.length; ++i) {
//            System.out.println(ste[i]);
//        }
    return ste[4].getMethodName();
  }

  private MethodInvokation getNextExpectedInvokation() {
    if (myCurrent.hasNext()) {
      return myCurrent.next();
    }
    return null;
  }

  private boolean isMethodUnderControl(String name) {
    if (myMethodSequence == null) {
      myFailures.add(new UnexpectedInvokationException(("No expected sequence specified")));
      return false;
    }
    if (myFailures.size() > 0) {
      return false;
    }
    if (myNotControlledMethods.contains(name)) {
      return false;
    }
    return true;
  }

  private String getFailureIfOccurs(List<Object> params) {
    final String name = currentMethod();
    if (!isMethodUnderControl(name)) {
      return null;
    }
//    System.out.println("call " + name);
    final MethodInvokation expected = getNextExpectedInvokation();
    if ((expected == null) || (!currentMethod().equals(expected.getMethodName()))) {
//      System.out.println("unexpected " + name);
      return "Unexpected method invokation: " + name;
    }
    expected.setInvoked();
    final List<Object> expectedParams = expected.getMethodParams();
    for (int i = 0; i < params.size(); ++i) {
      final Object expectedParam = expectedParams.get(i);
      final Object actualParam = params.get(i);
      if (MethodInvokation.ANY_VALUE.equals(expectedParam)) {
        continue;
      }
      if (!actualParam.equals(expectedParam)) {
        System.out.println("wrong param in " + name);
        return "Unexpected parameter value: <" + actualParam + "> in method: " + name;
      }
    }
    return null;
  }

  public void checkIfAllExpectedMethodsWereInvoked() {
    for (MethodInvokation invokation : myMethodSequence) {
      if (!invokation.wasInvoked()) {
        myFailures.add(new UnexpectedInvokationException("Method: " + invokation.getMethodName() + " not invoked"));
        break;
      }
    }
  }

  public void message(java.lang.String s) {
    List<Object> params = new ArrayList();
    params.add(s);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void logTestStarted(java.lang.String s, java.util.Date date) {
    List<Object> params = new ArrayList();
    params.add(s);
    params.add(date);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void logTestFinished(java.lang.String s, java.util.Date date) {
    List<Object> params = new ArrayList();
    params.add(s);
    params.add(date);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void warning(java.lang.String s) {
    List<Object> params = new ArrayList();
    params.add(s);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void error(java.lang.String s) {
    List<Object> params = new ArrayList();
    params.add(s);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }


  public void logSuiteStarted(java.lang.String s, java.util.Date date) {
    List<Object> params = new ArrayList();
    params.add(s);
    params.add(date);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void logSuiteFinished(java.lang.String s, java.util.Date date) {
    List<Object> params = new ArrayList();
    params.add(s);
    params.add(date);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void logTestFailed(java.lang.String s, java.lang.String s1, java.lang.String s2) {
    List<Object> params = new ArrayList();
    params.add(s);
    params.add(s1);
    params.add(s2);
    final String message = getFailureIfOccurs(params);
    if (message != null) {
      myFailures.add(new UnexpectedInvokationException(message));
    }
  }

  public void logTestStarted(java.lang.String s) {
  }

  private void updateAndLog(jetbrains.buildServer.messages.BuildMessage1 buildMessage1) { /* compiled code */ }

  public void logTestFinished(java.lang.String s) { /* compiled code */ }

  public void logTestIgnored(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void logSuiteStarted(java.lang.String s) { /* compiled code */ }

  public void logSuiteFinished(java.lang.String s) { /* compiled code */ }

  public void logTestStdOut(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void logTestStdErr(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void logComparisonFailure(java.lang.String s, java.lang.Throwable throwable, java.lang.String s1, java.lang.String s2) { /* compiled code */ }

  public void logTestFailed(java.lang.String s, java.lang.Throwable throwable) { /* compiled code */ }

  public void activityStarted(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void activityFinished(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void targetStarted(java.lang.String s) { /* compiled code */ }

  public void targetFinished(java.lang.String s) { /* compiled code */ }

  public void progressStarted(java.lang.String s) { /* compiled code */ }

  public void progressFinished() { /* compiled code */ }

  public void progressMessage(java.lang.String s) { /* compiled code */ }

  public void buildFailureDescription(java.lang.String s) { /* compiled code */ }

  public void exception(java.lang.Throwable throwable) { /* compiled code */ }

  public void flush() {
  }

  public void log(jetbrains.buildServer.messages.BuildMessage1 buildMessage1) {
  }

  public void flowStarted(java.lang.String s, java.lang.String s1) { /* compiled code */ }

  public void flowFinished(java.lang.String s) { /* compiled code */ }

  public void ensureFlow() { /* compiled code */ }

  public void buildFinished() { /* compiled code */ }

  public void logMessage(jetbrains.buildServer.messages.BuildMessage1 buildMessage1) { /* compiled code */ }
}