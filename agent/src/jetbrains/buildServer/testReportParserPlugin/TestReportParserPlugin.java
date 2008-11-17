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

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.log.Loggers;
import static jetbrains.buildServer.testReportParserPlugin.TestReportParserPluginUtil.isTestReportParsingEnabled;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter {
    private static final Logger LOG = Loggers.AGENT;
    private static final String PLUGIN_LOG_PREFIX = "TestReportParserPlugin: ";
    private static final String TEST_REPORT_DIR_PROPERTY = "testReportParsing.reportDirs";

    private TestReportDirectoryWatcher myDirectoryWatcher;
    private TestReportProcessor myReportProcessor;
    private BaseServerLoggerFacade myLogger;

    private boolean myTestReportParsingEnabled = false;
    private long myBuildStartTime;

    private volatile boolean myStopped;


    public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
        agentDispatcher.addListener(this);
    }

    public static String createBuildLogMessage(String message) {
        return PLUGIN_LOG_PREFIX + message;
    }

    public static void log(String message) {
        LOG.debug("T-R-P-PLUGIN: " + Thread.currentThread().getId() + ": " + message);
    }

    public void buildStarted(@NotNull AgentRunningBuild agentRunningBuild) {
        myStopped = false;
        myBuildStartTime = new Date().getTime();
    }

    public void beforeRunnerStart(@NotNull AgentRunningBuild agentRunningBuild) {
        final Map<String, String> runnerParameters = agentRunningBuild.getRunnerParameters();

        myTestReportParsingEnabled = isTestReportParsingEnabled(runnerParameters);
        if (!myTestReportParsingEnabled) {
            return;
        }

        final BuildProgressLogger logger = agentRunningBuild.getBuildLogger();
        if (logger instanceof BaseServerLoggerFacade) {
            myLogger = (BaseServerLoggerFacade) logger;
        } else {
            // not expected
        }

        final String dirProperty = runnerParameters.get(TEST_REPORT_DIR_PROPERTY);
        final File workingDir = agentRunningBuild.getWorkingDirectory();
        final List<File> reportDirs = getReportDirsFromDirProperty(dirProperty, workingDir);

        if (reportDirs.size() == 0) {
            myLogger.warning(createBuildLogMessage("no report directories specified."));
        }

        final File f = new File("C:\\work\\TS\7964\\TeamCity\\buildAgent\\work\\TestProject\\reports\\ill");
        try {
            FileWriter fw = new FileWriter(f);
            fw.write("<<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\" ?>>\njvhbzfxdlbvhzdxfklbv;zdfoxvb;zodfxvh;zodfivnh;zodfivnh;ozdfivnhzdo;vzd'f");
        } catch (IOException e) {
        }

        LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<File>();
        myDirectoryWatcher = new TestReportDirectoryWatcher(this, reportDirs, queue);
        myReportProcessor = new TestReportProcessor(this, queue, myDirectoryWatcher);
        new Thread(myDirectoryWatcher, "TestReportParserPlugin-DirectoryWatcher").start();
        new Thread(myReportProcessor, "TestReportParserPlugin-ReportParser").start();
    }

    //dirs are not supposed to contain ';' in their path, as it is separator
    private static List<File> getReportDirsFromDirProperty(String dirProperty, final File workingDir) {
        if ((dirProperty == null) || dirProperty.length() == 0) {
            return Collections.emptyList();
        }

        final String separator = ";";
        final List<File> dirs = new ArrayList<File>();

        if (!dirProperty.endsWith(separator)) {
            dirProperty += separator;
        }

        int from = 0;
        int to = dirProperty.indexOf(separator);

        while (to != -1) {
            dirs.add(FileUtil.resolvePath(workingDir, dirProperty.substring(from, to)));
            from = to + 1;
            to = dirProperty.indexOf(separator, from);
        }
        return dirs;
    }

    public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
        myStopped = true;

        if (!myTestReportParsingEnabled) {
            return;
        }

        switch (buildFinishedStatus) {
            case INTERRUPTED:
                myLogger.warning(createBuildLogMessage("build interrupted, plugin may not finish it's work."));
            case FINISHED_SUCCESS:
            case FINISHED_FAILED:
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