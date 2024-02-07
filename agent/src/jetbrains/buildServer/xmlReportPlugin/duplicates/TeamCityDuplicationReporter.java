

package jetbrains.buildServer.xmlReportPlugin.duplicates;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.duplicator.DuplicateInfo;
import jetbrains.buildServer.xmlReportPlugin.BaseMessageLogger;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 08.02.11
 * Time: 16:12
 */
public class TeamCityDuplicationReporter extends BaseMessageLogger implements DuplicationReporter {
  @NotNull
  private final DuplicatesReporter myDuplicatesReporter;

  public TeamCityDuplicationReporter(@NotNull DuplicatesReporter duplicatesReporter,
                                     @NotNull BuildProgressLogger logger,
                                     @NotNull String baseFolder,
                                     @NotNull String buildProblemType) {
    super(logger, buildProblemType, baseFolder);
    myDuplicatesReporter = duplicatesReporter;
  }

  public void startDuplicates() {
    myDuplicatesReporter.startDuplicates();
  }

  public void reportDuplicate(@NotNull DuplicationResult duplicate) {
    final List<DuplicateInfo.Fragment> fragmentsList = new ArrayList<DuplicateInfo.Fragment>();

    for (DuplicatingFragment fragment : duplicate.getFragments()) {
      fragmentsList.add(new DuplicateInfo.Fragment(fragment.getHash(), fragment.getPath(), fragment.getLine(),
        new DuplicateInfo.LineOffset(fragment.getLine(), fragment.getLine() + duplicate.getLines())));
    }

    myDuplicatesReporter.addDuplicate(new DuplicateInfo(duplicate.getHash(), duplicate.getTokens(), fragmentsList.toArray(new DuplicateInfo.Fragment[0])));
  }

  public void finishDuplicates() {
    myDuplicatesReporter.finishDuplicates();
  }
}