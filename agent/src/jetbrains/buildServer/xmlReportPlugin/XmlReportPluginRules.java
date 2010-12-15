/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.FileRule;
import jetbrains.buildServer.vcs.FileRuleSet;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

/**
 * User: vbedrosova
 * Date: 09.12.10
 * Time: 12:36
 */
public class XmlReportPluginRules extends FileRuleSet<FileRule, FileRule> {
  @NotNull
  private final File myCheckoutDir;

  private Set<File> myRootIncludePaths;

  public XmlReportPluginRules(@NotNull final Set<String> body, @NotNull final String checkoutDir) {
    super(new ArrayList<String>(body));
    myCheckoutDir = new File(checkoutDir);
  }

  @Override
  protected void doPostInitProcess(@NotNull final List<FileRule> includeRules, @NotNull final List<FileRule> excludeRules) {
    sortByFrom(includeRules, true);
  }

  @Override
  protected FileRule createNewIncludeRule(@NotNull final String line) {
    return createRule(line, true);
  }

  private FileRule createRule(@NotNull final String line, boolean isInclude) {
    return new FileRule(line, this, isInclude);
  }

  @Override
  protected FileRule createExcludeRule(@NotNull final String line) {
    return createRule(line, false);
  }

  @Override
  protected FileRule createNewIncludeRule(@NotNull final FileRule includeRule) {
    return createNewIncludeRule(includeRule.getFrom());
  }

  @Override
  protected FileRule createNewExcludeRule(@NotNull final FileRule excludeRule) {
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

      for (final FileRule processed : processedRules) {
        if (isSubDir(rule.getFrom(), processed.getFrom())) {
          iterator.remove();
          break;
        }
      }

      processedRules.add(rule);
    }

    myRootIncludePaths = new HashSet<File>();
    for (final FileRule rule : resultRules) {
      myRootIncludePaths.add(FileUtil.resolvePath(myCheckoutDir, rule.getFrom()));
    }
    return myRootIncludePaths;
  }

  @NotNull
  public Set<File> getRootIncludePaths() {
    if (myRootIncludePaths == null) initRootIncludePaths();
    return myRootIncludePaths;
  }

  public boolean shouldInclude(File path) {
    return super.shouldInclude(FileUtil.getRelativePath(myCheckoutDir, path));
  }
}
