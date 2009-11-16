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

import jetbrains.buildServer.xmlReportPlugin.XmlReportParser;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Parser;
import javax.swing.text.html.parser.TagElement;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * User: vbedrosova
 * Date: 02.11.2009
 * Time: 14:04:31
 */
class BugCollection {
  private static final String FINDBUGS_XML = "findbugs.xml";
  private static final String MESSAGES_XML = "messages.xml";

  private Map<String, Category> myCategories;
  private Map<String, Pattern> myBugPatterns;

  @NotNull
  private static DetailsParser DETAILS_PARSER;

  static {
    try {
      DETAILS_PARSER = new DetailsParser(DTD.getDTD(""));
    } catch (IOException e) {
      XmlReportPlugin.LOG.error("Couldn't create empty DTD", e);
    }
  }

  public Map<String, Category> getCategories() {
    return myCategories;
  }

  public Map<String, Pattern> getPatterns() {
    return myBugPatterns;
  }

  public void loadBundledPatterns() {
    XmlReportPlugin.LOG.debug("Loading bundled bug patterns");
    myCategories = new HashMap<String, Category>();
    myBugPatterns = new HashMap<String, Pattern>();
    try {
      parse(new FindBugsHandler(), new InputSource(this.getClass().getResourceAsStream("findbugs.xml")));
      parse(new MessagesHandler(), new InputSource(this.getClass().getResourceAsStream("messages.xml")));
    } catch (Exception e) {
      XmlReportPlugin.LOG.error("Couldn't load bug patterns from bundled findbugs.xml and messages.xml", e);
    }
  }

  public void loadPatternsFromFindBugs(@NotNull File findBugsHome) {
    XmlReportPlugin.LOG.debug("Loading bug patterns from FindBugs home " + findBugsHome.getAbsolutePath());
    myCategories = new HashMap<String, Category>();
    myBugPatterns = new HashMap<String, Pattern>();
    load(new File(findBugsHome.getAbsolutePath() + File.separator + "lib", "findbugs.jar"));
    final File pluginFolder = new File(findBugsHome, "plugin");
    final File[] plugins = pluginFolder.listFiles();
    if ((plugins == null) || (plugins.length == 0)) {
      return;
    }
    for (final File p : plugins) {
      if (!p.getAbsolutePath().endsWith(".jar")) {
        continue;
      }
      load(p);
    }
  }

  private void load(@NotNull File file) {
    XmlReportPlugin.LOG.debug("Loading bug patterns from plugin jar " + file.getAbsolutePath());
    JarFile jar = null;
    try {
      jar = new JarFile(file);
      final JarEntry findugs = jar.getJarEntry(FINDBUGS_XML);
      final JarEntry messages = jar.getJarEntry(MESSAGES_XML);
      if (findugs == null) {
        XmlReportPlugin.LOG.warn("Couldn't find findbugs.xml in plugin jar " + file.getAbsolutePath());
        return;
      }
      if (messages == null) {
        XmlReportPlugin.LOG.warn("Couldn't find messages.xml in plugin jar " + file.getAbsolutePath());
        return;
      }
      parse(new FindBugsHandler(), new InputSource(jar.getInputStream(findugs)));
      parse(new MessagesHandler(), new InputSource(jar.getInputStream(messages)));
    } catch (Exception e) {
      XmlReportPlugin.LOG.error("Couldn't load bug patterns from findbugs.xml and messages.xml from plugin jar " + file.getAbsolutePath(), e);
    } finally {
      try {
        if (jar != null) {
          jar.close();
        }
      } catch (IOException e) {
        XmlReportPlugin.LOG.error("Couldn't close plugin jar " + file.getAbsolutePath(), e);
      }
    }
  }

  private final class FindBugsHandler extends DefaultHandler {
    public static final String BUG_PATTERN = "BugPattern";

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if (BUG_PATTERN.equals(localName)) {
        myBugPatterns.put(attributes.getValue("type"),
          new Pattern(attributes.getValue("category")));
      }
    }
  }

  private final class MessagesHandler extends DefaultHandler {
    public static final String CATEGORY = "BugCategory";
    public static final String DESCRIPTION = "Description";
    public static final String SHORT_DESCRIPTION = "ShortDescription";
    public static final String DETAILS = "Details";
    public static final String BUG_PATTERN = "BugPattern";

    private Category myCurrentCategory;
    private Pattern myCurrentPattern;
    private final StringBuffer myCData = new StringBuffer();

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
      throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentCategory = new Category();
        myCategories.put(attributes.getValue("category"), myCurrentCategory);
      } else if (BUG_PATTERN.equals(localName)) {
        myCurrentPattern = myBugPatterns.get(attributes.getValue("type"));
      }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (CATEGORY.equals(localName)) {
        myCurrentCategory = null;
      } else if (BUG_PATTERN.equals(localName)) {
        myCurrentPattern = null;
      } else if (DESCRIPTION.equals(localName)) {
        myCurrentCategory.setName(XmlReportParser.formatText(myCData));
      } else if (SHORT_DESCRIPTION.equals(localName)) {
        if (myCurrentPattern != null) {
          myCurrentPattern.setName(XmlReportParser.formatText(myCData));
        }
      } else if (DETAILS.equals(localName)) {
        if (myCurrentCategory != null) {
          final String text = XmlReportParser.formatText(myCData);
          myCurrentCategory.setDescription(text.substring(0, 1).toUpperCase() + text.substring(1));
        } else if (myCurrentPattern != null) {
          myCurrentPattern.setDescription(formatText(myCData));
        }
      }
      myCData.delete(0, myCData.length());
    }

    public void characters(char ch[], int start, int length) throws SAXException {
      myCData.append(ch, start, length);
    }
  }

  private static String formatText(@NotNull StringBuffer sb) {
//    return XmlReportParser.formatText(sb).replaceAll("</?\\p{Alnum}([^>\"]*(\"[^\"]*\")?)*>|&\\p{Alnum}*;", "");
    try {
      DETAILS_PARSER.parse(new BufferedReader(new StringReader(sb.toString())));
    } catch (IOException e) {
      XmlReportPlugin.LOG.error("Couldn't format html description to text", e);
    }
    return DETAILS_PARSER.getText().replace("&nbsp", "");
  }

  private void parse(@NotNull DefaultHandler handler, @NotNull InputSource source) throws Exception {
    XmlReportParser.createXmlReader(handler, handler, false).parse(source);
  }

  public static final class Category {
    @NotNull
    private String myName;
    @NotNull
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

    @NotNull
    public String getName() {
      return myName;
    }

    public void setName(@NotNull String name) {
      myName = name;
    }

    @NotNull
    public String getDescription() {
      return myDescription;
    }

    public void setDescription(@NotNull String description) {
      myDescription = description;
    }
  }

  public static final class Pattern {
    private String myName;
    private final String myCategory;
    private String myDescription;

    public Pattern(String category) {
      this("No name", category, "No description");
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

  private static final class DetailsParser extends Parser {
    private final StringBuffer myStringBuffer = new StringBuffer();

    public DetailsParser(DTD dtd) {
      super(dtd);
    }

    public void parse(Reader in) throws java.io.IOException {
      myStringBuffer.delete(0, myStringBuffer.length());
      super.parse(in);
    }

    protected void handleText(char text[]) {
      myStringBuffer.append(text);
    }

    protected void handleEndTag(TagElement tag) {
      myStringBuffer.append(" ");
    }

    public String getText() {
      return myStringBuffer.toString();
    }
  }
}
