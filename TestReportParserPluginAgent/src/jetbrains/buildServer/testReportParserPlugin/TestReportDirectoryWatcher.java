/*
 * Copyright 2000-2007 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin.createBuildLogMessage;
import jetbrains.buildServer.testReportParserPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportDirectoryWatcher implements Runnable {
    private static final int SCAN_INTERVAL = 50;

    private final LinkedBlockingQueue<File> myReportQueue;
    private final Set<File> myDirectories;
    private final Set<File> myActiveDirectories;
    private final List<String> myProcessedFiles;
    private final BaseServerLoggerFacade myLogger;
    private final long myBuildStartTime;

    private volatile boolean myStopWatching;
    private volatile boolean myStopped;


    public TestReportDirectoryWatcher(@NotNull final List<File> directories, @NotNull final LinkedBlockingQueue<File> queue, @NotNull final BaseServerLoggerFacade logger, long buildStartTime) {
        myDirectories = new LinkedHashSet<File>(directories);
        myActiveDirectories = new HashSet<File>();
        myReportQueue = queue;
        myStopWatching = false;
        myStopped = false;
        myProcessedFiles = new ArrayList<String>();
        myLogger = logger;
        myBuildStartTime = buildStartTime;
    }

    public void run() {
        while (!myStopWatching) {
            try {
                scanDirectories();
                Thread.sleep(SCAN_INTERVAL);
            } catch (InterruptedException e) {
                myLogger.warning(createBuildLogMessage("directory watcher thread interrupted."));
            }
        }
        scanDirectories();
        synchronized (this) {
            myStopped = true;
            this.notify();
        }
    }

    private void scanDirectories() {
        for (File dir : myDirectories) {
            if (dir.isDirectory()) {

                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; ++i) {
                    File report = files[i];

                    if (report.isFile() && (report.lastModified() > myBuildStartTime)) {
                        myActiveDirectories.add(dir);
                        if (!myProcessedFiles.contains(report.getPath())) {
                            if (report.canRead() && AntJUnitReportParser.isReportFileComplete(report)) {
                                myLogger.message(createBuildLogMessage("found report file " + report.getPath() + "."));
                                myProcessedFiles.add(report.getPath());
                                try {
                                    myReportQueue.put(report);
                                } catch (InterruptedException e) {
                                    myLogger.warning(createBuildLogMessage("directory watcher thread interrupted."));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void stopWatching() {
        myStopWatching = true;
    }

    public boolean isStopped() {
        return myStopped;
    }

    public void logDirectoryTotals() {
        if (myDirectories.removeAll(myActiveDirectories)) {
            for (File dir : myDirectories) {
                if (!dir.exists()) {
                    myLogger.warning(createBuildLogMessage(dir.getPath() + " directory didn't appear on disk during the build."));
                } else if (!dir.isDirectory()) {
                    myLogger.warning(createBuildLogMessage(dir.getPath() + " is not actually a directory."));
                } else {
                    myLogger.warning(createBuildLogMessage("no reports found in " + dir.getPath() + " directory."));
                }
            }
        }
    }
}