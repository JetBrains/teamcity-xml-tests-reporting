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
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 15:23
 */
public class ParserUtils {
  private static final String DEFAULT_PARSER = "com.sun.org.apache.xerces.internal.parsers.SAXParser";

  @NotNull
  public static XMLReader createXmlReader(boolean validate) throws SAXException {
    final XMLReader xmlReader = createXmlReader();

    setValidationFeature(validate, xmlReader);

    return xmlReader;
  }

  @NotNull
  public static XMLReader createXmlReader(@NotNull ContentHandler contentHandler,
                                          @NotNull ErrorHandler errorHandler,
                                          boolean validate) throws SAXException {
    final XMLReader xmlReader = createXmlReader(validate);

    xmlReader.setContentHandler(contentHandler);
    xmlReader.setErrorHandler(errorHandler);
    return xmlReader;
  }

  @NotNull
  private static XMLReader createXmlReader() throws SAXException {
    try {
      return XMLReaderFactory.createXMLReader(DEFAULT_PARSER);
    } catch (Exception e) {
      LoggingUtils.LOG.warn("Failed to load " + DEFAULT_PARSER, e);
      return XMLReaderFactory.createXMLReader();
    }
  }

  private static void setValidationFeature(boolean validate, @NotNull XMLReader xmlReader) throws SAXNotRecognizedException, SAXNotSupportedException {
    try {
      xmlReader.setFeature("http://xml.org/sax/features/validation", validate);
    } catch (Exception e) {
      LoggingUtils.LOG.warn("Failed to set validation: " + validate, e);
    }
  }

  @NotNull
  public static String formatText(@NotNull StringBuilder s) {
    return s.toString().replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").replaceAll("<[a-z]>|</[a-z]>", "").trim();
  }

  public static boolean isReportComplete(@NotNull final File report, @NotNull String rootTag) {
    // here we preparse the report to check it's complete
    final CompleteReportHandler handler = new CompleteReportHandler(rootTag);
    try {
      final XMLReader reader = createXmlReader(handler, handler, false);
      reader.parse(new InputSource(report.toURI().toString()));
      return handler.isReportComplete();
    } catch (SAXParseException e) {
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  private static final class CompleteReportHandler extends DefaultHandler {
    @NotNull
    private final String myRootTag;
    private boolean myReportComplete = false;

    private CompleteReportHandler(@NotNull String rootTag) {
      myRootTag = rootTag;
    }

    public boolean isReportComplete() {
      return myReportComplete;
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (myRootTag.equals(localName)) myReportComplete = true;
    }
  }
}
