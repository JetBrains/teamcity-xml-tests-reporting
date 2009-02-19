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
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class FindBugsCategories {
  public static final Map<String, Category> CATEGORIES = new HashMap<String, Category>();

  public static void loadCategories(SimpleBuildLogger logger, InputStream resource) {
    try {
      XMLReader xmlReader = XMLReaderFactory.createXMLReader();
      xmlReader.setContentHandler(new Handler());
      xmlReader.setFeature("http://xml.org/sax/features/validation", false);

      xmlReader.parse(new InputSource(resource));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        resource.close();
      } catch (IOException e) {
        logger.exception(e);
      }
    }
  }

  public static boolean isCommonCategory(String id) {
    return CATEGORIES.containsKey(id);
  }

  public static String getName(String id) {
    return CATEGORIES.get(id).myName;
  }

  public static String getDescription(String id) {
    return CATEGORIES.get(id).myDescription;
  }

  private static final class Handler extends DefaultHandler {
    public static final String CATEGORY = "Category";

    private String myCurrentType;
    private Category myCurrentCategory;
    private StringBuffer myCData;

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentType = attributes.getValue("id");
        myCurrentCategory = new Category(attributes.getValue("name"));
        myCData = new StringBuffer();
      }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentCategory.setDescription(FindBugsReportParser.formatText(myCData.toString()));
        CATEGORIES.put(myCurrentType, myCurrentCategory);
        myCData = null;
      }
    }

    public void characters(char ch[], int start, int length) throws SAXException {
      if (myCData != null) {
        myCData.append(ch, start, length);
      }
    }
  }

  private static final class Category {
    public final String myName;
    public String myDescription;

    public Category(String name) {
      this.myName = name;
    }

    public void setDescription(String description) {
      myDescription = description;
    }
  }
}
