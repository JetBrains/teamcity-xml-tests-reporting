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

package jetbrains.buildServer.xmlReportPlugin.pmd;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.InspectionsReportParser;
import jetbrains.buildServer.xmlReportPlugin.ParserUtils;
import jetbrains.buildServer.xmlReportPlugin.ParsingException;
import jetbrains.buildServer.xmlReportPlugin.ParsingResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;


public class PmdReportParser extends InspectionsReportParser {
  public static final String TYPE = "pmd";
  private static final String DEFAULT_MESSAGE = "No message";

  private String myCurrentFile;

  public PmdReportParser(@NotNull XMLReader xmlReader,
                         @NotNull InspectionReporter inspectionReporter,
                         @NotNull File checkoutDirectory,
                         @NotNull BuildProgressLogger logger) {
    super(xmlReader, inspectionReporter, checkoutDirectory, logger, true);
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "pmd")) {
      return false;
    }
    parse(file);
    return true;
  }

  //  Handler methods

  @Override
  public void startElement(String uri, String localName,
                           String qName, Attributes attributes) throws SAXException {
    if ("file".equals(localName)) {
      myCurrentFile = getRelativePath(attributes.getValue("name"), myCheckoutDirectory);
    } else if ("violation".equals(localName)) {
      myCurrentBug = new InspectionInstance();
      myCurrentBug.setLine(getNumber(attributes.getValue("beginline")));
      myCurrentBug.setInspectionId(attributes.getValue("rule"));
      myCurrentBug.setMessage(DEFAULT_MESSAGE);
      myCurrentBug.setFilePath(myCurrentFile);
      reportInspectionType(attributes);
      processPriority(getNumber(attributes.getValue("priority")));
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("violation".equals(localName)) {
      myCurrentBug.setMessage(ParserUtils.formatText(getCData()));
      myInspectionReporter.reportInspection(myCurrentBug);
    }
    clearCData();
  }

  // Auxiliary methods

  private void reportInspectionType(Attributes attributes) {
    final String id = attributes.getValue("rule");
    final String category = attributes.getValue("ruleset");
    reportInspectionType(id, id, category, category, myInspectionReporter);
  }
}