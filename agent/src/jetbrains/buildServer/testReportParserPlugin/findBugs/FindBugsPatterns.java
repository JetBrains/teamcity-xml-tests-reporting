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

import jetbrains.buildServer.agent.SimpleBuildLogger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FindBugsPatterns {
  public static final Map<String, Pattern> BUG_PATTERNS = new HashMap<String, Pattern>();

  public static void loadPatterns(SimpleBuildLogger logger, InputStream is) {
    try {
      final Element root = new SAXBuilder().build(is).getRootElement();

      List categories = root.getChildren("BugPattern");

      for (Object o : categories) {
        final Element p = (Element) o;
        BUG_PATTERNS.put(p.getAttributeValue("type"),
          new Pattern(p.getAttributeValue("name"), p.getAttributeValue("category"),
            FindBugsReportParser.formatText(p.getText())));
      }
    } catch (Exception e) {
      //TODO: remove looger from parameters
//      logger.error("Couldn't load petterns from file " + is.getPath() + ", exception occured: " + e);
      logger.exception(e);
      e.printStackTrace();
    }
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
