package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Petrenko
 *         Created: 18.08.2009 15:33:49
 */
public class TestName {
  private final String myTestId;
  private final String myDataRowInfo;

  public TestName(@NotNull final String testId, final String dataRowInfo) {
    myTestId = testId;
    myDataRowInfo = dataRowInfo;
  }

  public TestName(final String testId) {
    this(testId, null);
  }

  @NotNull
  public String getTestId() {
    return myTestId;
  }

  public String presentName(@NotNull final String testName) {
    if (myDataRowInfo == null) {
      return testName;
    } else {
      return testName + "(" + myDataRowInfo + ")";
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TestName testName = (TestName)o;
    return !(myDataRowInfo != null ? !myDataRowInfo.equals(testName.myDataRowInfo) : testName.myDataRowInfo != null) &&
           myTestId.equals(testName.myTestId);
  }

  @Override
  public int hashCode() {
    int result = myTestId.hashCode();
    result = 31 * result + (myDataRowInfo != null ? myDataRowInfo.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    //This method is used in test
    return (myDataRowInfo == null ? "" : "(" + myDataRowInfo + ")") + myTestId;
  }
}
