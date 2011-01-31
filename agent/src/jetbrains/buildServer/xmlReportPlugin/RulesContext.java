package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;

/**
 * User: vbedrosova
 * Date: 20.01.11
 * Time: 12:59
 */
public class RulesContext {
  @NotNull
  private final XmlReportPlugin.RulesData myRulesData;

  @NotNull
  private final RulesFileStateHolder myRulesFilesState;

  @NotNull
  private final Map<File, ParsingResult> myFailedToParse;

  @NotNull
  private ScheduledFuture myMonitorTask;

  @NotNull
  private final List<Future> myParseTasks = new ArrayList<Future>();

  @NotNull
  private MonitorRulesCommand myMonitorRulesCommand;

  public RulesContext(@NotNull XmlReportPlugin.RulesData rulesData,
                      @NotNull RulesFileStateHolder rulesFilesState,
                      @NotNull Map<File, ParsingResult> failedToParse) {
    myRulesData = rulesData;
    myRulesFilesState = rulesFilesState;
    myFailedToParse = failedToParse;
  }

  public void setMonitorTask(@NotNull ScheduledFuture monitorTask) {
    myMonitorTask = monitorTask;
  }

  public void addParseTask(@NotNull Future parseTask) {
    myParseTasks.add(parseTask);
  }

  @NotNull
  public ScheduledFuture getMonitorTask() {
    return myMonitorTask;
  }

  @NotNull
  public List<Future> getParseTasks() {
    return Collections.unmodifiableList(myParseTasks);
  }

  public void clearParseTasks() {
    myParseTasks.clear();
  }

  public void setMonitorRulesCommand(@NotNull MonitorRulesCommand monitorRulesCommand) {
    myMonitorRulesCommand = monitorRulesCommand;
  }

  @NotNull
  public MonitorRulesCommand getMonitorRulesCommand() {
    return myMonitorRulesCommand;
  }

  @NotNull
  public XmlReportPlugin.RulesData getRulesData() {
    return myRulesData;
  }

  @NotNull
  public RulesFileStateHolder getRulesFilesState() {
    return myRulesFilesState;
  }

  @NotNull
  public Map<File, ParsingResult> getFailedToParse() {
    return myFailedToParse;
  }
}
