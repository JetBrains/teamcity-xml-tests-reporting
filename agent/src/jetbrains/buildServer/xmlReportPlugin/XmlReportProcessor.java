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

package jetbrains.buildServer.xmlReportPlugin;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.xmlReportPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.xmlReportPlugin.nUnit.NUnitReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class XmlReportProcessor extends Thread {
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final int TRIES_TO_PARSE = 100;
  private static final long SCAN_INTERVAL = 300;

  private final XmlReportPlugin myPlugin;

  private final LinkedBlockingQueue<Pair<String, File>> myReportQueue;
  private final XmlReportDirectoryWatcher myWatcher;
  private final Map<String, XmlReportParser> myParsers;

  private ReportData myCurrentReport;

  public XmlReportProcessor(@NotNull final XmlReportPlugin plugin,
                            @NotNull final LinkedBlockingQueue<Pair<String, File>> queue,
                            @NotNull final XmlReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor");
    myPlugin = plugin;
    myReportQueue = queue;
    myWatcher = watcher;
    myParsers = new HashMap<String, XmlReportParser>();
  }

  public void run() {
    myCurrentReport = null;

    while (!myPlugin.isStopped()) {
      processReport(takeNextReport(FILE_WAIT_TIMEOUT));
    }
    try {
      myWatcher.join();
    } catch (InterruptedException e) {
      myPlugin.getLogger().debugToAgentLog("Report processor thread interrupted");
    }
    while (!allReportsProcessed()) {
      processReport(takeNextReport(1));
    }
    for (String type : myParsers.keySet()) {
      myParsers.get(type).logParsingTotals(myPlugin.getParameters());
    }
  }

  private void processReport(ReportData report) {
    if (report == null) {
      return;
    }
    final XmlReportParser parser = myParsers.get(report.getType());
    final long processedTests = parser.parse(report.getFile(), report.getProcessedTests());
    if (processedTests != -1) {
      myCurrentReport.setProcessedTests(processedTests);

      if (myCurrentReport.getTriesToParse() == TRIES_TO_PARSE) {
        myPlugin.getLogger().debugToAgentLog("Unable to get full report from " + TRIES_TO_PARSE + " tries. File is supposed to have illegal structure or unsupported format");
        parser.abnormalEnd();
        myPlugin.getLogger().warning(report.getFile().getPath() + " report has unexpected finish or unsupported format");
        myCurrentReport = null;
      } else {
        final long currLength = myCurrentReport.getFile().length();
        final long prevLength = myCurrentReport.getPrevLength();
        if (currLength > prevLength) {
          myCurrentReport.setPrevLength(currLength);
          myCurrentReport.setTriesToParse(0);
        }
        try {
          Thread.sleep(SCAN_INTERVAL);
        } catch (InterruptedException e) {
          myPlugin.getLogger().debugToAgentLog("Report processor thread interrupted");
        }
      }
    } else {
      parser.logReportTotals(report.getFile());
      myCurrentReport = null;
    }
  }

  private ReportData takeNextReport(long timeout) {
    if (myCurrentReport != null) {
      myCurrentReport.parsedOnceMore();
      return myCurrentReport;
    }

    try {
      final Pair<String, File> pair = myReportQueue.poll(timeout, TimeUnit.MILLISECONDS);
      if (pair == null) {
        return null;
      }
      final String reportType = pair.getFirst();
      final File report = pair.getSecond();

      if (!myParsers.containsKey(reportType)) {
        initializeParser(reportType);
      }

      myPlugin.getLogger().message("Found report file: " + report.getAbsolutePath());
      myCurrentReport = new ReportData(report, reportType);
      return myCurrentReport;
    } catch (InterruptedException e) {
      myPlugin.getLogger().debugToAgentLog("Report processor thread interrupted");
    }
    return null;
  }

  private boolean allReportsProcessed() {
    return (myCurrentReport == null) && myReportQueue.isEmpty();
  }

  private void initializeParser(String type) {
    if (AntJUnitReportParser.TYPE.equals(type) || ("surefire".equals(type))) {
      myParsers.put(type, new AntJUnitReportParser(myPlugin.getLogger()));

    } else if (NUnitReportParser.TYPE.equals(type)) {
      myParsers.put(type, new NUnitReportParser(myPlugin.getLogger(), myPlugin.getTmpDir()));

    } else if (FindBugsReportParser.TYPE.equals(type)) {
      myParsers.put(type, new FindBugsReportParser(myPlugin.getLogger().getBuildLogger(), myPlugin.getInspectionReporter(), myPlugin.getWorkingDir()));

    } else {
      myPlugin.getLogger().debugToAgentLog("No parser for " + type + " available");
    }
  }

  private static final class ReportData {
    private final File myFile;
    private long myProcessedTests;
    private long myPrevLength;
    private int myTriesToParse;
    private String myType;

    public ReportData(@NotNull final File file, String type) {
      myFile = file;
      myProcessedTests = 0;
      myTriesToParse = 0;
      myPrevLength = file.length();
      myType = type;
    }

    public File getFile() {
      return myFile;
    }

    public long getProcessedTests() {
      return myProcessedTests;
    }

    public void setProcessedTests(long tests) {
      myProcessedTests = tests;
    }

    public int getTriesToParse() {
      return myTriesToParse;
    }

    public void setTriesToParse(int triesToParse) {
      myTriesToParse = triesToParse;
    }

    public void parsedOnceMore() {
      ++myTriesToParse;
    }

    public long getPrevLength() {
      return myPrevLength;
    }

    public void setPrevLength(long prevLength) {
      myPrevLength = prevLength;
    }

    public String getType() {
      return myType;
    }
  }
}
