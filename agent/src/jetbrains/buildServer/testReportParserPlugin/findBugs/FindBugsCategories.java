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


public class FindBugsCategories {
  public static final Map<String, Category> CATEGORIES = new HashMap<String, Category>();

  static {
    CATEGORIES.put("CORRECTNESS", new Category("Correctness bug",
      "Probable bug - an apparent coding mistake resulting in code that was probably not what the developer intended. " +
        "We strive for a low false positive rate."));
    CATEGORIES.put("BAD_PRACTICE", new Category("Bad Practice", "Violations of recommended and essential coding practice. " +
      "Examples include hash code and equals problems, cloneable idiom, dropped exceptions, serializable problems, and misuse of finalize. " +
      "We strive to make this analysis accurate, although some groups may not care about some of the bad practices."));
    CATEGORIES.put("DODGY", new Category("Dodgy", "Code that is confusing, anomalous, or written in a way that leads itself to errors. " +
      "Examples include dead local stores, switch fall through, unconfirmed casts, and redundant null check of value " +
      "known to be null. More false positives accepted. In previous versions of FindBugs, this category was known as Style."));
    // for old versions support
    CATEGORIES.put("STYLE", new Category("Dodgy", "Code that is confusing, anomalous, or written in a way that leads itself to errors. " +
      "Examples include dead local stores, switch fall through, unconfirmed casts, and redundant null check of value " +
      "known to be null. More false positives accepted. In previous versions of FindBugs, this category was known as Style."));
  }

  public static boolean isCommonCategory(String id) {
    return CATEGORIES.containsKey(id);
  }

  public static String getName(String id) {
    return CATEGORIES.get(id).NAME;
  }

  public static String getDescription(String id) {
    return CATEGORIES.get(id).DESCRIPTION;
  }

  private static final class Category {
    public final String NAME;
    public final String DESCRIPTION;

    private Category(String name, String description) {
      this.NAME = name;
      this.DESCRIPTION = description;
    }
  }
}
