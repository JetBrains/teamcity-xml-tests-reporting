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

package jetbrains.buildServer.testReportParserPlugin;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import org.jetbrains.annotations.NotNull;


public class TestReportLogger {
  private static final String PLUGIN_LOG_PREFIX = "xml-report-plugin: ";
  private static final Logger AGENT_LOG = Logger.getInstance(TestReportParserPlugin.class.getName());

  private final BaseServerLoggerFacade myBuildLogger;
  private final boolean myVerboseOutput;

  public TestReportLogger(@NotNull BaseServerLoggerFacade buildLogger, boolean verboseOutput) {
    myBuildLogger = buildLogger;
    myVerboseOutput = verboseOutput;
  }

  public static String createLogMessage(String message) {
    return PLUGIN_LOG_PREFIX + message;
  }

  public void debugToAgentLog(String message) {
    AGENT_LOG.debug(createLogMessage(message));
  }

  public void error(String message) {
    final String m = createLogMessage(message);
    AGENT_LOG.debug(m);
    if (myVerboseOutput) {
      myBuildLogger.error(m);
    }
  }

  public void message(String message) {
    final String m = createLogMessage(message);
    AGENT_LOG.debug(m);
    if (myVerboseOutput) {
      myBuildLogger.message(m);
    }
  }

  public void warning(String message) {
    final String m = createLogMessage(message);
    AGENT_LOG.debug(m);
    if (myVerboseOutput) {
      myBuildLogger.warning(m);
    }
  }

  public void exception(Throwable throwable) {
    AGENT_LOG.debug(throwable);
    if (myVerboseOutput) {
      myBuildLogger.exception(throwable);
    }
  }

  public void logSystemOut(String message) {
    myBuildLogger.message("[System out]\n" + message);
  }

  public void logSystemError(String message) {
    myBuildLogger.warning("[System error]\n" + message);
  }

  public BaseServerLoggerFacade getBuildLogger() {
    return myBuildLogger;
  }
}
