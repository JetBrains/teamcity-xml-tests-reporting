

package jetbrains.buildServer.xmlReportPlugin.duplicates;

import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 14:54
 */
public class DuplicatingFragment {
  @NotNull
  private final String myPath;
  private final int myLine;
  private int myHash;

  public DuplicatingFragment(@NotNull String path, int line) {
    myPath = path;
    myLine = line;
  }

  @NotNull
  public String getPath() {
    return myPath;
  }

  public int getLine() {
    return myLine;
  }

  public void setHash(int hash) {
    myHash = hash;
  }

  /**
   * Note: hash is set after all fragments withing one {@link jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult} collected
   */
  public int getHash() {
    return myHash;
  }
}