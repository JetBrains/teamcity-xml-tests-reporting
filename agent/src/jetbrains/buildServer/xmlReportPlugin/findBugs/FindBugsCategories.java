/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.xmlReportPlugin.XmlReportParser;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class FindBugsCategories {
  public final Map<String, Category> myCategories = new HashMap<String, Category>();

  public void loadCategories(BaseServerLoggerFacade logger, InputStream resource) {
    try {
      final Handler handler = new Handler();
      XmlReportParser.createXmlReader(handler, handler, false).parse(new InputSource(resource));
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

  public Map<String, Category> getCategories() {
    return myCategories;
  }

  private final class Handler extends DefaultHandler {
    public static final String CATEGORY = "Category";

    private String myCurrentType;
    private Category myCurrentCategory;
    private StringBuffer myCData = new StringBuffer();

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentType = attributes.getValue("id");
        myCurrentCategory = new Category(attributes.getValue("name"));
      }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentCategory.setDescription(XmlReportParser.formatText(myCData));
        myCategories.put(myCurrentType, myCurrentCategory);
      }
      myCData.delete(0, myCData.length());
    }

    public void characters(char ch[], int start, int length) throws SAXException {
      myCData.append(ch, start, length);
    }
  }

  public static final class Category {
    private String myName;
    private String myDescription;

    public Category() {
      this("No name");
    }

    private Category(String name) {
      this(name, "No description");
    }

    private Category(String name, String description) {
      myName = name;
      myDescription = description;
    }

    public String getName() {
      return myName;
    }

    public void setName(String name) {
      myName = name;
    }

    public String getDescription() {
      return myDescription;
    }

    public void setDescription(String description) {
      myDescription = description;
    }
  }
}
