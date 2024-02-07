

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Collection;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.utils.LoggingUtils;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:17
 */
public class MonitorRulesCommand {
  public interface MonitorRulesParameters {
    @NotNull Rules getRules();

    @NotNull String getType();

    boolean isParseOutOfDate();

    long getStartTime();

    @NotNull BuildProgressLogger getThreadLogger();

    boolean isReparseUpdated();
  }

  public interface MonitorRulesListener {
    void modificationDetected(@NotNull File file);
  }

  @NotNull
  private final MonitorRulesParameters myParameters;

  @NotNull
  private final ReportStateHolder myReportStateHolder;

  @NotNull
  private final MonitorRulesListener myListener;
  private final boolean myQuietMode;

  private boolean myFirstRun;

  public MonitorRulesCommand(@NotNull MonitorRulesParameters parameters,
                             @NotNull ReportStateHolder reportStateHolder,
                             final boolean quietMode,
                             @NotNull MonitorRulesListener listener) {
    myParameters = parameters;
    myReportStateHolder = reportStateHolder;
    myListener = listener;
    myQuietMode = quietMode;

    myFirstRun = true;
  }

  public void run() {
    if (myFirstRun) {
      if (!myQuietMode) {
        logWatchingPaths();
      }

      myFirstRun = false;
    }

    monitorRules(
      new MonitorRulesFileProcessor() {
        public void processFile(@NotNull File file) {
          if (acceptFile(file)) {

            final long fileLastModified = file.lastModified();
            final long fileLength = file.length();

            if (timeConstraintsSatisfied(fileLastModified)) {
              switch (myReportStateHolder.getReportState(file)) {
                case ON_PROCESSING:
                  return;
                case UNKNOWN:
                  myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.ON_PROCESSING, fileLastModified, fileLength);
                  modificationDetected(file);
                  return;
                case PROCESSED:
                  if (!myParameters.isReparseUpdated()) return;
                case ERROR:
                case OUT_OF_DATE:
                  final Long lastModified = myReportStateHolder.getLastModified(file);
                  final Long length = myReportStateHolder.getLength(file);

                  assert lastModified != null;
                  assert length != null;

                  if (fileLastModified > lastModified || fileLength > length) {
                    myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.ON_PROCESSING, fileLastModified, fileLength);
                    modificationDetected(file);
                  }
              }
            } else {
              myReportStateHolder.setReportState(file, ReportStateHolder.ReportState.OUT_OF_DATE, fileLastModified, fileLength);
            }
          }
        }
      }
    );
  }

  public void logWatchingPaths() {
    LoggingUtils.logInTarget(LoggingUtils.getTypeDisplayName(myParameters.getType()) + " report watcher",
      new Runnable() {
        public void run() {
          String message = "Watching paths:";
          final Collection<String> rules = myParameters.getRules().getBody();
          if (rules.isEmpty()) {
            message += " <no paths>";
            LoggingUtils.warn(message, getThreadLogger());
          } else {
            LoggingUtils.message(message, getThreadLogger());
            for (String rule : rules) {
              LoggingUtils.message(rule, getThreadLogger());
            }
          }
        }
      }, getThreadLogger());
  }

  @NotNull
  private BuildProgressLogger getThreadLogger() {
    return myParameters.getThreadLogger();
  }

  private void modificationDetected(File file) {
    myListener.modificationDetected(file);
  }

  private interface MonitorRulesFileProcessor {
    void processFile(@NotNull File file);
  }

  private void monitorRules(@NotNull MonitorRulesFileProcessor monitorRulesFileProcessor) {
    for (File file : myParameters.getRules().collectFiles()) {
      monitorRulesFileProcessor.processFile(file);
    }
  }

  private boolean acceptFile(@NotNull File f) {
      return f.isFile() && f.canRead() && f.length() > 0;
  }

  private boolean timeConstraintsSatisfied(long lastModified) {
    return myParameters.isParseOutOfDate() || isFresh(lastModified);
  }

  private boolean isFresh(long lastModified) {
    return lastModified/1000*1000 >= myParameters.getStartTime();
  }
}