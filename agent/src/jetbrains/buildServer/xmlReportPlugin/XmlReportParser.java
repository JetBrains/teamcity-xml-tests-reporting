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
}