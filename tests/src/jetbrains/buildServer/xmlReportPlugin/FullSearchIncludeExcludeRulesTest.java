package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * User: Victory.Bedrosova
 * Date: 3/27/13
 * Time: 2:57 PM
 */

@Test
public class FullSearchIncludeExcludeRulesTest extends BaseRulesTest {
  @NotNull
  @Override
  protected Rules createRules(@NotNull final File baseDir, @NotNull final List<String> rules) {
    return new FullSearchIncludeExcludeRules(baseDir, rules);
  }

  @Override
  protected boolean starsMatchRoot() {
    return false;
  }
}
