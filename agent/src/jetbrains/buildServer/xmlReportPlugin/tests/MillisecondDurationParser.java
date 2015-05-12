/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 01.03.11
 * Time: 19:03
 */
public class MillisecondDurationParser implements DurationParser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(SecondDurationParser.class);

  public long parseTestDuration(@Nullable String duration) {
    if (duration == null || "".equals(duration)) {
      return 0L;
    }
    try {
      return Long.parseLong(duration);
    } catch (NumberFormatException e) {
      LOG.warn("Unable to parse execution time string " + duration, e);
      return 0L;
    }
  }
}
