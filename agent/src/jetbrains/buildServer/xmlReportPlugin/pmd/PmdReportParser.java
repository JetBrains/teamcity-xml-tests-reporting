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

package jetbrains.buildServer.xmlReportPlugin.pmd;

import jetbrains.buildServer.agent.SimpleBuildLogger;
import jetbrains.buildServer.agent.inspections.*;
import static jetbrains.buildServer.xmlReportPlugin.XmlParserUtil.*;
import jetbrains.buildServer.xmlReportPlugin.XmlReportParser;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.util.*;


public class PmdReportParser extends DefaultHandler implements XmlReportParser {
  public static final String TYPE = "pmd";

  private static final String DEFAULT_MESSAGE = "No message";

  private final SimpleBuildLogger myLogger;
  private final InspectionReporter myInspectionReporter;
  private final String myCheckoutDirectory;
  private Set<String> myReportedInstanceTypes;
  private XMLReader myXmlReader;

  private int myErrors;
  private int myWarnings;

  private String myCurrentFile;
  private InspectionInstance myCurrentBug;

  private StringBuffer myCData;

  public PmdReportParser(@NotNull final SimpleBuildLogger logger,
                         @NotNull InspectionReporter inspectionReporter,
                         @NotNull String checkoutDirectory) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
    myCheckoutDirectory = checkoutDirectory;
    myErrors = 0;
    myWarnings = 0;
    myReportedInstanceTypes = new HashSet<String>();
    try {
      myXmlReader = XMLReaderFactory.createXMLReader();
      myXmlReader.setContentHandler(this);
      myXmlReader.setFeature("http://xml.org/sax/features/validation", false);
    } catch (Exception e) {
      myLogger.exception(e);
    }
  }

  public void startElement(String uri, String localName,
                           String qName, Attributes attributes)
    throws SAXException {
    if ("file".equals(localName)) {
      myCurrentFile = attributes.getValue("name");
      if (myCurrentFile.startsWith(myCheckoutDirectory)) {
        myCurrentFile = myCurrentFile.substring(myCheckoutDirectory.length());
      }
      myCurrentFile = myCurrentFile.replace("\\", "|").replace("/", "|");
      if (myCurrentFile.startsWith("|")) {
        myCurrentFile = myCurrentFile.substring(1);
      }
    } else if ("violation".equals(localName)) {
      myCurrentBug = new InspectionInstance();
      myCurrentBug.setLine(getNumber(attributes.getValue("beginline")));
      myCurrentBug.setInspectionId(attributes.getValue("rule"));
      myCurrentBug.setMessage(DEFAULT_MESSAGE);
      String filePathSpec = attributes.getValue("package");
      final String className = attributes.getValue("class");
      if (className != null) {
        filePathSpec = filePathSpec + "." + className;
      }
      filePathSpec = filePathSpec.replace(File.separator, "/");
      if (myCurrentFile.length() > 0) {
        filePathSpec = filePathSpec + " :: " + myCurrentFile;
      }
      myCurrentBug.setFilePath(filePathSpec);
      reportInspectionType(attributes);
      processPriority(getNumber(attributes.getValue("priority")));
    }
  }

  private void reportInspectionType(Attributes attributes) {
    final String id = attributes.getValue("rule");
    if (!myReportedInstanceTypes.contains(id)) {
      final InspectionTypeInfo type = new InspectionTypeInfo();
      type.setId(id);
      type.setName(id);
      final String category = attributes.getValue("ruleset");
      type.setCategory(category);
      type.setDescription(category);
      myInspectionReporter.reportInspectionType(type);
    }
  }

  private void processPriority(int priority) {
    InspectionSeverityValues level;
    switch (priority) {
      case 1:
        ++myErrors;
        level = InspectionSeverityValues.ERROR;
        break;
      case 2:
        ++myWarnings;
        level = InspectionSeverityValues.WARNING;
        break;
      default:
        level = InspectionSeverityValues.INFO;
    }
    final Collection<String> attrValue = new Vector<String>();
    attrValue.add(level.toString());
    myCurrentBug.addAttribute(InspectionAttributesId.SEVERITY.toString(), attrValue);
  }


  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("violation".equals(localName)) {
      myCurrentBug.setMessage(formatText(myCData));
      myInspectionReporter.reportInspection(myCurrentBug);
    }
    myCData.delete(0, myCData.length());
  }

  public void characters(char ch[], int start, int length) throws SAXException {
    myCData.append(ch, start, length);
  }

  public int parse(@NotNull File report, int testsToSkip) {
    myCData = new StringBuffer();
    try {
      myXmlReader.parse(new InputSource(report.toURI().toString()));
    } catch (Exception e) {
      myLogger.exception(e);
    } finally {
      myInspectionReporter.flush();
    }
    return -1;
  }

  public boolean abnormalEnd() {
    return false;
  }

  public void logReportTotals(File report) {
  }

  public void logParsingTotals(Map<String, String> parameters) {
    boolean limitReached = false;

    final int errorLimit = XmlReportPluginUtil.getMaxErrors(parameters);
    if ((errorLimit != -1) && (myErrors > errorLimit)) {
      myLogger.error("Errors limit reached: found " + myErrors + " errors, limit " + errorLimit);
      limitReached = true;
    }

    final int warningLimit = XmlReportPluginUtil.getMaxWarnings(parameters);
    if ((warningLimit != -1) && (myWarnings > warningLimit)) {
      myLogger.error("Warnings limit reached: found " + myWarnings + " warnings, limit " + warningLimit);
      limitReached = true;
    }

    final String buildStatus = generateBuildStatus(myErrors, myWarnings);
    myLogger.message("##teamcity[buildStatus status='" +
      (limitReached ? "FAILURE" : "SUCCESS") +
      "' text='" + buildStatus + "']");
  }
}
