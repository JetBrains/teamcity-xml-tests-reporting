package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.FileRule;
import jetbrains.buildServer.vcs.FileRuleSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:19
 */
public class Rules extends FileRuleSet<FileRule, FileRule> {
  @NotNull
  private final File myBaseDir;

  private Set<File> myRootIncludePaths;

  public Rules(@NotNull Collection<String> body,
               @NotNull File baseDir) {
    super(new ArrayList<String>(body));
    myBaseDir = baseDir;
  }

  @Override
  protected void doPostInitProcess(@NotNull List<FileRule> includeRules, @NotNull List<FileRule> excludeRules) {
    sortByFrom(includeRules, true);
  }

  @Override
  protected FileRule createNewIncludeRule(@NotNull String line) {
    return createRule(line, true);
  }

  private FileRule createRule(@NotNull String line, boolean isInclude) {
    return new FileRule(line, this, isInclude);
  }

  @Override
  protected FileRule createExcludeRule(@NotNull String line) {
    return createRule(line, false);
  }

  @Override
  protected FileRule createNewIncludeRule(@NotNull FileRule includeRule) {
    return createNewIncludeRule(includeRule.getFrom());
  }

  @Override
  protected FileRule createNewExcludeRule(@NotNull FileRule excludeRule) {
    return createExcludeRule(excludeRule.getFrom());
  }

  private Set<File> initRootIncludePaths() {
    final ArrayList<FileRule> resultRules = new ArrayList<FileRule>();
    resultRules.addAll(getIncludeRules());

    final Set<FileRule> processedRules = new HashSet<FileRule>();

    for (Iterator<FileRule> iterator = resultRules.iterator(); iterator.hasNext();) {
      FileRule rule = iterator.next();

      if (!shouldInclude(rule.getFrom())) {
        iterator.remove();
        continue;
      }

      for (FileRule processed : processedRules) {
        if (isSubDir(rule.getFrom(), processed.getFrom())) {
          iterator.remove();
          break;
        }
      }

      processedRules.add(rule);
    }

    myRootIncludePaths = new HashSet<File>();
    for (FileRule rule : resultRules) {
      myRootIncludePaths.add(FileUtil.resolvePath(myBaseDir, rule.getFrom()));
    }
    return myRootIncludePaths;
  }

  @NotNull
  public Set<File> getPaths() {
    if (myRootIncludePaths == null) initRootIncludePaths();
    return myRootIncludePaths;
  }

  public boolean shouldInclude(@NotNull File path) {
    return super.shouldInclude(FileUtil.getRelativePath(myBaseDir, path));
  }
}
