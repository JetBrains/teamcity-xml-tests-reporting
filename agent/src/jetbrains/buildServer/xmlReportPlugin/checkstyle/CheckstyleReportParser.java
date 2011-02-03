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

package jetbrains.buildServer.xmlReportPlugin.checkstyle;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 23.12.2009
 * Time: 16:11:49
 */
public class CheckstyleReportParser extends InspectionsReportParser {
  private File myCurrentReport;
  private String myCurrentFile;

  public CheckstyleReportParser(@NotNull XMLReader xmlReader,
                                   @NotNull InspectionReporter inspectionReporter,
                                   @NotNull File checkoutDirectory,
                                   @NotNull BuildProgressLogger logger) {
    super(xmlReader, inspectionReporter, checkoutDirectory, logger, true);
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "checkstyle")) {
      return false;
    }
    myCurrentReport = file;
    parse(file);
    return true;
  }

//  Handler methods

  @Override
  public void startElement(String uri, String name, String qName, Attributes attributes) throws SAXException {
    if ("checkstyle".equals(name)) {
      LoggingUtils.LOG.info(specifyMessage("Parsing report of version " + attributes.getValue("version")));
    } else if ("file".equals(name)) {
      myCurrentFile = getRelativePath(attributes.getValue("name"), myCheckoutDirectory);
    } else if ("error".equals(name)) {
      if (myCurrentFile == null) {
        LoggingUtils.LOG.warn(specifyMessage("Unexpected report structure: error tag comes outside file tag"));
      }
      reportInspectionType(attributes);

      myCurrentBug = new InspectionInstance();
      myCurrentBug.setFilePath(myCurrentFile);
      myCurrentBug.setLine(getNumber(attributes.getValue("line")));
      myCurrentBug.setMessage(attributes.getValue("message"));
      myCurrentBug.setInspectionId(attributes.getValue("source"));
      processPriority(getPriority(attributes.getValue("severity")));

      myInspectionReporter.reportInspection(myCurrentBug);
    }
  }

  @Override
  public void endElement(String uri, String name, String qName) throws SAXException {
    if ("file".equals(name)) {
      myCurrentFile = null;
    } else if ("exception".equals(name)) {
      myLogger.error("Exception in report " + myCurrentReport.getAbsolutePath() + "\n" + getCData().toString().trim());
    }
    clearCData();
  }

  // Auxiliary methods

  private void reportInspectionType(Attributes attributes) {
    final String source = attributes.getValue("source");
    reportInspectionType(source, source, attributes.getValue("severity"), "From " + source, myInspectionReporter);
  }

  private int getPriority(String severity) {
    if ("error".equals(severity)) {
      return 1;
    } else if ("warning".equals(severity)) {
      return 2;
    } else if ("info".equals(severity)) {
      return 3;
    } else {
      LoggingUtils.LOG.warn(specifyMessage("Came across illegal severity value: " + severity));
      return 3;
    }
  }

  private String specifyMessage(String message) {
    return "<CheckstyleReportParser> " + message;
  }
}