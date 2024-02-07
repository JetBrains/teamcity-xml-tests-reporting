

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Map;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 15:18
 */
public interface ParseParameters {
  boolean isVerbose();

  @NotNull
  BuildProgressLogger getThreadLogger();

  @NotNull
  InspectionReporter getInspectionReporter();

  @NotNull
  DuplicationReporter getDuplicationReporter();

  @NotNull
  TestReporter getTestReporter();

  @NotNull
  Map<String, String> getParameters();

  @NotNull
  String getType();

  @NotNull
  File getCheckoutDir();
}