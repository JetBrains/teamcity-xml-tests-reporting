/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.filters.Filter;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * User: vbedrosova
 * Date: 10.12.10
 * Time: 19:32
 */
public abstract class BaseRulesTest extends BaseTestCase {
  @NotNull
  private File myBaseFolder;
  @NotNull
  private File myOuterFolder;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();
    myBaseFolder = createTempDir();
    myOuterFolder = createTempDir();
  }

  @Test
  public void test_include_recursive() throws Exception {
    final Rules rules = createRules("+:some/path/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_recursive_dot() throws Exception {
    final Rules rules = createRules("+:./some/path/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_exclude_recursive() throws Exception {
    final Rules rules = createRules("+:some/path/**/*", "-:some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_content() throws Exception {
    final Rules rules = createRules("+:some/path/*");

    assertInclude(rules, createFile("some/path/file.txt"));

    assertExclude(rules, createFile("some/path/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_content_dot() throws Exception {
    final Rules rules = createRules("+:./some/path/*");

    assertInclude(rules, createFile("some/path/file.txt"));

    assertExclude(rules, createFile("some/path/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_content_recursive() throws Exception {
    final Rules rules = createRules("+:some/path/**");

    assertInclude(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_include_content_recursive_dot() throws Exception {
    final Rules rules = createRules("+:./some/path/**");

    assertInclude(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_absolute_path_include() throws Exception {
    final Rules rules = createRules("+:##BASE_DIR##\\some\\path\\**\\*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_absolute_include_exclude() throws Exception {
    final Rules rules = createRules("+:##BASE_DIR##/some/path/**/*", "-:##BASE_DIR##/some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_mixed_include_exclude() throws Exception {
    final Rules rules = createRules("+:some/path/**/*", "-:##BASE_DIR##/some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_mixed_include_exclude_1() throws Exception {
    final Rules rules = createRules("+:##BASE_DIR##/some/path/**/*", "-:some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_outer_include_exclude() throws Exception {
    final Rules rules = createRules("+:##OUTER_DIR##/some/path/**/*", "-:##OUTER_DIR##/some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createOuterFile("some/path/file.txt"));
    assertInclude(rules, createOuterFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createOuterFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createOuterFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createOuterFile("some/file.txt"));
    assertExclude(rules, createOuterFile("another/file.txt"));
    assertExclude(rules, createOuterFile("file.txt"));
  }

  @Test
  public void test_mixed_include_exclude_2() throws Exception {
    final Rules rules = createRules("+:##OUTER_DIR##/some/path/**/*", "-:##OUTER_DIR##/some/path/content/inner/**/*",
                                    "+:##BASE_DIR##/some/path/**/*", "-:##BASE_DIR##/some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createOuterFile("some/path/file.txt"));
    assertInclude(rules, createOuterFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createOuterFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createOuterFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createOuterFile("some/file.txt"));
    assertExclude(rules, createOuterFile("another/file.txt"));
    assertExclude(rules, createOuterFile("file.txt"));

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_mixed_include_exclude_3() throws Exception {
    final Rules rules = createRules("+:##OUTER_DIR##/some/path/**/*", "-:##OUTER_DIR##/some/path/content/inner/**/*",
                                    "+:some/path/**/*", "-:some/path/content/inner/**/*");

    assertIncludeWithStars(rules, createOuterFile("some/path/file.txt"));
    assertInclude(rules, createOuterFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createOuterFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createOuterFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createOuterFile("some/file.txt"));
    assertExclude(rules, createOuterFile("another/file.txt"));
    assertExclude(rules, createOuterFile("file.txt"));

    assertIncludeWithStars(rules, createFile("some/path/file.txt"));
    assertInclude(rules, createFile("some/path/content/file.txt"));

    assertExcludeWithStars(rules, createFile("some/path/content/inner/file.txt"));
    assertExclude(rules, createFile("some/path/content/inner/content/file.txt"));
    assertExclude(rules, createFile("some/file.txt"));
    assertExclude(rules, createFile("another/file.txt"));
    assertExclude(rules, createFile("file.txt"));
  }

  @Test
  public void test_file() throws Exception {
    assertInclude(createRules("some/path/file.txt"), createFile("some/path/file.txt"));
  }

  @Test
  public void test_outer_file() throws Exception {
    assertInclude(createRules("##OUTER_DIR##/some/path/file.txt"), createOuterFile("some/path/file.txt"));
  }

  @Test
  public void test_absolute_file() throws Exception {
    assertInclude(createRules("##BASE_DIR##/some/path/file.txt"), createFile("some/path/file.txt"));
  }

  @Test
  public void test_file_include_exclude() throws Exception {
    assertExclude(createRules("some/path/**/*", "-:some/path/content/file.txt"), createFile("some/path/content/file.txt"));
  }

  @NotNull
  private Rules createRules(@NotNull String... rules) {
    return createRules(myBaseFolder,
                       CollectionsUtil.convertCollection(Arrays.asList(rules),
                                                         new Converter<String, String>() {
                                                           public String createFrom(@NotNull final String source) {
                                                             return source.replace("##BASE_DIR##", myBaseFolder.getAbsolutePath())
                                                               .replace("##OUTER_DIR##", myOuterFolder.getAbsolutePath());
                                                           }
                                                         }));
  }

  @NotNull
  protected abstract Rules createRules(@NotNull File baseDir, @NotNull List<String> rules);

  private void assertInclude(@NotNull Rules rules, @NotNull File file) throws Exception {
    assertTrue(contains(rules.collectFiles(), file));
  }

  private void assertIncludeWithStars(@NotNull Rules rules, @NotNull File file) throws Exception {
    final boolean contains = contains(rules.collectFiles(), file);
    if (starsMatchRoot()) assertTrue(contains);
    else assertFalse(contains);
  }

  private void assertExclude(@NotNull Rules rules, @NotNull File file) throws Exception {
    assertFalse(contains(rules.collectFiles(), file));
  }

  private void assertExcludeWithStars(@NotNull Rules rules, @NotNull File file) throws Exception {
    final boolean contains = contains(rules.collectFiles(), file);
    if (starsMatchRoot()) assertFalse(contains);
    else assertTrue(contains);
  }

  private boolean contains(@NotNull java.util.Collection<File> files, @NotNull final File file) {
    return null != CollectionsUtil.findFirst(files, new Filter<File>() {
      public boolean accept(@NotNull final File data) {
        try {
          return file.getCanonicalFile().equals(data.getCanonicalFile());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  @NotNull
  private File createOuterFile(@NotNull final String path) throws IOException {
    return createFile("##OUTER_DIR##/" + path);
  }

  @NotNull
  private File createFile(@NotNull final String path) throws IOException {
    final File resolved = FileUtil.resolvePath(myBaseFolder, path.replace("##OUTER_DIR##", myOuterFolder.getAbsolutePath()));
    resolved.getParentFile().mkdirs();
    FileUtil.writeFileAndReportErrors(resolved, "some text");
    return resolved;
  }

  protected boolean starsMatchRoot() {
    return true;
  }
}
