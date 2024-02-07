

package jetbrains.buildServer.xmlReportPlugin.inspections;

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 13:10
 */
public class InspectionResult {
  @Nullable
  private final String myFilePath, myInspectionId, myMessage;
  private final int myLine;
  private final int myPriority;

  public InspectionResult(@Nullable String filePath,
                          @Nullable String inspectionId,
                          @Nullable String message,
                          int line, int priority) {
    myFilePath = filePath;
    myInspectionId = inspectionId;
    myMessage = message;
    myLine = line;
    myPriority = priority;
  }

  @Nullable
  public String getFilePath() {
    return myFilePath;
  }

  @Nullable
  public String getInspectionId() {
    return myInspectionId;
  }

  @Nullable
  public String getMessage() {
    return myMessage;
  }

  public int getLine() {
    return myLine;
  }

  public int getPriority() {
    return myPriority;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final InspectionResult that = (InspectionResult)o;

    if (myLine != that.myLine) return false;
    if (myPriority != that.myPriority) return false;
    if (myFilePath != null ? !myFilePath.equals(that.myFilePath) : that.myFilePath != null) return false;
    if (myInspectionId != null ? !myInspectionId.equals(that.myInspectionId) : that.myInspectionId != null) return false;
    return !(myMessage != null ? !myMessage.equals(that.myMessage) : that.myMessage != null);

  }

  @Override
  public int hashCode() {
    int result = myFilePath != null ? myFilePath.hashCode() : 0;
    result = 31 * result + (myInspectionId != null ? myInspectionId.hashCode() : 0);
    result = 31 * result + (myMessage != null ? myMessage.hashCode() : 0);
    result = 31 * result + myLine;
    result = 31 * result + myPriority;
    return result;
  }

  @Override
  public String toString() {
    return "InspectionInstance{" +
           "myInspectionId='" + myInspectionId + '\'' +
           ", myMessage='" + myMessage + '\'' +
           ", myFilePath='" + myFilePath + '\'' +
           ", myLine=" + myLine +
           ", myPriority=" + myPriority +
           '}';
  }
}