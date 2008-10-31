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
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

@RunWith(JMock.class)
public class TestReportParserPluginTest {

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

    @Before
    public void setUp() {
        myContext = new JUnit4Mockery() {
//            {
//                setImposteriser(ClassImposteriser.INSTANCE);
//            }
        };
    }

    @Test
    public void test() {

    }
}
