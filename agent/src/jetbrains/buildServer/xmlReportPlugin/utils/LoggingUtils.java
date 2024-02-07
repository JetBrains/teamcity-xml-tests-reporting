

package jetbrains.buildServer.xmlReportPlugin.utils;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 12.01.11
 * Time: 14:46
 */
public class LoggingUtils {
  public static final Logger LOG = Logger.getInstance(XmlReportPlugin.class.getName());

  public static void logInTarget(@NotNull String target,
                                 @NotNull Runnable activity,
                                 @NotNull BuildProgressLogger logger) {
    LOG.info(target);
    logger.targetStarted(target);
    activity.run();
    logger.targetFinished(target);
  }

  public static void verbose(@NotNull final String message, @NotNull final BuildProgressLogger logger) {
    LOG.debug(message);
    logger.debug(message);
  }

  public static void message(@NotNull String message, @NotNull BuildProgressLogger logger) {
    LOG.info(message);
    logger.message(message);
  }

  public static void warn(@NotNull String message, @NotNull BuildProgressLogger logger) {
    LOG.warn(message);
    logger.warning(message);
  }

  public static void error(@NotNull String message, @NotNull BuildProgressLogger logger) {
    LOG.warn(message);
    logger.error(message);
  }

  public static void logError(@NotNull String error,
                              @Nullable Throwable throwable,
                              @NotNull BuildProgressLogger logger,
                              boolean addStackTraceToBuildLog) {
    error(error, logger);
    if (throwable != null) {
      if (addStackTraceToBuildLog) {
        logger.exception(throwable);
      } else {
        LOG.warn(throwable);
      }
    }
  }

  public static void logException(@NotNull String message,
                                  @NotNull Throwable throwable,
                                  @NotNull BuildProgressLogger logger) {
    logger.error(message);
    logger.exception(throwable);
    LOG.warn(message, throwable);
  }

  public static String getTypeDisplayName(@NotNull String type) {
    return XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(type);
  }
}