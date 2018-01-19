/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 27.10.2008 12:10:37
 */
class DurationParser {
  private static final Logger LOG = Logger.getLogger(DurationParser.class.getName());

  public long parseTestDuration(@NotNull final String duration) {
    try {
      String[] durations = duration.split(":");
      if (durations.length != 3) {
        LOG.warn("Failed to parse duration string: " + duration + ". Format unexpected.");
        return -1;
      }
      final double seconds = Double.parseDouble(durations[2]);
      final double minutes = Double.parseDouble(durations[1]);
      final double hours = Double.parseDouble(durations[0]);

      return (long)(Math.ceil(1000 * seconds) + 1000 * (60 * (minutes + 60 * hours)));
    } catch (NumberFormatException e) {
      LOG.warn("Failed to parse duration string: " + duration + ". " + e.getMessage(), e);
    }
    return -1;
  }

  public long parseTestDuration(@NotNull final String startTime, @NotNull final String endTime) {
    Long start = parseDotNetTime(startTime);
    Long finish = parseDotNetTime(endTime);

    if (start == null || finish == null) return -1;
    return finish - start;
  }

  @Nullable
  private Long parseDotNetTime(@NotNull String time) {
    //NOTE: We ommit timezone because we need only a duration. Incoming format is like: 2010-02-12T14:44:45.9393792+00:00
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    try {
      final Date date = sdf.parse(time);
      return date != null ? date.getTime() : null;
    } catch (ParseException e) {
      return null;
    }
  }
}
