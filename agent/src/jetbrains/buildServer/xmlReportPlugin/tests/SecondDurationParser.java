

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