

package org.jetbrains.testProject;


public class BuggyClass {
  public static final String CONSTANT = "c";
  private String str;

  public boolean equal(Object o) {
    return true;
  }

  public int hashCode() {
    return 0;
  }

  private class BuggyInnerClass {
    private int i;

    private void m() {}
    private void m(int i) {}
  }
}