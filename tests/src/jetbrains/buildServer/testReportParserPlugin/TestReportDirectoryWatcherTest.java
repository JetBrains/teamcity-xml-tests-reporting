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

import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
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
        oneOf(runningBuild).getRunnerParameters();
        will(returnValue(runParams));
        oneOf(runningBuild).getWorkingDirectory();
        will(returnValue(workingDirFile));
      }
    });
    return runningBuild;

  }

  private TestReportParserPlugin createTestReportParserPlugin() {
    return myContext.mock(TestReportParserPlugin.class);
  }

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private LinkedBlockingQueue<File> createLinkedBlockingQueue() {
    return myContext.mock(LinkedBlockingQueue.class);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
  }

//    @Test(expected = IllegalArgumentException.class)
//    public void testFirstConstructorNullArgument() {
//        final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(null, new ArrayList<File>(), createLinkedBlockingQueue());
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testSecondConstructorNullArgument() {
//        final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(createTestReportParserPlugin(), null, createLinkedBlockingQueue());
//    }
//
//    @Test(expected = IllegalArgumentException.class)
//    public void testThirdConstructorNullArgument() {
//        final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(createTestReportParserPlugin(), new ArrayList<File>(), null);
//    }

//  @Test
//  public void testIsStoppedAfterCreation() {
//    final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(createTestReportParserPlugin(), new ArrayList<File>(), createLinkedBlockingQueue());
//    assertFalse("Watcher:stopWatching() method not invoked, but watcher is stopped.", watcher.isStopped());
//  }

  @Test
  public void testLogDirectoryTotalsAfterCreation() {
    final BaseServerLoggerFacade logger = createBaseServerLoggerFacade();
    myContext.checking(new Expectations() {
      {
        never(logger).warning(with(any(String.class)));
//                allowing (any(BaseServerLoggerFacade.class)).method(".*");
//                allowing (never(any(Object.class))).method(".*");
//                allowing (never(logger)).warning(with(any(String.class)));
//                never(logger);
      }
    });
    final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher(createTestReportParserPlugin(), new ArrayList<File>(), createLinkedBlockingQueue());
    watcher.logDirectoriesTotals();
    myContext.assertIsSatisfied();
  }

//    @Test
//    public void testNoDirectoriesToWatch() throws InterruptedException {
//        final LinkedBlockingQueue<File> queue = createLinkedBlockingQueue();
//        myContext.checking(new Expectations() {
//            {
//            }
//        });
//        final TestReportDirectoryWatcher watcher = new TestReportDirectoryWatcher();
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//                }
//                watcher.stopWatching();
//            }
//        }).start();
//        watcher.run();
//
//        myContext.checking(new Expectations() {
//            {
//                oneOf(queue).take(); will(returnValue(null));
//            }
//        });
//
//        myContext.assertIsSatisfied();
//    }
}
