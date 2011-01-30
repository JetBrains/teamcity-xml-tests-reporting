package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.Map;

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
  BuildProgressLogger getInternalizingThreadLogger();

  @NotNull
  InspectionReporter getInspectionReporter();

  @NotNull
  DuplicatesReporter getDuplicatesReporter();

  @NotNull
  Map<String, String> getParameters();

  @NotNull
  XMLReader getXmlReader();

  @NotNull
  String getType();

  @NotNull
  File getCheckoutDir();

  @NotNull
  File getTempDir();
}
