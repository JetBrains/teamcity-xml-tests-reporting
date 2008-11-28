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

import com.intellij.openapi.diagnostic.Logger;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin.createBuildLogMessage;
import jetbrains.buildServer.testReportParserPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TestReportProcessor extends Thread {
  private static final long FILE_WAIT_TIMEOUT = 500;
  private static final int TRIES_TO_PARSE = 20;

  private static final Logger LOG = Logger.getInstance(TestReportProcessor.class.getName());

  private final TestReportParserPlugin myPlugin;

  private final LinkedBlockingQueue<File> myReportQueue;
  private final TestReportDirectoryWatcher myWatcher;
  private final AntJUnitReportParser myParser;

  private ReportData myCurrentReport;

  public TestReportProcessor(@NotNull final TestReportParserPlugin plugin,
                             @NotNull final LinkedBlockingQueue<File> queue,
                             @NotNull final TestReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportParser");
    myPlugin = plugin;
    myReportQueue = queue;
    myWatcher = watcher;
    myParser = new AntJUnitReportParser(myPlugin.getLogger());
  }

  public void run() {
    myCurrentReport = null;

    try {
      // workaround to show test suites in the build log
      myPlugin.getLogger().targetStarted("junit-report-parser");

      while (!myPlugin.isStopped()) {
        processReport(takeNextReport(FILE_WAIT_TIMEOUT));
      }
      try {
        myWatcher.join();
      } catch (InterruptedException e) {
        myPlugin.getLogger().warning(createBuildLogMessage("report processor thread interrupted."));
      }
      while (!allReportsProcessed()) {
        processReport(takeNextReport(1));
      }
    } finally {
      myPlugin.getLogger().targetFinished("junit-report-parser");
    }
  }

  private void processReport(ReportData report) {
    if (report == null) {
      return;
    }

    long processedTests = myParser.parse(report.getFile(), report.getProcessedTests());
    if (processedTests != -1) {
      myCurrentReport.setProcessedTests(processedTests);

      if (myCurrentReport.getTriesToParse() == TRIES_TO_PARSE) {
        System.out.println(myCurrentReport.getFile().getPath());
        LOG.debug("Unable to get full report from " + TRIES_TO_PARSE + " tries. File is supposed to have illegal structure or unsupported format.");

        if (myParser.abnormalEnd()) {
          myPlugin.getLogger().warning(createBuildLogMessage(report.getFile().getPath() + " report has unexpected finish."));
        } else {
          myPlugin.getLogger().warning(createBuildLogMessage(report.getFile().getPath() + " is not Ant JUnit report file."));
        }
        myCurrentReport = null;
      }
    } else {
      //myPlugin.getLogger().message(createBuildLogMessage(report.getFile().getPath() + " report processed."));
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
//        myPlugin.getLogger().message(createBuildLogMessage("found report file " + file.getPath() + "."));
        myCurrentReport = new ReportData(file);
        return myCurrentReport;
      }
    } catch (InterruptedException e) {
      myPlugin.getLogger().warning(createBuildLogMessage("report processor thread interrupted."));
    }

    return null;
  }

  private boolean allReportsProcessed() {
    return (myCurrentReport == null) && myReportQueue.isEmpty();
  }

  private static final class ReportData {
    private final File myFile;
    private long myProcessedTests;
    private int myTriesToParse;

    public ReportData(@NotNull final File file) {
      myFile = file;
      myProcessedTests = 0;
      myTriesToParse = 0;
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

    public void parsedOnceMore() {
      ++myTriesToParse;
    }
  }
}
