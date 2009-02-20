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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

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


public class FindBugsPatterns {
  public final Map<String, Pattern> myBugPatterns = new HashMap<String, Pattern>();

  public void loadPatterns(SimpleBuildLogger logger, InputStream resource) {
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

  public Map<String, Pattern> getPatterns() {
    return myBugPatterns;
  }

  private final class Handler extends DefaultHandler {
    public static final String BUG_PATTERN = "BugPattern";

    private String myCurrentType;
    private Pattern myCurrentPattern;
    private StringBuffer myCData = new StringBuffer();

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if (BUG_PATTERN.equals(localName)) {
        myCurrentType = attributes.getValue("type");
        myCurrentPattern = new Pattern(attributes.getValue("name"), attributes.getValue("category"));
      }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (BUG_PATTERN.equals(localName)) {
        myCurrentPattern.setDescription(FindBugsReportParser.formatText(myCData));
        myBugPatterns.put(myCurrentType, myCurrentPattern);
      }
      myCData.delete(0, myCData.length());
    }

    public void characters(char ch[], int start, int length) throws SAXException {
      myCData.append(ch, start, length);
    }
  }

  public static final class Pattern {
    private String myName;
    private final String myCategory;
    private String myDescription;

    public Pattern(String category) {
      this("No name", category, "No description");
    }

    public Pattern(String name, String category) {
      this(name, category, "No description");
    }

    private Pattern(String name, String category, String description) {
      myName = name;
      myCategory = category;
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

    public String getCategory() {
      return myCategory;
    }
  }
}
