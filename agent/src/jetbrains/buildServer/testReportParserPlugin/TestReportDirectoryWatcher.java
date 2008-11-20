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

import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin.createBuildLogMessage;
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
      } catch (InterruptedException e) {
        myPlugin.getLogger().warning(createBuildLogMessage("directory watcher thread interrupted."));
      }
    }
    scanDirectories();
  }

  private void scanDirectories() {
    for (File dir : myDirectories) {
      if (dir.isDirectory()) {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; ++i) {
          File report = files[i];

          if (report.isFile() && (report.lastModified() > myPlugin.getBuildStartTime())) {
            myActiveDirectories.add(dir);
            if (!myProcessedFiles.contains(report.getPath()) &&
              report.canRead() &&
              AntJUnitReportParser.isReportFileComplete(report)) {
              myPlugin.getLogger().message(createBuildLogMessage("found report file " + report.getPath() + "."));
              myProcessedFiles.add(report.getPath());
              try {
                myReportQueue.put(report);
              } catch (InterruptedException e) {
                myPlugin.getLogger().warning(createBuildLogMessage("directory watcher thread interrupted."));
              }
            }
          }
        }
      }
    }
  }

  public void logDirectoryTotals() {
    if (myDirectories.removeAll(myActiveDirectories)) {
      for (File dir : myDirectories) {
        if (!dir.exists()) {
          myPlugin.getLogger().warning(createBuildLogMessage(dir.getPath() + " directory didn't appear on disk during the build."));
        } else if (!dir.isDirectory()) {
          myPlugin.getLogger().warning(createBuildLogMessage(dir.getPath() + " is not actually a directory."));
        } else {
          myPlugin.getLogger().warning(createBuildLogMessage("no reports found in " + dir.getPath() + " directory."));
        }
      }
    }
  }
}