

package jetbrains.buildServer.xmlReportPlugin.inspections;

import jetbrains.buildServer.xmlReportPlugin.MessageLogger;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 13:24
 */
public interface InspectionReporter extends MessageLogger {
  /**
   * Report inspection instance
   *
   * @param inspection Inspection description
   */
  void reportInspection(@NotNull InspectionResult inspection);

  /**
   * Report inspection description
   *
   * @param inspectionType Inspection type description
   */
  void reportInspectionType(@NotNull InspectionTypeResult inspectionType);
}