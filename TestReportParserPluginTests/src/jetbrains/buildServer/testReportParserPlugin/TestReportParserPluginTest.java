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

import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.util.EventDispatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginTest {
    private static final String WORKING_DIR = "workingDir";

    private TestReportParserPlugin myPlugin;
    private Mockery myContext;
    private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;

    private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File workingDirFile) {
        final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
        myContext.checking(new Expectations() {
            {
                allowing(runningBuild).getRunParameters();
                will(returnValue(runParams));
                allowing(runningBuild).getWorkingDirectory();
                will(returnValue(workingDirFile));
            }
        });
        return runningBuild;
    }

    private BaseServerLoggerFacade createBaseServerLoggerFacade() {
        return myContext.mock(BaseServerLoggerFacade.class);
    }

    @Before
    public void setUp() {
        myContext = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    }

    private void isSilentWhenDisabled(BuildFinishedStatus status) {
        Map<String, String> runParams = new HashMap<String, String>();
        File workingDir = new File(WORKING_DIR);
        TestReportParserPluginUtil.enableTestReportParsing(runParams, false);

        final AgentRunningBuild runningBuild = createAgentRunningBuild(runParams, workingDir);
        myPlugin = new TestReportParserPlugin(myEventDispatcher);

        myEventDispatcher.getMulticaster().buildStarted(runningBuild);
        myEventDispatcher.getMulticaster().beforeRunnerStart(runningBuild);
        myEventDispatcher.getMulticaster().beforeBuildFinish(status);
        myContext.assertIsSatisfied();
    }

    @Test
    public void testIsSilentWhenDisabledNormalFinish() {
        isSilentWhenDisabled(BuildFinishedStatus.FINISHED);
    }


    @Test
    public void testIsSilentWhenDisabledInterruptedFinish() {
        isSilentWhenDisabled(BuildFinishedStatus.INTERRUPTED);
    }

    @Test
    public void testIsSilentWhenDisabledDoesNotExistFinish() {
        isSilentWhenDisabled(BuildFinishedStatus.DOES_NOT_EXIST);
    }
}
