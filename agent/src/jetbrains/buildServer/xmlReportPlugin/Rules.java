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

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.lang.String;
import java.util.*;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.FileRule;
import jetbrains.buildServer.vcs.FileRuleSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:19
 */
public class Rules {
  @NotNull
  private final FileRuleSet<FileRule, FileRule> myRules;
  private Collection<File> myRootPaths;

  public Rules(@NotNull Collection<String> body) {
    this(null, body);
  }

  public Rules(@Nullable final File baseDir, @NotNull Collection<String> body) {
    myRules = new FileRuleSet<FileRule, FileRule>(new ArrayList<String>(body)) {

      @Override
      protected void doPostInitProcess(final List<FileRule> includeRules, final List<FileRule> excludeRules) {
        sortByFrom(includeRules, true);
        initRootIncludePaths();
      }

      @Override
      protected FileRule createNewIncludeRule(final String line) {
        return createRule(line, true);
      }

      @Override
      protected FileRule createNewExcludeRule(final String line) {
        return createRule(line, false);
      }

      @Override
      protected FileRule createNewIncludeRule(final FileRule includeRule) {
        return createNewIncludeRule(includeRule.getFrom());
      }

      @Override
      protected FileRule createNewExcludeRule(final FileRule excludeRule) {
        return createNewExcludeRule(excludeRule.getFrom());
      }

      private FileRule createRule(@NotNull String line, boolean isInclude) {
        return new FileRule(resolvePath(line, baseDir) , null, this, isInclude);
      }

      private void initRootIncludePaths() {
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

        myRootPaths = new HashSet<File>();
        for (FileRule rule : resultRules) {
          //noinspection ConstantConditions
          myRootPaths.add(new File((SystemInfo.isWindows ? "" : "/") + rule.getFrom()));
        }
      }
    };
  }

  @NotNull
  private static String resolvePath(@NotNull String path, @Nullable File baseDir) {
    if (baseDir != null) {
      return FileUtil.resolvePath(baseDir, path).getPath();
    }
    return path;
  }

  public Collection<File> getPaths() {
    return myRootPaths;
  }

  public List<String> getBody() {
    return myRules.getBody();
  }

  public boolean shouldInclude(@NotNull File path) {
    return myRules.shouldInclude(SystemInfo.isWindows ? path.getPath() : path.getPath().substring(1));
  }
}
