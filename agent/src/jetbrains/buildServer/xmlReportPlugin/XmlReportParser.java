/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.Map;


public abstract class XmlReportParser extends DefaultHandler {
  private final XMLReader myXmlReader;
  protected StringBuilder myCData;

  public static String formatText(@NotNull StringBuilder s) {
    return s.toString().replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").replaceAll("<[a-z]>|</[a-z]>", "").trim();
  }

//  public static String formatTextWithouNewLine(@NotNull StringBuffer s) {
//    return s.toString().replace("\r", "").replaceAll("\\s+", " ").trim();
//  }

  protected static int getNumber(String number) {
    if (number != null) {
      try {
        return Integer.parseInt(number);
      } catch (NumberFormatException e) {
        return 0;
      }
    }
    return 0;
  }

  public static XMLReader createXmlReader(ContentHandler contentHandler, ErrorHandler errHandler, boolean validate)
    throws SAXException {
    final XMLReader xmlReader = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
    xmlReader.setContentHandler(contentHandler);
    xmlReader.setErrorHandler(errHandler);
    xmlReader.setFeature("http://xml.org/sax/features/validation", validate);
    return xmlReader;
  }

  protected XmlReportParser() {
    try {
      myXmlReader = createXmlReader(this, this, false);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  public void logReportTotals(@NotNull ReportFileContext context, boolean verbose) {
  }

  void logParsingTotals(@NotNull final SessionContext sessionContext, @NotNull Map<String, String> parameters, boolean verbose) {
  }

  protected final void doSAXParse(@NotNull ReportFileContext context) throws Exception {
    clearCData();
    myXmlReader.parse(new InputSource(context.getFile().toURI().toString()));
  }

  protected void clearCData() {
    if (myCData != null) {
      myCData.delete(0, myCData.length());
    }
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    if (myCData != null) {
      myCData.append(ch, start, length);
    }
  }

  public abstract void parse(@NotNull ReportFileContext data) throws Exception;

  /**
   * This method is used to dispose the parser (ex. close streams, dispose loggers).
   * Will be called after parser done all it's work
   */
  public void dispose() {}

  public boolean supportOnTheFlyParsing() {
    return false;
  }
}
