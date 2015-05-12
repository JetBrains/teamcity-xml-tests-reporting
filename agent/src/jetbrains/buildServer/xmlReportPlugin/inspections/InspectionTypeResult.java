/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
