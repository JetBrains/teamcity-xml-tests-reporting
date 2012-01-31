package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
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

  public Collection<File> getPaths() {
    return Collections.singletonList(myFile);
  }

  public List<String> getBody() {
    return Collections.singletonList(myFile.getPath());
  }

  public boolean shouldInclude(@NotNull final File path) {
    return true; // we believe that MonitorRulesCommand collects good files
  }
}
