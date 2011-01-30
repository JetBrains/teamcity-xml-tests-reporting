package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 19:28
 */

/**
 * Holds information about current state of files
 */
public interface FilesState {
  public static enum FileState {
    UNKNOWN, ON_PROCESSING, PROCESSED
  }

  /**
   * Gets the specified file state, which can be
   *
   * 1) UNKNOWN if file wasn't added or was removed
   * 2) ON_PROCESSING if file was added, but not processed yet
   * 3) PROCESSED if file was added and processed
   *
   *  @param file file
   * @return file state
   */
  @NotNull
  FileState getFileState(@NotNull File file);

  /**
   * Marks the previously added file as PROCESSED with the specified result
   * @param file file
   * @param parsingResult result
   */
  void setFileProcessed(@NotNull File file, @NotNull ParsingResult parsingResult);

  /**
   * Adds file and marks it as ON_PROCESSING
   * @param file file
   */
  void addFile(@NotNull File file);

  /**
   * Removes file from state and marks it's state as UNKNOWN
   * @param file file
   */
  void removeFile(@NotNull File file);
}
