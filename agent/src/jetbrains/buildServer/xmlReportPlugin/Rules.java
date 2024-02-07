

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 13:19
 */
public interface Rules {
  @NotNull Collection<String> getBody();
  @NotNull Collection<File> getPaths();
  @NotNull Collection<File> collectFiles();
}