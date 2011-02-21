package jetbrains.buildServer.xmlReportPlugin.duplicates;

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 14:54
 */
public class DuplicatingFragment {
  @Nullable
  private final String myPath;
  private final int myLine;

  public DuplicatingFragment(@Nullable String path, int line) {
    myPath = path;
    myLine = line;
  }

  @Nullable
  public String getPath() {
    return myPath;
  }

  public int getLine() {
    return myLine;
  }
}
