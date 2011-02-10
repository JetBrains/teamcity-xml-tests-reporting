/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import java.util.*;
import jetbrains.buildServer.vcs.FileRule;
import jetbrains.buildServer.vcs.FileRuleSet;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:19
 */
public class Rules extends FileRuleSet<FileRule, FileRule> {
  private Set<File> myRootIncludePaths;

  public Rules(@NotNull Collection<String> body) {
    super(new ArrayList<String>(body));
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
    return new FileRule(line, null, this, isInclude);
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
      myRootIncludePaths.add(new File(rule.getFrom()));
    }
    return myRootIncludePaths;
  }

  @NotNull
  public Set<File> getPaths() {
    if (myRootIncludePaths == null) initRootIncludePaths();
    return myRootIncludePaths;
  }

  public boolean shouldInclude(@NotNull File path) {
    return super.shouldInclude(path.getPath());
  }
}
