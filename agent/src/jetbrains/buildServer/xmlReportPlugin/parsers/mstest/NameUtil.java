

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 20.01.2009 14:49:47
 */
class NameUtil {
  @Nullable
  public static String getTestName(String clazzFQ, String name) {
    if (clazzFQ != null && name != null) {
      final int fq = clazzFQ.indexOf(",");
      return (fq > 0 ? clazzFQ.substring(0, fq) : clazzFQ) + "." + name;
    }
    return null;
  }
}