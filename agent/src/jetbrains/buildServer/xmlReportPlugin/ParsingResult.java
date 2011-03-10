/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import java.io.File;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 22:29
 */
public interface ParsingResult {
  /**
   * Accumulate the given result into current
   * @param parsingResult te result to accumulate
   */
  void accumulate(@NotNull ParsingResult parsingResult);

  /**
   * Processes result of parsing the file
   * @param file file
   * @param parameters additional parameters
   */
  void logAsFileResult(@NotNull File file,
                       @NotNull ParseParameters parameters);

  /**
   * Processes total parsing result
   * @param parameters additional parameters
   */
  void logAsTotalResult(@NotNull ParseParameters parameters);


  /**
   * Returns problem that occurred during parsing if any
   * @return Problem or null if no problem took place during parsing
   */
  @Nullable
  Throwable getProblem();


  /**
   * Sets problem that occurred during parsing if any
   * @param problem the problem
   */
  void setProblem(@NotNull Throwable problem);
}
