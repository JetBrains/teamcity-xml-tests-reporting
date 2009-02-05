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

import jetbrains.buildServer.testReportParserPlugin.TestReportLogger;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FindBugsCategories {
  public static final Map<String, Category> CATEGORIES = new HashMap<String, Category>();

  public static void loadCategories(TestReportLogger logger, InputStream is) {
    try {
      final Element root = new SAXBuilder().build(is).getRootElement();

      List categories = root.getChildren("Category");

      for (Object o : categories) {
        final Element c = (Element) o;
        CATEGORIES.put(c.getAttributeValue("id"),
          new Category(c.getAttributeValue("name"), FindBugsReportParser.formatText(c.getText())));
      }
    } catch (Exception e) {
      //TODO: remove looger from parameters      
      e.printStackTrace();
//      logger.error("Couldn't load categories from file " + is.getPath() + ", exception occured: " + e);
      logger.exception(e);
    }
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
