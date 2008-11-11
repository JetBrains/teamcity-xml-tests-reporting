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

package jetbrains.buildServer.testReportParserPlugin.performance;

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.testReportParserPlugin.TestReportParserPlugin;
import jetbrains.buildServer.util.EventDispatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginPerformanceTest {
    private static final String WORKING_DIR = "workingDir";

    private TestReportParserPlugin myPlugin;
    private AgentRunningBuild myRunningBuild;
    private Map<String, String> myRunParams;
    private File myWorkingDir;
    private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;
    private BaseServerLoggerFacade myTestLogger;

    private Mockery myContext;

    private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File workingDirFile, final BaseServerLoggerFacade logger) {
        final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
        myContext.checking(new Expectations() {
            {
                allowing(runningBuild).getBuildLogger();
                will(returnValue(logger));
                ignoring(runningBuild);
            }
        });
        return runningBuild;
    }

    @Before
    public void setUp() {
        myContext = new JUnit4Mockery();
        myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
        myRunParams = new HashMap<String, String>();
        myWorkingDir = new File(WORKING_DIR);
        myTestLogger = new BaseServerLoggerFacadeForTesting();
        myRunningBuild = createAgentRunningBuild(myRunParams, myWorkingDir, myTestLogger);
        myPlugin = new TestReportParserPlugin(myEventDispatcher);
    }

    @Test
    public void test() {
        myEventDispatcher.getMulticaster().buildStarted(myRunningBuild);
        myEventDispatcher.getMulticaster().beforeRunnerStart(myRunningBuild);
        myEventDispatcher.getMulticaster().beforeBuildFinish(BuildFinishedStatus.FINISHED);
        myContext.assertIsSatisfied();
    }

}