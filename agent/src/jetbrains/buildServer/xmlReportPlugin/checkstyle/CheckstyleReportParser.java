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

package jetbrains.buildServer.xmlReportPlugin.checkstyle;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionInstance;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.InspectionsReportParser;
import jetbrains.buildServer.xmlReportPlugin.ReportData;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;

/**
 * User: vbedrosova
 * Date: 23.12.2009
 * Time: 16:11:49
 */
public class CheckstyleReportParser extends InspectionsReportParser {
  public static final String TYPE = "checkstyle";

  private File myCurrentReport;
  private String myCurrentFile;

  public CheckstyleReportParser(@NotNull final BaseServerLoggerFacade logger,
                                @NotNull InspectionReporter inspectionReporter,
                                @NotNull String checkoutDirectory) {
    super(logger, inspectionReporter, checkoutDirectory);
  }

  @Override
  public void parse(@NotNull ReportData data) {
    myCurrentReport = data.getFile();
    if (!isReportComplete(myCurrentReport, "</checkstyle>")) {
      data.setProcessedEvents(0);
      return;
    }
    try {
      parse(myCurrentReport);
    } catch (SAXParseException spe) {
      myLogger.error(myCurrentReport.getAbsolutePath() + " is not parsable by Checkstyle parser");
    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      myInspectionReporter.flush();
    }
    data.setProcessedEvents(-1);
  }

//  Handler methods

  public void startElement(String uri, String name, String qName, Attributes attributes) throws SAXException {
    if ("checkstyle".equals(name)) {
      XmlReportPlugin.LOG.info(specifyMessage("Parsing report of version " + attributes.getValue("version")));
    } else if ("file".equals(name)) {
      myCurrentFile = resolveSourcePath(attributes.getValue("name"));
    } else if ("error".equals(name)) {
      if (myCurrentFile == null) {
        XmlReportPlugin.LOG.error(specifyMessage("Unexpected report structure: error tag comes outside file tag"));
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

  public void endElement(String uri, String name, String qName) throws SAXException {
    if ("file".equals(name)) {
      myCurrentFile = null;
    } else if ("exception".equals(name)) {
      myLogger.error("Exception in report " + myCurrentReport.getAbsolutePath() + "\n" + myCData.toString().trim());
    }
    myCData.delete(0, myCData.length());
  }

  // Auxiliary methods

  private void reportInspectionType(Attributes attributes) {
    final String source = attributes.getValue("source");
    reportInspectionType(source, source, attributes.getValue("severity"), "From " + source);
  }

  private int getPriority(String severity) {
    if ("error".equals(severity)) {
      return 1;
    } else if ("warning".equals(severity)) {
      return 2;
    } else if ("info".equals(severity)) {
      return 3;
    } else {
      XmlReportPlugin.LOG.error(specifyMessage("Came across illegal severity value: " + severity));
      return 3;
    }
  }

  public String specifyMessage(String message) {
    return "<CheckstyleReportParser> " + message;
  }
}
