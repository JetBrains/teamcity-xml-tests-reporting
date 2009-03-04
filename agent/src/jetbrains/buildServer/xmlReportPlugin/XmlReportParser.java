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

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.Map;


public abstract class XmlReportParser extends DefaultHandler {
  private XMLReader myXmlReader;
  protected final BaseServerLoggerFacade myLogger;

  protected StringBuffer myCData;

  public static String formatText(@NotNull StringBuffer s) {
    return s.toString().replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").trim();
  }

  public static int getNumber(String number) {
    if (number != null) {
      try {
        return Integer.parseInt(number);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  public static String generateBuildStatus(int errors, int warnings) {
    return "Errors: " + errors + ", warnings: " + warnings;
  }

  public static XMLReader createXmlReader(ContentHandler contentHandler, ErrorHandler errHandler, boolean validate) throws Exception {
    final XMLReader xmlReader = XMLReaderFactory.createXMLReader();
    xmlReader.setContentHandler(contentHandler);
    xmlReader.setErrorHandler(errHandler);
    xmlReader.setFeature("http://xml.org/sax/features/validation", validate);
    return xmlReader;
  }

  public XmlReportParser(@NotNull final BaseServerLoggerFacade logger) {
    myLogger = logger;
    myCData = new StringBuffer();
    try {
      myXmlReader = createXmlReader(this, this, false);
    } catch (Exception e) {
      myLogger.exception(e);
    }
  }

  boolean abnormalEnd() {
    return false;
  }

  void logReportTotals(@NotNull File report) {
  }

  void logParsingTotals(@NotNull Map<String, String> parameters) {
  }

  public final void parse(@NotNull File report) throws SAXParseException {
    try {
      myXmlReader.parse(new InputSource(report.toURI().toString()));
    } catch (SAXParseException se) {
      throw se;
    } catch (Exception e) {
      myLogger.exception(e);
    }
  }

  public void characters(char ch[], int start, int length) throws SAXException {
    myCData.append(ch, start, length);
  }

  public abstract int parse(@NotNull File report, int testsToSkip);
}
