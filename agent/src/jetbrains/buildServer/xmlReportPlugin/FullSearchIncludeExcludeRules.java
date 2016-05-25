/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
import java.util.*;
import java.util.regex.Pattern;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.Converter;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.vcs.FileRule;
import jetbrains.buildServer.vcs.FileRuleSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: Victory.Bedrosova
 * Date: 1/31/12
 * Time: 4:55 PM
 */
public class FullSearchIncludeExcludeRules implements Rules {
  @NotNull
  private final FileRuleSet<FileRule, FileRule> myRules;
  @NotNull
  private Collection<String> myBaseRules;

  public FullSearchIncludeExcludeRules(@NotNull final File baseDir, @NotNull Collection<String> body) {
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

        myBaseRules = new HashSet<String>();
        for (FileRule rule : resultRules) {
          //noinspection ConstantConditions
          myBaseRules.add((SystemInfo.isWindows ? "" : "/") + rule.getFrom());
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

  @NotNull
  public Collection<File> getPaths() {
    return CollectionsUtil.convertCollection(myBaseRules, new Converter<File, String>() {
      public File createFrom(@NotNull final String source) {
        return getPathWithoutPattern(source);
      }
    });
  }

  @NotNull
  public Collection<String> getBody() {
    return myRules.getBody();
  }

  @NotNull
  public Collection<File> collectFiles() {
    final List<File> result = new ArrayList<File>();

    for (String baseRule : myBaseRules) {
      final List<File> resultPart = new ArrayList<File>();
      final File basePath = getPathWithoutPattern(baseRule);
      final String pattern = baseRule.replace(normalizePath(basePath.getPath()), "");

      if (pattern.length() == 0) {
        if (basePath.isFile()) resultPart.add(basePath);
      } else {
        FileUtil.collectMatchedFiles(basePath, getRegexPattern(pattern), resultPart);
      }

      for (File file : resultPart) {
        if (myRules.shouldInclude(SystemInfo.isWindows ? file.getPath() : file.getPath().substring(1))) result.add(file);
      }
    }
    return result;
  }

  @NotNull
  private static File getPathWithoutPattern(@NotNull String path) {
    final int firstStar = path.indexOf('*');
    final int firstQuest = path.indexOf('?');

    if (firstStar < 0 && firstQuest < 0) {
      return new File(path);
    }

    int mark;
    if (firstStar < 0) {
      mark = firstQuest;
    } else if (firstQuest < 0) {
      mark = firstStar;
    } else {
      mark = Math.min(firstStar, firstQuest);
    }

    final int lastSlash = path.lastIndexOf('/', mark);
    return new File(lastSlash > 0 ? path.substring(0, lastSlash) : "");
  }

  @NotNull
  private static Pattern getRegexPattern(@NotNull String antPattern) {
    return Pattern.compile(FileUtil.convertAntToRegexp(antPattern));
  }

  @NotNull
  private static String normalizePath(@NotNull String path) {
    return path.replace("\\", "/");
  }
}
