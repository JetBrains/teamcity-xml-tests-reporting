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

import jetbrains.buildServer.agent.*;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.isTestReportParsingEnabled;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter {
    //    private static final Logger LOG = Loggers.AGENT;
    private static final String PLUGIN_LOG_PREFIX = "TestReportParserPlugin: ";
    private static final String TEST_REPORT_DIR_PROPERTY = "testReportParsing.reportDirs";

    private BaseServerLoggerFacade myLogger;
    private TestReportDirectoryWatcher myDirectoryWatcher;
    private TestReportProcessor myReportProcessor;

    private boolean myTestReportParsingEnabled = false;
    private long myBuildStartTime;

    private volatile boolean myStopped;

    public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
        agentDispatcher.addListener(this);
    }

    public static String createBuildLogMessage(String message) {
        return PLUGIN_LOG_PREFIX + message;
    }

//    public static void log(String message) {
//        LOG.debug("T-R-P-PLUGIN: " + Thread.currentThread().getId() + ": " + message);
//    }

    public void buildStarted(@NotNull AgentRunningBuild agentRunningBuild) {
        myStopped = false;
        myBuildStartTime = new Date().getTime();
    }

    public void beforeRunnerStart(@NotNull AgentRunningBuild agentRunningBuild) {
        final Map<String, String> runParameters = agentRunningBuild.getRunParameters();
        myTestReportParsingEnabled = isTestReportParsingEnabled(runParameters);
        if (!myTestReportParsingEnabled) {
            return;
        }

        BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
        if (logger instanceof BaseServerLoggerFacade) {
            myLogger = (BaseServerLoggerFacade) logger;
        } else {
            // not expected
        }
        final String dir = runParameters.get(TEST_REPORT_DIR_PROPERTY);
        final File wd = agentRunningBuild.getWorkingDirectory();
        final List<File> reportDirs = getReportDirsFromDirsString(dir, wd);

        for (File s : reportDirs) {
            System.out.println(s.getPath());
        }
        if (reportDirs.size() == 0) {
            myLogger.warning(createBuildLogMessage("no report directories specified."));
        }

        LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<File>();
        myDirectoryWatcher = new TestReportDirectoryWatcher(this, reportDirs, queue);
        myReportProcessor = new TestReportProcessor(this, queue, myDirectoryWatcher);
        new Thread(myDirectoryWatcher, "TestReportParserPlugin-DirectoryWatcher").start();
        new Thread(myReportProcessor, "TestReportParserPlugin-ReportParser").start();
    }

    //dirsStr is not supposed to contain ';' in their path, as it is separator
    private static List<File> getReportDirsFromDirsString(String dirsStr, final File workingDir) {
        if ((dirsStr == null) || dirsStr.length() == 0) {
            return Collections.emptyList();
        }

        final String separator = ";";
        final List<File> dirs = new ArrayList<File>();

        if (!dirsStr.endsWith(separator)) {
            dirsStr += separator;
        }

        int from = 0;
        int to = dirsStr.indexOf(separator);

        while (to != -1) {
            dirs.add(FileUtil.resolvePath(workingDir, dirsStr.substring(from, to)));
            from = to + 1;
            to = dirsStr.indexOf(separator, from);
        }
        return dirs;
    }

    public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
        myStopped = true;

        if (!myTestReportParsingEnabled) {
            return;
        }

        switch (buildFinishedStatus) {
            case DOES_NOT_EXIST:
                myLogger.warning(createBuildLogMessage("build finished with not existing status."));
                break;
            case INTERRUPTED:
                myLogger.warning(createBuildLogMessage("build interrupted, plugin may not finish it's work."));
                break;
            case FINISHED:
                synchronized (myReportProcessor) {
                    while (!myReportProcessor.isProcessingFinished()) {
                        try {
                            myReportProcessor.wait();
                        } catch (InterruptedException e) {
                            myLogger.warning(createBuildLogMessage("plugin thread interrupted."));
                        }
                    }
                }
                myDirectoryWatcher.logDirectoryTotals();
                break;
        }
    }

    public BaseServerLoggerFacade getLogger() {
        return myLogger;
    }

    public long getBuildStartTime() {
        return myBuildStartTime;
    }

    public boolean isStopped() {
        return myStopped;
    }
}