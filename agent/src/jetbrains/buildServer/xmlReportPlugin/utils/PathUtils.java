/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.utils;

import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 08.02.11
 * Time: 15:42
 */
public class PathUtils {
  private static final char SEPARATOR = '/';

  @NotNull
  public static String getRelativePath(@NotNull String base, @Nullable String path) {
    if (path == null || path.trim().length() == 0) return "<unknown>";

    base = unifySlashes(base);
    path = unifySlashes(path);

    String resolved = FileUtil.getRelativePath(base, path, SEPARATOR);

    if (resolved == null)
      //noinspection ConstantConditions
      return path;

    if (resolved.startsWith("./")) {
      resolved = resolved.substring(2);
    }

    return resolved;
  }
  private static String unifySlashes(String s) {
    return s == null ? "" : s.replace('\\', SEPARATOR);
  }
}
