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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.testReportParserPlugin.antJUnit.AntJUnitReportParser;
import jetbrains.buildServer.testReportParserPlugin.findBugs.FindBugsReportParser;
import jetbrains.buildServer.testReportParserPlugin.findBugs.FindBugsReportParserMy;
import jetbrains.buildServer.testReportParserPlugin.nUnit.NUnitReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestReportProcessor extends Thread {
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final int TRIES_TO_PARSE = 100;
  private static final long SCAN_INTERVAL = 300;

  private final TestReportParserPlugin myPlugin;

  private final LinkedBlockingQueue<File> myReportQueue;
  private final TestReportDirectoryWatcher myWatcher;
  private final TestReportParser myParser;

  private ReportData myCurrentReport;

  public TestReportProcessor(@NotNull final TestReportParserPlugin plugin,
                             @NotNull final LinkedBlockingQueue<File> queue,
                             @NotNull final TestReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor");
    myPlugin = plugin;
    myReportQueue = queue;
    myWatcher = watcher;

    final String expectedReportType = plugin.getParameters().getReportType();

    if (AntJUnitReportParser.TYPE.equals(expectedReportType) || ("surefire".equals(expectedReportType))) {
      myParser = new AntJUnitReportParser(myPlugin.getLogger());

    } else if (NUnitReportParser.TYPE.equals(expectedReportType)) {
      myParser = new NUnitReportParser(myPlugin.getLogger(), myPlugin.getParameters().getTmpDir());

    } else if (FindBugsReportParser.TYPE.equals(expectedReportType)) {
      myParser = new FindBugsReportParserMy(myPlugin.getLogger().getBuildLogger(), myPlugin.getInspectionReporter(), myPlugin.getParameters().getRunnerWorkingDir().getPath());

    } else {
      myPlugin.getLogger().debugToAgentLog("No parser for " + expectedReportType + " available");
      myParser = null;
    }
  }

  public void run() {
    if (myParser == null) {
      return;
    }

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
  }

  private void processReport(ReportData report) {
    if (report == null) {
      return;
    }

    final long processedTests = myParser.parse(report.getFile(), report.getProcessedTests());
    if (processedTests != -1) {
      myCurrentReport.setProcessedTests(processedTests);

      if (myCurrentReport.getTriesToParse() == TRIES_TO_PARSE) {
        myPlugin.getLogger().debugToAgentLog("Unable to get full report from " + TRIES_TO_PARSE + " tries. File is supposed to have illegal structure or unsupported format");

        myParser.abnormalEnd();
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
      myParser.logReportTotals(report.getFile());
      myCurrentReport = null;
    }
  }

  private ReportData takeNextReport(long timeout) {
    if (myCurrentReport != null) {
      myCurrentReport.parsedOnceMore();
      return myCurrentReport;
    }

    try {
      final File file = myReportQueue.poll(timeout, TimeUnit.MILLISECONDS);

      if (file != null) {
        myPlugin.getLogger().message("Found report file: " + file.getPath());
        myCurrentReport = new ReportData(file);
        return myCurrentReport;
      }
    } catch (InterruptedException e) {
      myPlugin.getLogger().debugToAgentLog("Report processor thread interrupted");
    }

    return null;
  }

  private boolean allReportsProcessed() {
    return (myCurrentReport == null) && myReportQueue.isEmpty();
  }

  private static final class ReportData {
    private final File myFile;
    private long myProcessedTests;
    private long myPrevLength;
    private int myTriesToParse;

    public ReportData(@NotNull final File file) {
      myFile = file;
      myProcessedTests = 0;
      myTriesToParse = 0;
      myPrevLength = file.length();
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
  }
}
