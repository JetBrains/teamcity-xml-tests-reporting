/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import jetbrains.buildServer.xmlReportPlugin.ParseParameters;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import jetbrains.buildServer.xmlReportPlugin.ParserFactory;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 17:08
 */
public class PmdCpdFactory implements ParserFactory {
  @NotNull
  @Override
  public String getType() {
    return "pmdCpd";
  }

  @NotNull
  public Parser createParser(@NotNull ParseParameters parameters) {
    return new PmdCpdReportParser(parameters.getDuplicationReporter(), parameters.getCheckoutDir());
  }

  @NotNull
  public ParsingResult createEmptyResult() {
    return new PmdCpdParsingResult();
  }
}
