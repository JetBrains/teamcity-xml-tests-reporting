/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
