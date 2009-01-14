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
  private final Map<File, List<File>[]> myActiveDirectories;

  public TestReportDirectoryWatcher(@NotNull final TestReportParserPlugin plugin,
                                    @NotNull final List<File> directories,
                                    @NotNull final LinkedBlockingQueue<File> queue) {
    super("xml-report-plugin-DirectoryWatcher");

    myPlugin = plugin;
    myDirectories = new LinkedHashSet<File>(directories);
    myReportQueue = queue;
    myActiveDirectories = new HashMap<File, List<File>[]>();
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

  public synchronized void addDirectories(List<File> directories) {
    myDirectories.addAll(directories);
  }

  private synchronized void scanDirectories() {
    for (File dir : myDirectories) {
      if (dir.isDirectory()) {
        final File[] files = dir.listFiles();
        if ((files == null) || (files.length == 0)) continue;

        final List<File>[] processedFiles;

        if (!myActiveDirectories.containsKey(dir)) {
          processedFiles = new List[2];
          processedFiles[0] = new ArrayList<File>();
          processedFiles[1] = new ArrayList<File>();
        } else {
          processedFiles = myActiveDirectories.get(dir);
        }

        for (final File report : files) {
          if (!isFileOk(report)) {
            if (!processedFiles[1].contains(report)) {
              processedFiles[1].add(report);
            }
            continue;
          }

          if (!processedFiles[0].contains(report) && AntJUnitReportParser.isReportFileComplete(report)) {
            processedFiles[0].add(report);
            processedFiles[1].remove(report);

            try {
              myReportQueue.put(report);
            } catch (InterruptedException e) {
              myPlugin.getLogger().debugToAgentLog("Directory watcher thread interrupted");
            }

          }
        }
        if ((processedFiles[0].size() > 0) || (processedFiles[1].size() > 0)) {
          myActiveDirectories.put(dir, processedFiles);
        }
      }
    }
  }

  private boolean isFileOk(File report) {
//    return !(!report.isFile() || !report.canRead() || (report.lastModified() < myPlugin.getBuildStartTime()));
    return !(!report.isFile() || !report.canRead());
  }

  public void logDirectoriesTotals() {
    if (myDirectories.isEmpty()) return;
    for (File dir : myDirectories) {
      logDirectoryTotals(dir);
    }
  }

  private void logDirectoryTotals(File dir) {
    if (!dir.exists()) {
      myPlugin.getLogger().warning(dir.getPath() + " directory didn't appear on disk during the build");
    } else if (!dir.isDirectory()) {
      myPlugin.getLogger().warning(dir.getPath() + " is not actually a directory");
    } else if (!myActiveDirectories.containsKey(dir)) {
      myPlugin.getLogger().warning(dir.getPath() + ": no reports found in directory");
    } else {
      logActiveDirectoryTotals(dir);
    }
  }

  private void logActiveDirectoryTotals(File dir) {
    final List<File> processedFiles = myActiveDirectories.get(dir)[0];
    final List<File> unprocessedFiles = myActiveDirectories.get(dir)[1];
    final int fileNumber = processedFiles.size() + unprocessedFiles.size();

    String message = dir.getPath() + " directory: " + fileNumber + " files(s) found";

    if (unprocessedFiles.size() > 0) {
      message = message.concat(", " + unprocessedFiles.size() + " of them unprocessed (see reasons below):");
    }
    myPlugin.getLogger().message(message);

    logUnprocessedFilesTotals(unprocessedFiles);
  }

  private void logUnprocessedFilesTotals(List<File> unprocessedFiles) {
    for (File file : unprocessedFiles) {
      if (!file.isFile()) {
        myPlugin.getLogger().warning(file.getPath() + " is not actually a file");
        continue;
      }
      if (!file.canRead()) {
        myPlugin.getLogger().warning(file.getPath() + ": unable to read file");
        continue;
      }
      if (file.lastModified() < myPlugin.getBuildStartTime()) {
        myPlugin.getLogger().warning(file.getPath() + " file has modification date preceding build start time");
      }
    }
  }
}