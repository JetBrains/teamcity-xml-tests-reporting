

package jetbrains.buildServer.xmlReportPlugin.tests;

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 01.03.11
 * Time: 19:11
 */
public interface DurationParser {
  long parseTestDuration(@Nullable String duration);
}