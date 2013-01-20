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

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 11:52
 */

/**
 * Report parser
 */
public interface Parser {
  /**
   * Parses the specified file
   * @param file file to parse
   * @param prevResult previous parsing result if available
   * @return true if file is fully parsed and doesn't need more parsing, false otherwise
   * @throws ParsingException if parser comes across critical error
   */
  boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException;

  /**
   * Gets parser implementation specific parsing result
   * @return parsing result which can be null if parser hasn't yet parsed anything
   */
  @Nullable ParsingResult getParsingResult();
}
