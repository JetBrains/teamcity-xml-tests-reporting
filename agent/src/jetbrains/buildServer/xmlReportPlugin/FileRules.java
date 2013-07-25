package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * User: Victory.Bedrosova
 * Date: 1/31/12
 * Time: 4:55 PM
 */
public class FileRules implements Rules {
  @NotNull
  private final File myFile;

  public FileRules(@NotNull final File file) {
    myFile = file;
  }

  @NotNull
  public Collection<String> getBody() {
    return Collections.singletonList(myFile.getPath());
  }

  @NotNull
  public Collection<File> getPaths() {
    return Collections.singletonList(myFile);
  }

  @NotNull
  public List<File> collectFiles() {
    return myFile.isFile() ? Collections.singletonList(myFile) : collectFilesInFolder(myFile);
  }

  @NotNull
  private List<File> collectFilesInFolder(@NotNull File folder) {
    if (myFile.isDirectory()) {
      final File[] files = folder.listFiles();
      return files == null || files.length == 0 ? emptyFileList() : Arrays.asList(files);
    }
    return emptyFileList();
  }

  @NotNull
  private List<File> emptyFileList() {
    return Collections.emptyList();
  }
}
