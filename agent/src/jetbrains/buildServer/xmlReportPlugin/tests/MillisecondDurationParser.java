

package jetbrains.buildServer.xmlReportPlugin.tests;

import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 01.03.11
 * Time: 19:03
 */
public class MillisecondDurationParser implements DurationParser {
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MillisecondDurationParser.class);

  public long parseTestDuration(@Nullable String duration) {
    if (!ParserUtils.isNumber(duration)) {
      LOG.warn("Unable to parse execution time string " + duration);
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