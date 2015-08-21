/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.utils;

import java.io.File;
import jetbrains.buildServer.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 15:23
 */
public class ParserUtils {

  @NotNull
  public static XMLReader createXmlReader(@NotNull ContentHandler contentHandler,
                                          @NotNull ErrorHandler errorHandler,
                                          boolean validate) throws SAXException {
    final XMLReader xmlReader = XmlUtil.createXMLReader(validate);

    xmlReader.setContentHandler(contentHandler);
    xmlReader.setErrorHandler(errorHandler);
    return xmlReader;
  }

  @NotNull
  public static String formatText(@NotNull String s) {
    return s.replace("&nbsp;", " ").replace("\r", "").replace("\n", " ").replaceAll("\\s+", " ").replaceAll("<[a-z]>|</[a-z]>", "").trim();
  }

  public static boolean isReportComplete(@NotNull final File report, @Nullable String rootTag) {
    // here we pre-parse the report to check it's complete
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
    private String myRootTag;
    private int myDepth = 0;
    private boolean myRightStart = false;
    private boolean myRightEnd = false;

    private CompleteReportHandler(@Nullable String rootTag) {
      myRootTag = rootTag;
    }

    public boolean isReportComplete() {
      return myRightStart && myRightEnd && myDepth == 0;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
      if (myDepth == 0) {
        if (myRootTag == null) myRootTag = localName;
        if (myRootTag.equals(localName)) myRightStart = true;
      }
      myDepth++;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      myDepth--;
      if (myDepth == 0 && myRootTag != null && myRootTag.equals(localName)) myRightEnd = true;
    }
  }
}
