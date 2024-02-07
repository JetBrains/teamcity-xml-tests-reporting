

package jetbrains.buildServer.xmlReportPlugin.duplicates;

import jetbrains.buildServer.xmlReportPlugin.MessageLogger;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 08.02.11
 * Time: 15:24
 */
public interface DuplicationReporter extends MessageLogger {
  /**
   * Indicates the beginning of a duplicates block
   */
  void startDuplicates();

  /**
   * Reports duplicate within block
   *
   * @param duplicate Duplicate info
   */
  void reportDuplicate(@NotNull DuplicationResult duplicate);

  /**
   * Indicates the end of a duplicates block
   */
  void finishDuplicates();
}