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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportDirectoryWatcher extends Thread {
  private static final int SCAN_INTERVAL = 50;

  private final TestReportParserPlugin myPlugin;

  private final LinkedBlockingQueue<File> myReportQueue;
  private final Set<File> myDirectories;
  private final Set<File> myActiveDirectories;
  private final List<String> myProcessedFiles;

  public TestReportDirectoryWatcher(@NotNull final TestReportParserPlugin plugin,
                                    @NotNull final List<File> directories,
                                    @NotNull final LinkedBlockingQueue<File> queue) {
    super("xml-report-plugin-DirectoryWatcher");

    myPlugin = plugin;
    myDirectories = new LinkedHashSet<File>(directories);
    myActiveDirectories = new HashSet<File>();
    myReportQueue = queue;
    myProcessedFiles = new ArrayList<String>();
  }

  public void run() {
    while (!myPlugin.isStopped()) {
      try {
        scanDirectories();
        Thread.sleep(SCAN_INTERVAL);
      } catch (Throwable e) {
        myPlugin.getLogger().exception(e);
      }
    }

    scanDirectories();
  }

  private synchronized void scanDirectories() {
    for (File dir : myDirectories) {
      if (dir.isDirectory()) {
        final File[] files = dir.listFiles();
        if (files == null) continue;

        for (final File report : files) {
          if (report.isFile() && (report.lastModified() >= myPlugin.getBuildStartTime())) {
            myActiveDirectories.add(dir);

            if (!myProcessedFiles.contains(report.getPath()) && report.canRead() &&
              AntJUnitReportParser.isReportFileComplete(report)) {

              myProcessedFiles.add(report.getPath());

              try {
                myReportQueue.put(report);
              } catch (InterruptedException e) {
                myPlugin.getLogger().debugToAgentLog("directory watcher thread interrupted.");
              }

            }
          }
        }
      }
    }
  }

  public synchronized void addDirectories(List<File> directories) {
    myDirectories.addAll(directories);
  }

  public void logDirectoryTotals() {
    if (myDirectories.isEmpty()) return;
    if (myDirectories.size() != myActiveDirectories.size()) {
      for (File dir : myDirectories) {
        if (!dir.exists()) {
          myPlugin.getLogger().warning(dir.getPath() + " directory didn't appear on disk during the build.");
        } else if (!dir.isDirectory()) {
          myPlugin.getLogger().warning(dir.getPath() + " is not actually a directory.");
        } else if (!myActiveDirectories.contains(dir)) {
          myPlugin.getLogger().warning("no reports found in " + dir.getPath() + " directory.");
        }
      }
    }
  }
}