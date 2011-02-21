package jetbrains.buildServer.xmlReportPlugin.duplicates;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 14:53
 */
public class DuplicationResult {
  private final int myLines;
  private final int myTokens;
  private int myHash;

  @NotNull
  private final List<DuplicatingFragment> myFragments = new ArrayList<DuplicatingFragment>();

  public DuplicationResult(int lines, int tokens) {
    myLines = lines;
    myTokens = tokens;
  }

  public int getLines() {
    return myLines;
  }

  public int getTokens() {
    return myTokens;
  }

  public void addFragment(@NotNull DuplicatingFragment fragment) {
    myFragments.add(fragment);
  }

  @NotNull
  public List<DuplicatingFragment> getFragments() {
    return myFragments;
  }

  public int getHash() {
    return myHash;
  }

  public void setHash(int hash) {
    myHash = hash;
  }
}
