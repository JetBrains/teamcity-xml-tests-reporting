package jetbrains.buildServer.xmlReportPlugin.inspections;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.messages.Status;
import jetbrains.buildServer.messages.serviceMessages.BuildStatus;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 18:01
 */
public class InspectionsResultProcessor implements ResultProcessor {
  public void processResult(@NotNull File file,
                            @NotNull ParsingResult result,
                            @NotNull ParseParameters parameters) {
    final InspectionsParsingResult inspectionsParsingResult
      = (InspectionsParsingResult) result;

    String message = file + " report processed";
    if (inspectionsParsingResult.getErrors() > 0) {
      message = message.concat(": " + inspectionsParsingResult.getErrors() + " error(s)");
    }
    if (inspectionsParsingResult.getWarnings() > 0) {
      message = message.concat(": " + inspectionsParsingResult.getWarnings() + " warning(s)");
    }
    if (inspectionsParsingResult.getInfos() > 0) {
      message = message.concat(": " + inspectionsParsingResult.getInfos() + " info message(s)");
    }
    if (parameters.isVerbose()) {
      parameters.getThreadLogger().message(message);
    }
    LoggingUtils.LOG.debug(message);
  }

  public void processTotalResult(@NotNull ParsingResult result, @NotNull ParseParameters parameters) {
    final InspectionsParsingResult inspectionsParsingResult = (InspectionsParsingResult) result;

    final BuildProgressLogger logger = parameters.getThreadLogger();

    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters.getParameters());
    if ((errorLimit != -1) && (inspectionsParsingResult.getErrors() > errorLimit)) {
      logger.error("Errors limit reached: found " + inspectionsParsingResult.getErrors() + " errors, limit " + errorLimit);
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters.getParameters());
    if ((warningLimit != -1) && (inspectionsParsingResult.getWarnings() > warningLimit)) {
      logger.error("Warnings limit reached: found " + inspectionsParsingResult.getWarnings() + " warnings, limit " + warningLimit);
      limitReached = true;
    }

    if (limitReached) {
      logger.message(new BuildStatus(generateBuildStatus(inspectionsParsingResult.getErrors(), inspectionsParsingResult.getWarnings(), inspectionsParsingResult.getInfos()), Status.FAILURE).asString());
    }
  }

  private static String generateBuildStatus(int errors, int warnings, int infos) {
    return "Errors: " + errors + ", warnings: " + warnings + ", information: " + infos;
  }
}
