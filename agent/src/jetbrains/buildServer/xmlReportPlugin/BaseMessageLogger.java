/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.problems.BuildProblemUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author vbedrosova
 */
public class BaseMessageLogger implements MessageLogger {
  @NotNull
  protected final BuildProgressLogger myLogger;
  @NotNull
  protected final String myBuildProblemType;
  @NotNull
  protected final String myBaseFolder;

  public BaseMessageLogger(@NotNull final BuildProgressLogger logger, @NotNull final String buildProblemType, @NotNull final String baseFolder) {
    myLogger = logger;
    myBuildProblemType = buildProblemType;
    myBaseFolder = baseFolder;
  }

  @Override
  public void info(@NotNull final String message) {
    myLogger.message(makeRelativePaths(message));
  }

  @Override
  public void warning(@NotNull final String message) {
    myLogger.warning(makeRelativePaths(message));
  }

  @Override
  public void error(@NotNull final String message) {
    myLogger.error(makeRelativePaths(message));
  }

  @Override
  public void failure(@NotNull final String message) {
    myLogger.error(message);
    myLogger.logBuildProblem(BuildProblemUtil.createBuildProblem(myBuildProblemType, makeRelativePaths(message), myBaseFolder));
  }

  @NotNull
  protected String makeRelativePaths(@NotNull final String message) {
    return message.replace(myBaseFolder.replace("\\", "/") + "/", "").replace(myBaseFolder.replace("/", "\\") + "\\", "");
  }
}
