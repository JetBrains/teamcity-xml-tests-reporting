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

import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.checkstyle.CheckstyleReportParser;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmdCpd.PmdCpdReportParser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 13.12.10
 * Time: 23:41
 */
public class Parsers {
  @NotNull
  private final XmlReportPluginParameters myParameters;

  @NotNull
  private final Map<String, XmlReportParser> myParsers = new HashMap<String, XmlReportParser>();

  public Parsers(@NotNull final XmlReportPluginParameters parameters) {
    myParameters = parameters;
  }

  @NotNull
  public XmlReportParser getParser(@NotNull String type) {
    if (!myParsers.containsKey(type)) {
     initializeParser(type);
    }
    return myParsers.get(type);
  }

  private void initializeParser(@NotNull String type) {
    myParsers.put(type, createParser(myParameters, type));
  }

  @NotNull
  private XmlReportParser createParser(@NotNull XmlReportPluginParameters parameters, @NotNull String type) {
    if (AntJUnitReportParser.TYPE.equals(type) || ("surefire".equals(type)))
      return new AntJUnitReportParser();

    if (NUnitReportParser.TYPE.equals(type))
      return new NUnitReportParser(myParameters.getLogger().getThreadLogger(), parameters.getTmpDir(), parameters.getNUnitSchema());

    if (XmlReportPluginUtil.isInspection(type)) {
      final InspectionReporter inspectionsReporter = parameters.getInspectionReporter();
      if(inspectionsReporter == null) {
        throw new RuntimeException("Inspection reporter not provided. Required for parser type: " + type);
      } else {
      // inspectionsReporter is needed for all parsers below
        if (FindBugsReportParser.TYPE.equals(type))
          return new FindBugsReportParser(inspectionsReporter, parameters.getCheckoutDir(), parameters.getFindBugsHome());

        if (PmdReportParser.TYPE.equals(type))
          return new PmdReportParser(inspectionsReporter, parameters.getCheckoutDir());

        if (CheckstyleReportParser.TYPE.equals(type))
          return new CheckstyleReportParser(inspectionsReporter, parameters.getCheckoutDir());
      }
    }

    if (XmlReportPluginUtil.isDuplication(type)) {
      final DuplicatesReporter duplicatesReporter = parameters.getDuplicatesReporter();
      if(duplicatesReporter == null) {
        throw new RuntimeException("Duplicates reporter not provided. Required for parser type: " + type);
      } else {
      // duplicatesReporter is needed for all parsers below
        if (PmdCpdReportParser.TYPE.equals(type))
          return new PmdCpdReportParser(duplicatesReporter, parameters.getCheckoutDir());
      }
    }

    throw new RuntimeException("No parser for " + type + " available");
  }

  public void doWithParsers(@NotNull final Processor processor) {
   for (final XmlReportParser parser : myParsers.values()) {
     processor.process(parser);
   }
  }

  public static interface Processor {
    public void process(@NotNull XmlReportParser parser);
  }
}
