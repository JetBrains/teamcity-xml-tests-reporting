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

import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class XmlReportProcessor extends Thread {
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final int TRIES_TO_PARSE = 1000;
  private static final long SCAN_INTERVAL = 500;

  private final XmlReportPlugin myPlugin;

  private final LinkedBlockingQueue<ReportData> myReportQueue;
  private final XmlReportDirectoryWatcher myWatcher;
  private final Map<String, XmlReportParser> myParsers;

  public XmlReportProcessor(@NotNull final XmlReportPlugin plugin,
                            @NotNull final LinkedBlockingQueue<ReportData> queue,
                            @NotNull final XmlReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor");
    myPlugin = plugin;
    myReportQueue = queue;
    myWatcher = watcher;
    myParsers = new HashMap<String, XmlReportParser>();
  }

  public void run() {
    while (!myPlugin.isStopped()) {
      processReport(takeNextReport(FILE_WAIT_TIMEOUT));
    }
    try {
      myWatcher.join();
    } catch (InterruptedException e) {
    }
    while (!myReportQueue.isEmpty()) {
      processReport(takeNextReport(1));
    }
    for (String type : myParsers.keySet()) {
      myParsers.get(type).logParsingTotals(myPlugin.getParameters());
    }
  }

  private void processReport(final ReportData data) {
    if (data == null) {
      return;
    }
    final XmlReportParser parser = myParsers.get(data.getType());
    final int processedEvents = parser.parse(data);
    if (processedEvents != -1) {
      if ((data.getTriesToParse() == TRIES_TO_PARSE) || myPlugin.isStopped()) {
        if (myPlugin.isVerbose()) {
          myPlugin.getLogger().error(data.getFile().getAbsolutePath() + ": failed to parse with " +
            XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(data.getType()) + " parser");
        }
        XmlReportPlugin.LOGGER.info("Unable to parse " + data.getFile().getAbsolutePath() +
          " from " + data.getTriesToParse() + " ties");
      } else {
        final long currLength = data.getFile().length();
        final long prevLength = data.getPrevLength();
        if (currLength > prevLength) {
          data.setPrevLength(currLength);
          data.setTriesToParse(0);
        }
        try {
          myReportQueue.put(data);
          Thread.sleep(SCAN_INTERVAL);
        } catch (InterruptedException e) {
        }
      }
    } else {
      if (myPlugin.isVerbose()) {
        parser.logReportTotals(data.getFile());
      }
    }
  }

  private ReportData takeNextReport(long timeout) {
    try {
      final ReportData data = myReportQueue.poll(timeout, TimeUnit.MILLISECONDS);
      if (data != null) {
        final String reportType = data.getType();
        if (!myParsers.containsKey(reportType)) {
          initializeParser(reportType);
        }
        return data;
      }
    } catch (InterruptedException e) {
    }
    return null;
  }

  private void initializeParser(String type) {
    if (AntJUnitReportParser.TYPE.equals(type) || ("surefire".equals(type))) {
      myParsers.put(type, new AntJUnitReportParser(myPlugin.getLogger()));
    } else if (NUnitReportParser.TYPE.equals(type)) {
      myParsers.put(type, new NUnitReportParser(myPlugin.getLogger(), myPlugin.getTmpDir()));
    } else if (FindBugsReportParser.TYPE.equals(type)) {
      myParsers.put(type, new FindBugsReportParser(myPlugin.getLogger(), myPlugin.getInspectionReporter(), myPlugin.getCheckoutDir()));
    } else if (PmdReportParser.TYPE.equals(type)) {
      myParsers.put(type, new PmdReportParser(myPlugin.getLogger(), myPlugin.getInspectionReporter(), myPlugin.getCheckoutDir()));
    } else {
      XmlReportPlugin.LOGGER.debug("No parser for " + type + " available");
    }
  }
}
