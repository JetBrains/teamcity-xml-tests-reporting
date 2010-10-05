/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import junit.framework.Assert;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
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
public class XmlReportPluginTest {
  private static final String CHECKOUT_DIR = "workingDir";

  private XmlReportPlugin myPlugin;
  private Map<String, String> myRunParams;
  private BuildProgressLogger myLogger;
  private EventDispatcher<AgentLifeCycleListener> myEventDispatcher;

  private AgentRunningBuild myBuild;
  private BuildRunnerContext myRunner;

  private Mockery myContext;
  private Sequence mySequence;
  private InspectionReporter myInspectionReporter;

  private InspectionReporter createInspectionReporter() {
    return myContext.mock(InspectionReporter.class);
  }

  private AgentRunningBuild createAgentRunningBuild(final File checkoutDirFile) {
    final AgentRunningBuild runningBuild = myContext.mock(AgentRunningBuild.class);

    myContext.checking(new Expectations() {
      {

        allowing(runningBuild).getCheckoutDirectory();
        will(returnValue(checkoutDirFile));
        allowing(runningBuild).getBuildTempDirectory();
        allowing(runningBuild).getBuildLogger();
        will(returnValue(myLogger));
      }
    });
    return runningBuild;
  }

  private BuildRunnerContext createBuildRunnerContext(final Map<String, String> runnerParameters) {
    final BuildRunnerContext context = myContext.mock(BuildRunnerContext.class);

    myContext.checking(new Expectations() {
      {

        allowing(context).getRunnerParameters();
        will(returnValue(runnerParameters));
        allowing(context).getBuildParameters();
        will(returnValue(createBuildParametersMap()));
      }
    });
    return context;
  }

   private BuildParametersMap createBuildParametersMap() {
    final BuildParametersMap map = myContext.mock(BuildParametersMap.class);
    myContext.checking(new Expectations() {
      {
        allowing(map).getSystemProperties();
        will(returnValue(new HashMap<String, String>()));
      }
    });
    return map;
  }

  private BuildProgressLogger createBaseServerLoggerFacade() {
    final BuildProgressLogger logger = myContext.mock(FlowLogger.class);
    myContext.checking(new Expectations() {
      {
        allowing(logger).getThreadLogger();
        will(returnValue(logger));
      }
    });
    return logger;
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    mySequence = myContext.sequence("Log Sequence");
    myEventDispatcher = EventDispatcher.create(AgentLifeCycleListener.class);
    myRunParams = new HashMap<String, String>();
    myLogger = createBaseServerLoggerFacade();
    myInspectionReporter = createInspectionReporter();
    myContext.checking(new Expectations() {
      {
        ignoring(myInspectionReporter);
      }
    });


    final File checkoutDir = new File(CHECKOUT_DIR);
    FileUtil.delete(checkoutDir);
    checkoutDir.mkdirs();

    myBuild = createAgentRunningBuild(checkoutDir);

    myRunner = createBuildRunnerContext(myRunParams);
  }

  private void isSilentWhenDisabled(BuildFinishedStatus status) {
    XmlReportPluginUtil.enableXmlReportParsing(myRunParams, "");

    myPlugin = new XmlReportPlugin(myEventDispatcher, myInspectionReporter);

    myEventDispatcher.getMulticaster().beforeRunnerStart(myBuild, myRunner);
    myEventDispatcher.getMulticaster().runnerFinished(myBuild, myRunner, BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testIsSilentWhenDisabledNormalFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_SUCCESS);
  }


  @Test
  public void testIsSilentWhenDisabledInterruptedFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.INTERRUPTED);
  }

  @Test
  public void testIsSilentWhenDisabledDoesNotExistFinish() {
    isSilentWhenDisabled(BuildFinishedStatus.FINISHED_FAILED);
  }

  private void warningWhenZeroReportDirsSize() {
    myContext.checking(new Expectations() {
      {
//        oneOf(myLogger).message(with("##teamcity[buildStatus status='FAILURE' text='Report paths setting " +
//          "is not specified on XML Report Processing panel at build runner settings page.']"));
//        inSequence(mySequence);
        oneOf(myLogger).targetStarted(with(any(String.class)));
        inSequence(mySequence);
        oneOf(myLogger).warning(with("Watching paths: <no paths>"));
        inSequence(mySequence);
        oneOf(myLogger).targetFinished(with(any(String.class)));
        inSequence(mySequence);
      }
    });
    myPlugin = new XmlReportPlugin(myEventDispatcher, myInspectionReporter);

    myEventDispatcher.getMulticaster().beforeRunnerStart(myBuild, myRunner);
    myEventDispatcher.getMulticaster().runnerFinished(myBuild, myRunner, BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testWarningWhenReportDirsNull() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunParams, "junit");
    XmlReportPluginUtil.setVerboseOutput(myRunParams, true);

    warningWhenZeroReportDirsSize();
  }

  @Test
  public void testIsStoppedWhenDisabled() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunParams, "");

    myPlugin = new XmlReportPlugin(myEventDispatcher, myInspectionReporter);

    myEventDispatcher.getMulticaster().beforeRunnerStart(myBuild, myRunner);
    myEventDispatcher.getMulticaster().runnerFinished(myBuild, myRunner, BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();

    Assert.assertTrue("Plugin must be stopped", myPlugin.isStopped());
  }

  private void isStoppedWhenZeroReportDirsSize() {
    myContext.checking(new Expectations() {
      {
        ignoring(myBuild);
        ignoring(myLogger);
      }
    });
    myPlugin = new XmlReportPlugin(myEventDispatcher, myInspectionReporter);

    myEventDispatcher.getMulticaster().beforeRunnerStart(myBuild, myRunner);
    myEventDispatcher.getMulticaster().runnerFinished(myBuild, myRunner, BuildFinishedStatus.FINISHED_SUCCESS);
    myContext.assertIsSatisfied();

    Assert.assertTrue("Plugin must be stopped", myPlugin.isStopped());
  }

  @Test
  public void testIsStoppedWhenReportDirsEmpty() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunParams, "junit");

    isStoppedWhenZeroReportDirsSize();
  }

  @Test
  public void testIsStoppedWhenReportDirsNull() {
    XmlReportPluginUtil.enableXmlReportParsing(myRunParams, "junit");

    isStoppedWhenZeroReportDirsSize();
  }
}