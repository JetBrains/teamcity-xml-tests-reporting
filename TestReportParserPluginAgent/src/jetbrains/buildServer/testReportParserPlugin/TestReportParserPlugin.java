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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;


public class TestReportParserPlugin extends AgentLifeCycleAdapter {
    private static final String TEST_REPORT_DIR_VARIABLE = "system.TEST_REPORT_DIR";
    private static final Logger LOG = Loggers.AGENT;

    private BaseServerLoggerFacade myLogger;
    private TestReportDirectoryWatcher myDirectoryWatcher;
    private TestReportProcessor myReportProcessor;

    private boolean myTestReportParsingEnabled = false;
    private long myBuildStartTme;


    public TestReportParserPlugin(@NotNull final EventDispatcher<AgentLifeCycleListener> agentDispatcher) {
        agentDispatcher.addListener(this);
    }

    public static void log(String message) {
        LOG.debug("T-R-P-PLUGIN: " + Thread.currentThread().getId() + ": " + message);
    }

    public void buildStarted(@NotNull AgentRunningBuild agentRunningBuild) {
        myBuildStartTme = new Date().getTime();
        BuildProgressLogger logger = agentRunningBuild.getBuildLogger();

        if (logger instanceof BaseServerLoggerFacade) {
            myLogger = (BaseServerLoggerFacade) logger;
        } else {
            // not expected
        }
    }

    public void beforeRunnerStart(@NotNull AgentRunningBuild agentRunningBuild) {
        final Map<String, String> buildParameters = agentRunningBuild.getBuildParameters();
        final Map<String, String> runParameters = agentRunningBuild.getRunParameters();
        final List<File> reportDirs = getReportDirsFromDirsString(buildParameters.get(TEST_REPORT_DIR_VARIABLE), agentRunningBuild.getWorkingDirectory());

        log("BEFORE " + myTestReportParsingEnabled);
        myTestReportParsingEnabled = isTestReportParsingEnabled(runParameters);
        log("AFTER " + myTestReportParsingEnabled);

        LinkedBlockingQueue<File> queue = new LinkedBlockingQueue<File>();
        myDirectoryWatcher = new TestReportDirectoryWatcher(reportDirs, queue, myLogger, myBuildStartTme);
        myReportProcessor = new TestReportProcessor(queue, myDirectoryWatcher, myLogger);

        if (myTestReportParsingEnabled) {
            new Thread(myDirectoryWatcher, "TestReportParserPlugin-DirectoryWatcher").start();
            new Thread(myReportProcessor, "TestReportParserPlugin-ReportParser").start();
        }
    }

    //dirsStr is not supposed to contain ';' in their path, as it is separator
    private List<File> getReportDirsFromDirsString(@NotNull String dirsStr, final File workingDir) {
        final String separator = ";";
        final List<File> dirs = new ArrayList<File>();

        if (!dirsStr.endsWith(separator)) {
            dirsStr += separator;
        }

        int from = 0;
        int to = dirsStr.indexOf(separator);

        while (to != -1) {
            if (workingDir == null) {
                log("WD IS NULL");
            } else {
                dirs.add(FileUtil.resolvePath(workingDir, dirsStr.substring(from, to)));
            }

            from = to + 1;
            to = dirsStr.indexOf(separator, from);
        }
        return dirs;
    }

    public void beforeBuildFinish(@NotNull BuildFinishedStatus buildFinishedStatus) {
        if (!myTestReportParsingEnabled) {
            return;
        }

        myDirectoryWatcher.stopWatching();
        myReportProcessor.stopProcessing();

        switch (buildFinishedStatus) {
            case DOES_NOT_EXIST:
                myLogger.warning("Build finished with not existing status.");
                break;
            case INTERRUPTED:
                myLogger.warning("Build interrupted. TestReportParserPlugin may not finish it's work.");
                break;
            case FINISHED:
                synchronized (myReportProcessor) {
                    while (!myReportProcessor.isProcessingFinished()) {
                        try {
                            myReportProcessor.wait();
                            TestReportParserPlugin.log("Plugin after waiting for processor");
                        } catch (InterruptedException e) {
                            myLogger.warning("TestReportProcessor thread interrupted.");
                        }
                    }
                }
                break;
        }
    }
}