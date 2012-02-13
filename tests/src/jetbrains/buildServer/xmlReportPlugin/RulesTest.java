/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.xmlReportPlugin;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * User: vbedrosova
 * Date: 10.12.10
 * Time: 19:32
 */
public class RulesTest extends TestCase {
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

  @Test
  public void test_absolute_path_include() {
    final Rules rules = createRules("+:C:\\some\\path");

    assertTrue(rules.shouldInclude(getFile("C:\\some\\path")));
    assertTrue(rules.shouldInclude(getFile("C:\\some\\path\\content")));

    assertFalse(rules.shouldInclude(getFile("C:\\some")));
    assertFalse(rules.shouldInclude(getFile("another")));
    assertFalse(rules.shouldInclude(getFile("C:\\another")));
  }

  private Rules createRules(@NotNull String... rules) {
    return new StringRules(getRulesSet(rules));
  }

  private Collection<String> getRulesSet(@NotNull String... rules) {
    return Arrays.asList(rules);
  }

  private File getFile(@NotNull final String path) {
    return new File(SystemInfo.isWindows ? path : "/" + path);
  }
}
