

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 19:28
 */

/**
 * Holds information about current state of reports
 */
public interface ReportStateHolder {

  enum ReportState {
    /*
    * UNKNOWN brand new report
    * ON_PROCESSING report was added, but not processed yet
    * PROCESSED report was successfully processed
    * ERROR report wasn't fully processed due to some problems
    * OUT_OF_DATE report is out-of-date
    */
    UNKNOWN, ON_PROCESSING, PROCESSED, ERROR, OUT_OF_DATE
  }

  @NotNull ReportState getReportState(@NotNull File report);
  @Nullable Long getLastModified(@NotNull File report);
  @Nullable Long getLength(@NotNull File report);
  void setReportState(@NotNull File report, @NotNull ReportState state, long lastModified, long length);
}