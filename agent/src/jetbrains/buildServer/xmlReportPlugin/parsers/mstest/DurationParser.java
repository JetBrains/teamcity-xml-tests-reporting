package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import com.intellij.openapi.diagnostic.Logger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 27.10.2008 12:10:37
 */
class DurationParser {
  private static final Logger LOG = Logger.getInstance(DurationParser.class.getName());

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
