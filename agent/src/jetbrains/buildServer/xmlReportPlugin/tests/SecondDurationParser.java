/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.tests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 22.02.11
 * Time: 18:02
 */
public class SecondDurationParser implements DurationParser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(SecondDurationParser.class);

  private static final String COMMA = ",";
  private static final String DOT = ".";
  private static final String MARK = "'";
  private static final String NBSP = "\u00A0";

  public long parseTestDuration(@Nullable String duration) {
    if (duration == null || "".equals(duration)) {
      return 0L;
    }
    try {
      return Math.round(Double.parseDouble(getUniformTimeStr(duration)) * 1000.0);
    } catch (NumberFormatException e) {
      LOG.warn("Unable to parse execution time string " + duration, e);
      return 0L;
    }
  }

  private String getUniformTimeStr(@NotNull String str) {
    final int commaIndex = str.lastIndexOf(COMMA);
    final int dotIndex = str.lastIndexOf(DOT);
    String result;
    if (commaIndex > dotIndex) {
      result = str.replace(DOT, "").replace(COMMA, DOT);
    } else if (commaIndex < dotIndex) {
      result = str.replace(COMMA, "");
    } else {
      result = str;
    }
    return result.replace(MARK, "").replace(NBSP, "");
  }
}
