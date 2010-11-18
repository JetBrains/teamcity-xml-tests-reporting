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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.checkstyle.CheckstyleReportParser;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmdCpd.PmdCpdReportParser;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXParseException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class XmlReportProcessor extends Thread {
  public static final Logger LOG = Logger.getLogger(XmlReportProcessor.class);
  
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final long SCAN_INTERVAL = 500;

  private final LinkedBlockingQueue<ReportData> myReportQueue;
  private final XmlReportDirectoryWatcher myWatcher;
  private final Map<String, XmlReportParser> myParsers;

  private final Set<String> myFailedReportTypes;
  private final XmlReportPluginParameters myParameters;
  private FlowLogger myFlowLogger; // initialized on the thread start

  private volatile boolean myStopSignaled = false;

  public XmlReportProcessor(@NotNull final XmlReportPluginParameters parameters,
                            @NotNull final LinkedBlockingQueue<ReportData> queue,
                            @NotNull final XmlReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor");
    myReportQueue = queue;
    myWatcher = watcher;
    myParsers = new HashMap<String, XmlReportParser>();
    myFailedReportTypes = new HashSet<String>();
    myParameters = parameters;
  }

  @Override
  public void run() {
    myFlowLogger = myParameters.getLogger().getThreadLogger();
    try {
      while (!myStopSignaled) {
        processReport(takeNextReport(false));
      }
      try {
        myWatcher.join();
      } catch (InterruptedException e) {
        myFlowLogger.exception(e);
      }
      while (!myReportQueue.isEmpty()) {
        processReport(takeNextReport(true));
      }
      for (String type : myParsers.keySet()) {
        myParsers.get(type).logParsingTotals(new SessionContext() {
          @NotNull
          public BuildProgressLogger getLogger() {
            return myFlowLogger;
          }
        }, myParameters.getRunnerParameters(), myParameters.isVerbose());
      }
      if (!myFailedReportTypes.isEmpty()) {
        final StringBuffer types = new StringBuffer();
        for (final String t : myFailedReportTypes) {
          types.append(t).append(", ");
        }
        myFlowLogger.error("Failed to process some " + types.substring(0, types.length() - 2) + " reports");
      }
    } finally {
      disposeParsers();
      myFlowLogger.disposeFlow();
    }
  }

  public void signalStop() {
    myStopSignaled = true;
  }

  private void processReport(final ReportData data) {
    if (data == null) return;

    final boolean logAsInternal = myParameters.getPathParameters(data.getImportRequestPath()).isLogAsInternal();

    final BuildProgressLogger requestLogger =
      logAsInternal ?
        ((MessageTweakingSupport)myFlowLogger).getTweakedLogger(MessageInternalizer.MESSAGE_INTERNALIZER) :
        myFlowLogger;

    // TODO it's not efficient to create a context object each time a file is processed. Needs refactoring

    ImportRequestContextImpl requestContext = new ImportRequestContextImpl(data.getImportRequestPath(), requestLogger);
    ReportFileContextImpl reportContext = new ReportFileContextImpl(data, requestContext);

    final XmlReportParser parser = myParsers.get(data.getType());
    final String typeName = XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(data.getType());

    try {
      LOG.debug("Parsing " + data.getFile().getAbsolutePath() + " with " + data.getType() + " parser.");
      parser.parse(reportContext);
    } catch (SAXParseException e) {
      myFlowLogger.error(data.getFile().getAbsolutePath() + " is not parsable with " + typeName + " parser");
      if (myParameters.isVerbose()) requestLogger.exception(e);
      return;
    } catch (Exception e) {
      myFlowLogger.error("Exception occurred while parsing " + data.getFile().getAbsolutePath());
      if (myParameters.isVerbose()) requestLogger.exception(e);
      return;
    }

    if (data.getProcessedEvents() != -1) {
      if (myStopSignaled) {
        final String message = "Failed to parse " + data.getFile().getAbsolutePath() + " with " + typeName + " parser";
        LOG.error(message);
        myFlowLogger.error(message);
        myFailedReportTypes.add(typeName);
      } else {
        try {
          myReportQueue.put(data);
          Thread.sleep(SCAN_INTERVAL);
        } catch (InterruptedException e) {
          myFlowLogger.exception(e);
        }
      }
    } else {
      parser.logReportTotals(reportContext, myParameters.isVerbose());
    }
  }

  private ReportData takeNextReport(boolean finalParsing) {
    try {
      final ReportData data = finalParsing ? myReportQueue.poll() : myReportQueue.poll(FILE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
      if (data != null) {
        final long len = data.getFile().length();
        try {
          if (len > data.getFileLength() || finalParsing) {
            final String reportType = data.getType();
            if (!myParsers.containsKey(reportType)) {
              initializeParser(reportType);
            }
            if (myParsers.get(data.getType()).supportOnTheFlyParsing() || finalParsing || (!reportGrows(data) && isReportComplete(data))) {
              return data;
            }
          }
          myReportQueue.put(data);
          return null;
        } finally {
          data.setFileLength(len);
        }
      }
    } catch (InterruptedException e) {
      myFlowLogger.exception(e);
    }
    return null;
  }

  private boolean reportGrows(ReportData data) throws InterruptedException {
    if (!myParameters.checkReportGrows())
      return false;
    final long oldLength = data.getFile().length();
    for (int i = 0; i < 10; ++i) {
      Thread.sleep(10);
      final long newLength = data.getFile().length();
      if (newLength > oldLength) {
        return true;
      }
    }
    return false;
  }

  private boolean isReportComplete(ReportData data) {
    return !myParameters.checkReportComplete() || myParsers.get(data.getType()).isReportComplete(data.getFile());
  }

  private void initializeParser(String type) {
    myParsers.put(type, createParser(myParameters, type));
  }

  private XmlReportParser createParser(@NotNull XmlReportPluginParameters parameters, @NotNull String type) {
    if (AntJUnitReportParser.TYPE.equals(type) || ("surefire".equals(type)))
      return new AntJUnitReportParser();

    if (NUnitReportParser.TYPE.equals(type))
      return new NUnitReportParser(myFlowLogger, parameters.getTmpDir(), parameters.getNUnitSchema());

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

  private void disposeParsers() {
    for (final XmlReportParser parser : myParsers.values()) {
      parser.dispose();
    }
  }
}
