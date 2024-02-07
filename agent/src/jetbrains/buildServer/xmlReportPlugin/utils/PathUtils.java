

package jetbrains.buildServer.xmlReportPlugin.utils;

import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
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
    if (StringUtil.isEmptyOrSpaces(path)) return "<unknown>";

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

  @NotNull
  private static String unifySlashes(String s) {
    return s == null ? StringUtil.EMPTY : s.replace('\\', SEPARATOR);
  }
}