/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;


public abstract class XmlReportParser extends DefaultHandler implements Parser {
  @NotNull
  private final XMLReader myXmlReader;

  private StringBuilder myCData;

  protected XmlReportParser(@NotNull XMLReader xmlReader, boolean useCData) {
    myXmlReader = xmlReader;
    if (useCData) {
      myCData = new StringBuilder();
    }
  }

  protected void parse(@NotNull File file) throws ParsingException {
    myXmlReader.setContentHandler(this);
    myXmlReader.setErrorHandler(this);
    try {
      myXmlReader.parse(new InputSource(file.toURI().toString()));
    } catch (IOException e) {
      throw new ParsingException(e);
    } catch (SAXException e) {
      throw new ParsingException(e);
    }
  }

  @Override
  public void characters(char ch[], int start, int length) throws SAXException {
    if (myCData != null) {
      myCData.append(ch, start, length);
    }
  }

  protected void clearCData() {
    if (myCData != null) {
      myCData.delete(0, myCData.length());
    }
  }

  protected StringBuilder getCData() {
    return myCData;
  }

//  @NotNull
//  private static XMLReader createXmlReader(@NotNull ContentHandler contentHandler,
//                                           @NotNull ErrorHandler errorHandler,
//                                           boolean validate) throws SAXException {
//    final XMLReader xmlReader = createXmlReader();
//
//    xmlReader.setContentHandler(contentHandler);
//    xmlReader.setErrorHandler(errorHandler);
//
//    setValidationFeature(validate, xmlReader);
//
//    return xmlReader;
//  }
//
//  @NotNull
//  private static XMLReader createXmlReader() throws SAXException {
//    try {
//      return XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
//    } catch (Exception e) {
//      XmlReportPlugin.LOG.warn("Failed to load default SAXParser", e);
//      return XMLReaderFactory.createXMLReader();
//    }
//  }
//
//  private static void setValidationFeature(boolean validate, XMLReader xmlReader) throws SAXNotRecognizedException, SAXNotSupportedException {
//    try {
//      xmlReader.setFeature("http://xml.org/sax/features/validation", validate);
//    } catch (Exception e) {
//      XmlReportPlugin.LOG.warn("Failed to set validation: " + validate, e);
//    }
//  }
//
//  @NotNull
//  protected static String formatText(@NotNull StringBuilder s) {
//    return s.toString().replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").replaceAll("<[a-z]>|</[a-z]>", "").trim();
//  }
//
//  protected static int getNumber(String number) {
//    if (number != null) {
//      try {
//        return Integer.parseInt(number);
//      } catch (NumberFormatException e) {
//        return 0;
//      }
//    }
//    return 0;
//  }
//
//  protected XmlReportParser(@Nullable StringBuilder cdata) {
//    myCData = cdata;
//    try {
//      myXmlReader = createXmlReader(this, this, false);
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  protected final void doSAXParse(@NotNull File file) throws Exception {
//    clearCData();
//    myXmlReader.parse(new InputSource(file.toURI().toString()));
//  }
//
//  protected void clearCData() {
//    if (myCData != null) {
//      myCData.delete(0, myCData.length());
//    }
//  }
//
//  @Override
//  public void characters(char ch[], int start, int length) throws SAXException {
//    if (myCData != null) {
//      myCData.append(ch, start, length);
//    }
//  }
//
//  public StringBuilder getCData() {
//    return myCData;
//  }
//
//  public abstract ParsingResult parse(@NotNull File report, int processedEvents) throws Exception;
//
//  public void logReportTotals(@NotNull File report, boolean verbose) {
//
//  }
//

//  public boolean isReportComplete(@NotNull final File report) {
//    // here we preparse the report to check it's complete
//    final CompleteReportHandler handler = new CompleteReportHandler();
//    try {
//      final XMLReader reader = createXmlReader(handler, null, false);
//      reader.parse(new InputSource(report.toURI().toString()));
//      return handler.isReportComplete();
//    } catch (SAXParseException e) {
//      return false;
//    } catch (Exception e) {
//      return true;
//    }
//  }
//
//  private final class CompleteReportHandler extends DefaultHandler {
//    private boolean myReportComplete = false;
//
//    public boolean isReportComplete() {
//      return myReportComplete;
//    }
//
//    @Override
//    public void endElement(String uri, String localName, String qName) throws SAXException {
//      if (getRootTag().equals(localName)) myReportComplete = true;
//    }
//  }
//
//  @NotNull
//  protected abstract String getRootTag();
}