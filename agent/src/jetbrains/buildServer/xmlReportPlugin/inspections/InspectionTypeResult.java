

package jetbrains.buildServer.xmlReportPlugin.inspections;

import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 13:11
 */
public class InspectionTypeResult {
  @Nullable
  private final String myId, myName, myDescription, myCategory;

  public InspectionTypeResult(@Nullable String id,
                              @Nullable String name,
                              @Nullable String description,
                              @Nullable String category) {
    myId = id;
    myName = name;
    myDescription = description;
    myCategory = category;
  }

  @Nullable
  public String getId() {
    return myId;
  }

  @Nullable
  public String getName() {
    return myName;
  }

  @Nullable
  public String getDescription() {
    return myDescription;
  }

  @Nullable
  public String getCategory() {
    return myCategory;
  }

  @Override
  public String toString() {
    return "InspectionTypeInfo{" +
           "myId='" + myId + '\'' +
           ", myName='" + myName + '\'' +
           ", myDescription='" + myDescription + '\'' +
           ", myCategory='" + myCategory + '\'' +
           '}';
  }
}