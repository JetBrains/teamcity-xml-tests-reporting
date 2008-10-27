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

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import static junit.framework.Assert.assertFalse;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(JMock.class)
public class TestReportDirectoryWatcherTest {
    private Mockery myContext;

    private AgentRunningBuild createAgentRunningBuild(final Map<String, String> runParams, final File workingDirFile) {
        final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);
        myContext.checking(new Expectations() {
            {
                oneOf(runningBuild).getRunParameters();
                will(returnValue(runParams));
                oneOf(runningBuild).getWorkingDirectory();
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
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFirstConstructorNullArgument() {
        TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(null, new LinkedBlockingQueue<File>(), createBaseServerLoggerFacade(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSecondConstructorNullArgument() {
        TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(new ArrayList<File>(), null, createBaseServerLoggerFacade(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThirdConstructorNullArgument() {
        TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(new ArrayList<File>(), new LinkedBlockingQueue<File>(), null, 0);
    }

    @Test
    public void testIsStoppedAfterCreation() {
        TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(new ArrayList<File>(), new LinkedBlockingQueue<File>(), createBaseServerLoggerFacade(), 0);
        (new Thread(watcher)).start();
        assertFalse("Watcher:stopWatching() method not invoked, but watcher is stopped.", watcher.isStopped());
    }
}
