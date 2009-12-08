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

package jetbrains.buildServer.xmlReportPlugin;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin.LOG;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

class XmlReportProcessor extends Thread {
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final long SCAN_INTERVAL = 500;

  private final XmlReportPlugin myPlugin;

  private final LinkedBlockingQueue<ReportData> myReportQueue;
  private final XmlReportDirectoryWatcher myWatcher;
  private final Map<String, XmlReportParser> myParsers;

  private final List<String> myFailedReportTypes;

  public XmlReportProcessor(@NotNull final XmlReportPlugin plugin,
                            @NotNull final LinkedBlockingQueue<ReportData> queue,
                            @NotNull final XmlReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor");
    myPlugin = plugin;
    myReportQueue = queue;
    myWatcher = watcher;
    myParsers = new HashMap<String, XmlReportParser>();
    myFailedReportTypes = new ArrayList<String>();
  }

  @Override
  public void run() {
    while (!myPlugin.isStopped()) {
      processReport(takeNextReport(false));
    }
    try {
      myWatcher.join();
    } catch (InterruptedException e) {
      myPlugin.interrupted(e);
    }
    while (!myReportQueue.isEmpty()) {
      processReport(takeNextReport(true));
    }
    for (String type : myParsers.keySet()) {
      myParsers.get(type).logParsingTotals(myPlugin.getParameters(), myPlugin.isVerbose());
    }
    if (!myFailedReportTypes.isEmpty()) {
      StringBuffer types = new StringBuffer();
      for (final String t : myFailedReportTypes) {
        types.append(t).append(", ");
      }
      myPlugin.getLogger().message("##teamcity[buildStatus status='FAILURE' text='{build.status.text}. Failed to process some " + types.substring(0, types.length() - 2) + " reports']");
    }
  }

  private void processReport(final ReportData data) {
    if (data == null) {
      return;
    }
    final XmlReportParser parser = myParsers.get(data.getType());
    LOG.debug("Parsing " + data.getFile().getAbsolutePath() + " with " + data.getType() + " parser.");
    parser.parse(data);
    if (data.getProcessedEvents() != -1) {
      if (myPlugin.isStopped()) {
        final String typeName = XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(data.getType());
        myPlugin.getLogger().error("Failed to parse " + data.getFile().getAbsolutePath() + " with " + typeName + " parser.");
        myFailedReportTypes.add(typeName);
        XmlReportPlugin.LOG.info("Unable to parse " + data.getFile().getAbsolutePath());
      } else {
        try {
          myReportQueue.put(data);
          Thread.sleep(SCAN_INTERVAL);
        } catch (InterruptedException e) {
          myPlugin.interrupted(e);
        }
      }
    } else {
      parser.logReportTotals(data.getFile(), myPlugin.isVerbose());
    }
  }

  private ReportData takeNextReport(boolean finalParsing) {
    try {
      final ReportData data = finalParsing ? myReportQueue.poll() : myReportQueue.poll(FILE_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
      if (data != null) {
        final long len = data.getFile().length();
        try {
          if (len > data.getFileLength()) {
            final String reportType = data.getType();
            if (!myParsers.containsKey(reportType)) {
              initializeParser(reportType);
            }
            return data;
          } else {
            if (!finalParsing) {
              myReportQueue.put(data);
            } else {
              return data;
            }
            return null;
          }
        } finally {
          data.setFileLength(len);
        }
      }
    } catch (InterruptedException e) {
      myPlugin.interrupted(e);
    }
    return null;
  }

  private void initializeParser(String type) {
    if (AntJUnitReportParser.TYPE.equals(type) || ("surefire".equals(type))) {
      myParsers.put(type, new AntJUnitReportParser(myPlugin.getLogger()));
    } else if (NUnitReportParser.TYPE.equals(type)) {
      myParsers.put(type, new NUnitReportParser(myPlugin.getLogger(), myPlugin.getTmpDir()));
    } else if (FindBugsReportParser.TYPE.equals(type)) {
      myParsers.put(type, new FindBugsReportParser(myPlugin.getLogger(), myPlugin.getInspectionReporter(), myPlugin.getCheckoutDir(), myPlugin.getFindBugsHome()));
    } else if (PmdReportParser.TYPE.equals(type)) {
      myParsers.put(type, new PmdReportParser(myPlugin.getLogger(), myPlugin.getInspectionReporter(), myPlugin.getCheckoutDir()));
    } else {
      XmlReportPlugin.LOG.debug("No parser for " + type + " available");
    }
  }
}
