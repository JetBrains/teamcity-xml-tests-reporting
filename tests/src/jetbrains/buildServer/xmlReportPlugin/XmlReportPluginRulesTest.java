package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.util.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * User: vbedrosova
 * Date: 10.12.10
 * Time: 19:32
 */
public class XmlReportPluginRulesTest extends TestCase {
  private File myCheckoutDir;

  @Before
  @Override
  protected void setUp() throws Exception {
    myCheckoutDir = FileUtil.createTempDirectory("", "");
  }

  @Test
  public void test_path_include() {
    final Rules rules = createRules("+:some/path");

    assertTrue(rules.shouldInclude(getFile("some/path")));
    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_path_include_dot() {
    final Rules rules = createRules("+:./some/path");

    assertTrue(rules.shouldInclude(getFile("some/path")));
    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_mask_include_content() {
    final Rules rules = createRules("+:some/path/*");

    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some/path/content/inner")));
    assertFalse(rules.shouldInclude(getFile("some/path")));
    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_mask_include_content_dot() {
    final Rules rules = createRules("+:./some/path/*");

    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some/path/content/inner")));
    assertFalse(rules.shouldInclude(getFile("some/path")));
    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_mask_include_content_recursive() {
    final Rules rules = createRules("+:some/path/**");

    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some/path")));
    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_mask_include_content_recursive_dot() {
    final Rules rules = createRules("+:./some/path/**");

    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some/path")));
    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_path_exclude_some_content() {
    final Rules rules = createRules("+:some/path", "-:some/path/content", "+:some/path/content/inner");

    assertTrue(rules.shouldInclude(getFile("some/path")));
    assertTrue(rules.shouldInclude(getFile("some/path/another")));
    assertTrue(rules.shouldInclude(getFile("some/path/another/content")));
    assertTrue(rules.shouldInclude(getFile("some/path/content/inner")));
    assertTrue(rules.shouldInclude(getFile("some/path/content/inner/content")));

    assertFalse(rules.shouldInclude(getFile("some/path/content")));
    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
  }

  @Test
  public void test_mask_exclude_some_content() {
    final Rules rules = createRules("+:some/path", "-:some/path/content/**");

    assertTrue(rules.shouldInclude(getFile("some/path")));
    assertTrue(rules.shouldInclude(getFile("some/path/another")));
    assertTrue(rules.shouldInclude(getFile("some/path/another/content")));
    assertTrue(rules.shouldInclude(getFile("some/path/content")));

    assertFalse(rules.shouldInclude(getFile("some")));
    assertFalse(rules.shouldInclude(getFile("another")));
    assertFalse(rules.shouldInclude(getFile("some/path/content/inner")));
    assertFalse(rules.shouldInclude(getFile("some/path/content/inner/content")));
  }

  private Rules createRules(@NotNull String... rules) {
    return new Rules(getRulesSet(rules), myCheckoutDir);
  }

  private Collection<String> getRulesSet(@NotNull String... rules) {
    return new HashSet<String>(Arrays.asList(rules));
  }

  private File getFile(@NotNull final String path) {
    return new File(myCheckoutDir, path);
  }
}
