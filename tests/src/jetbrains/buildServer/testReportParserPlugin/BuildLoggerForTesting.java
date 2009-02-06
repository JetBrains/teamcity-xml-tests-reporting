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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.SimpleBuildLogger;
import org.jetbrains.annotations.NotNull;

public class BuildLoggerForTesting implements SimpleBuildLogger {
  private final StringBuilder myText;

  public BuildLoggerForTesting(final StringBuilder text) {
    myText = text;
  }

  public void warning(@NotNull final String message) {
    myText.append("WARNING: ");
    myText.append(message);
    myText.append("\n");
  }

  public void exception(final Throwable th) {
    myText.append("EXCEPTION: ");
    myText.append(th.toString());
    myText.append("\n");
  }

  public void progressMessage(final String message) {
    myText.append("PROGRESS: ");
    myText.append(message);
    myText.append("\n");
  }

  public void message(final String message) {
    myText.append("MESSAGE: ");
    myText.append(message);
    myText.append("\n");
  }

  public void error(@NotNull final String message) {
    myText.append("ERROR: ");
    myText.append(message);
    myText.append("\n");
  }

  public StringBuilder getText() {
    return myText;
  }
}
