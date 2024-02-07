

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.pathMatcher.AntPatternFileCollector;
import org.jetbrains.annotations.NotNull;

/**
 * User: Victory.Bedrosova
 * Date: 3/27/13
 * Time: 2:23 PM
 */
public class OptimizingIncludeExcludeRules implements Rules {
  @NotNull
  private final File myBaseDir;
  @NotNull
  private final Collection<String> myBody;

  public OptimizingIncludeExcludeRules(@NotNull final File baseDir, @NotNull Collection<String> body) {
    myBaseDir = baseDir;
    myBody = body;
  }

  @NotNull
  @Override
  public Collection<File> getPaths() {
    return CollectionsUtil.convertAndFilterNulls(myBody, new Converter<File, String>() {
      @Override
      public File createFrom(@NotNull final String source) {
        return isIncludeRule(source) ? new File(getRulePath(source)) : null;
      }
    });
  }

  private static boolean isIncludeRule(@NotNull String rule) {
    return !rule.startsWith("-:");
  }

  @NotNull
  private static String getRulePath(@NotNull String rule) {
    if (rule.startsWith("+:") || rule.startsWith("-:")) {
      return rule.substring(2);
    }
    return rule;
  }

  @NotNull
  @Override
  public Collection<String> getBody() {
    return myBody;
  }

  @NotNull
  @Override
  public Collection<File> collectFiles() {
    return myBaseDir.exists() ? AntPatternFileCollector.scanDir(myBaseDir, getRulesArray(), getScanOptions()) : Collections.emptyList();
  }

  @NotNull
  private String[] getRulesArray() {
    return  myBody.toArray(new String[0]);
  }

  @NotNull
  private AntPatternFileCollector.ScanOption[] getScanOptions() {
    return new AntPatternFileCollector.ScanOption[]{AntPatternFileCollector.ScanOption.USE_RULE_STRICTNESS, AntPatternFileCollector.ScanOption.ALLOW_EXTERNAL_SCAN};
  }
}