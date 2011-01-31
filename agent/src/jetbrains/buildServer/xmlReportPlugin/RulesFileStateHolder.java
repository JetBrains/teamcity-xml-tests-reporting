package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 20.01.11
 * Time: 16:52
 */
public class RulesFileStateHolder implements FileStateHolder {
  @NotNull
  private final Map<File, ParsingResult> myParsingResults = new HashMap<File, ParsingResult>();

  @NotNull
  public synchronized FileState getFileState(@NotNull File file) {
    if (myParsingResults.containsKey(file)) {
      if (myParsingResults.get(file) != null) {
        return FileStateHolder.FileState.PROCESSED;
      }
      return FileStateHolder.FileState.ON_PROCESSING;
    }
    return FileStateHolder.FileState.UNKNOWN;
  }

  public synchronized void setFileProcessed(@NotNull File file, @NotNull ParsingResult parsingResult) {
    if (!myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is not present");
    }
    if (myParsingResults.get(file) != null) {
      throw new IllegalStateException("File " + file + " is already processed");
    }
    myParsingResults.put(file, parsingResult);
  }

  public synchronized void addFile(@NotNull File file) {
    if (myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is already present");
    }
    myParsingResults.put(file, null);
  }

  public synchronized void removeFile(@NotNull File file) {
    if (!myParsingResults.containsKey(file)) {
      throw new IllegalStateException("File " + file + " is not present");
    }
    myParsingResults.remove(file);
  }

  @NotNull
  public synchronized Map<File, ParsingResult> getFiles() {
    return myParsingResults;
  }
}
