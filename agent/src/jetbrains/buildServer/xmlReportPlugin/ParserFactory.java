/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.AgentExtension;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.01.11
 * Time: 23:19
 */
public interface ParserFactory extends AgentExtension {
  String TEAMCITY_PROPERTY_STAGE_PREFIX = "teamcity.xmlReport.parsingStage";

  @NotNull String getType();
  @NotNull Parser createParser(@NotNull ParseParameters parameters);
  @NotNull ParsingResult createEmptyResult();
  @NotNull ParsingStage getParsingStage();

  enum ParsingStage{
    RUNTIME, BEFORE_FINISH;

    @Nullable
    public static ParsingStage of(@Nullable final String name) {
      if (StringUtil.isEmptyOrSpaces(name)) return null;
      for(ParsingStage stage: values()) {
        if (stage.name().equalsIgnoreCase(name)) {
          return stage;
        }
      }
      return null;
    }
  }
}
