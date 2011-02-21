package jetbrains.buildServer.xmlReportPlugin.duplicates;

import java.io.File;
import java.util.ArrayList;
import jetbrains.buildServer.duplicator.DuplicateInfo;
import jetbrains.buildServer.xmlReportPlugin.PathUtils;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatesReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatingFragment;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 08.02.11
 * Time: 16:12
 */
public class XmlReportPluginDuplicatesReporter implements DuplicatesReporter {
  @NotNull
  private final jetbrains.buildServer.agent.duplicates.DuplicatesReporter myDuplicatesReporter;
  @NotNull
  private final File myBaseFolder;

  public XmlReportPluginDuplicatesReporter(@NotNull jetbrains.buildServer.agent.duplicates.DuplicatesReporter duplicatesReporter, @NotNull File baseFolder) {
    myDuplicatesReporter = duplicatesReporter;
    myBaseFolder = baseFolder;
  }

  public void startDuplicates() {
    myDuplicatesReporter.startDuplicates();
  }

  public void reportDuplicate(@NotNull DuplicationResult duplicate) {
    final ArrayList<DuplicateInfo.Fragment> fragmentsList = new ArrayList<DuplicateInfo.Fragment>();

    for (DuplicatingFragment fragment : duplicate.getFragments()) {
      fragmentsList.add(new DuplicateInfo.Fragment(duplicate.getHash(),
        PathUtils.getRelativePath(myBaseFolder.getAbsolutePath(), fragment.getPath()), fragment.getLine(),
        new DuplicateInfo.LineOffset(fragment.getLine(), fragment.getLine() + duplicate.getLines())));
    }

    myDuplicatesReporter.addDuplicate(new DuplicateInfo(duplicate.getHash(), duplicate.getTokens(), fragmentsList.toArray(new DuplicateInfo.Fragment[fragmentsList.size()])));
  }

  public void finishDuplicates() {
    myDuplicatesReporter.finishDuplicates();
  }
}
