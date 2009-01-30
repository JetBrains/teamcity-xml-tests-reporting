/*
 * Copyright 2008 JetBrains s.r.o.
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
package jetbrains.buildServer.testReportParserPlugin.findBugs;

import java.util.HashMap;
import java.util.Map;


public class FindBugsPatterns {
  public static final Map<String, Pattern> BUG_PATTERNS = new HashMap<String, Pattern>();

  static {
    BUG_PATTERNS.put("HE_EQUALS_USE_HASHCODE", new Pattern("Class defines equals() and uses Object.hashCode()",
      "BAD_PRACTICE",
      "This class overrides equals(Object), but does not override hashCode()."));
    BUG_PATTERNS.put("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT",
      new Pattern("equals() method does not check for null argument", "BAD_PRACTICE",
        "This implementation of equals(Object) violates the contract defined by java.lang.Object.equals() because " +
          "it does not check for null being passed as the argument. All equals() methods should return false if " +
          "passed a null value."));
  }

  public static boolean isCommonPattern(String id) {
    return BUG_PATTERNS.containsKey(id);
  }

  public static String getName(String id) {
    return BUG_PATTERNS.get(id).NAME;
  }

  public static String getCategory(String id) {
    return BUG_PATTERNS.get(id).CATEGORY;
  }

  public static String getDescription(String id) {
    return BUG_PATTERNS.get(id).DESCRIPTION;
  }

  private static final class Pattern {
    public final String NAME;
    public final String CATEGORY;
    public final String DESCRIPTION;

    private Pattern(String name, String category, String description) {
      this.NAME = name;
      this.CATEGORY = category;
      this.DESCRIPTION = description;
    }
  }
}
