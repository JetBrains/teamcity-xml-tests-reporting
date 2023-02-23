/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.AgentServerFunctionalTestCase;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.impl.SpringContextFixture;
import jetbrains.buildServer.agent.impl.SpringContextXmlBean;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.serverSide.BuildStatisticsOptions;
import jetbrains.buildServer.serverSide.BuildTypeEx;
import jetbrains.buildServer.serverSide.SFinishedBuild;
import jetbrains.buildServer.serverSide.SRunningBuild;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit.AntJUnitFactory;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @User Victory.Bedrosova
 * 12/16/13.
 */
@Test
@SpringContextFixture(beans = @SpringContextXmlBean(clazz = AntJUnitFactory.class))
public class XmlReportPluginIntegrationTest extends AgentServerFunctionalTestCase {

  public static final String RUN_TYPE = "reportPublisher";

  @NotNull
  private File myCheckoutDir;

  @NotNull
  private File myOuterDir;

  @BeforeClass
  @Override
  protected void setUpClass() {
    super.setUpClass();

  }

  @BeforeMethod
  @Override
  public void setUp1() throws Throwable {
    super.setUp1();
    myCheckoutDir = createTempDir();
    myOuterDir = createTempDir();
    new XmlReportPlugin(getExtensionHolder(),
                        getAgentEvents(),
                        getExtensionHolder().findSingletonService(InspectionReporter.class),
                        getExtensionHolder().findSingletonService(DuplicatesReporter.class),
                        getExtensionHolder().findSingletonService(BuildAgentConfiguration.class));
    registerRunner(new AgentBuildRunner() {
      @NotNull
      public BuildProcess createBuildProcess(@NotNull final AgentRunningBuild runningBuild, @NotNull final BuildRunnerContext context) {
        return new BuildProcess() {
          public void start() { }
          public boolean isInterrupted() { return false; }
          public boolean isFinished() { return false; }
          public void interrupt() { }

          @NotNull
          public BuildFinishedStatus waitFor() throws RunBuildException {
            try {
              final File reportFile = getCheckoutDirFile("report.xml");
              FileUtil.writeFile(reportFile,
                                 "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                 "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"1\" time=\"0.031\"\n" +
                                 "           timestamp=\"2008-10-30T17:11:25\">\n" +
                                 "  <properties/>\n" +
                                 "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.031\"/>\n" +
                                 "</testsuite>\n",
                                 "UTF-8"
              );
              // Make sure file is created after build start, as some
              // filesystems have 1 second last-modified resolution.
                assertTrue("Failed to update 'last-modified' attribute of report.xml", reportFile.setLastModified(reportFile.lastModified() + 1000));

              final File resultFile = getCheckoutDirFile("result.xml");
              FileUtil.writeFile(resultFile,
                                 "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                 "<testsuite errors=\"0\" failures=\"1\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"1\" time=\"0.047\"\n" +
                                 "           timestamp=\"2008-10-30T17:11:25\">\n" +
                                 "  <properties/>\n" +
                                 "  <testcase classname=\"TestCase\" name=\"test\" time=\"0.047\">\n" +
                                 "    <failure message=\"Assertion message from test\" type=\"junit.framework.AssertionFailedError\">\n" +
                                 "      junit.framework.AssertionFailedError: Assertion message from test\n" +
                                 "      at TestCase.test(Unknown Source)\n" +
                                 "    </failure>\n" +
                                 "  </testcase>\n" +
                                 "</testsuite>\n",
                                 "UTF-8"
              );
              // Make sure file is created after build start, as some
              // filesystems have 1 second last-modified resolution.
              assertTrue("Failed to update 'last-modified' attribute of result.xml", resultFile.setLastModified(resultFile.lastModified() + 1000));

              final File outerFile = getOuterDirFile("outer.xml");
              FileUtil.writeFile(outerFile,
                                 "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                                 "<testsuite errors=\"0\" failures=\"0\" hostname=\"ruspd-student3\" name=\"TestCase\" tests=\"1\" time=\"0.031\"\n" +
                                 "           timestamp=\"2008-10-30T17:11:25\">\n" +
                                 "  <properties/>\n" +
                                 "  <testcase classname=\"TestCase\" name=\"test\" executed=\"false\"/>\n" +
                                 "</testsuite>\n",
                                 "UTF-8"
              );
              // Make sure file is created after build start, as some
              // filesystems have 1 second last-modified resolution.
              assertTrue("Failed to update 'last-modified' attribute of outer.xml", outerFile.setLastModified(outerFile.lastModified() + 1000));

            } catch (IOException e) {
              throw new RunBuildException(e);
            }
            return BuildFinishedStatus.FINISHED_SUCCESS;
          }
        };
      }

      @NotNull
      public AgentBuildRunnerInfo getRunnerInfo() {
        return new AgentBuildRunnerInfo() {
          @NotNull
          public String getType() {
            return RUN_TYPE;
          }

          public boolean canRun(@NotNull final BuildAgentConfiguration agentConfiguration) {
            return true;
          }
        };
      }
    });
  }

  @NotNull
  private File getCheckoutDirFile(@NotNull String name) {
    return new File(myCheckoutDir, name);
  }

  @NotNull
  private File getOuterDirFile(@NotNull String name) {
    return new File(myOuterDir, name);
  }

  @Test
  public void testUnexisting() throws Exception {
    doTest("abrakadabra.xml", 0, "No reports found");
  }

  @Test
  public void testUnexistingAbsolute() throws Exception {
    doTest("##C_D##/abrakadabra.xml", 0, "No reports found");
  }

  @Test
  public void testUnexistingMask() throws Exception {
    doTest("abrakadabra*.xml", 0, "No reports found");
  }

  @Test
  public void testUnexistingAbsoluteMask() throws Exception {
    doTest("##C_D##/abrakadabra*.xml", 0, "No reports found");
  }

  @Test
  public void testSingleFile() throws Exception {
    doTest("report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleMaskFile() throws Exception {
    doTest("rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleAbsoluteFile() throws Exception {
    doTest("##C_D##/report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterFile() throws Exception {
    doTest("##O_D##/outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeFile() throws Exception {
    doTest("##C_D##/fold/../report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeFile() throws Exception {
    doTest("##O_D##/fold/../outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeFileDot() throws Exception {
    doTest("##C_D##/./report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeFileDot() throws Exception {
    doTest("##O_D##/./outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeMaskFile() throws Exception {
    doTest("##C_D##/fold/../rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeMaskFile() throws Exception {
    doTest("##O_D##/fold/../out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeMaskFileDot() throws Exception {
    doTest("##C_D##/./rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeMaskFileDot() throws Exception {
    doTest("##O_D##/./out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleAbsoluteMaskFile() throws Exception {
    doTest("##C_D##/rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterMaskFile() throws Exception {
    doTest("##O_D##/out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRule() throws Exception {
    doTest("+:report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleMaskRule() throws Exception {
    doTest("+:rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleAbsoluteRule() throws Exception {
    doTest("+:##C_D##/report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRule() throws Exception {
    doTest("+:##O_D##/outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeRule() throws Exception {
    doTest("+:##C_D##/fold/../report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeRule() throws Exception {
    doTest("+:##O_D##/fold/../outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeRuleDor() throws Exception {
    doTest("+:##C_D##/./report.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeRuleDot() throws Exception {
    doTest("+:##O_D##/./outer.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeMaskRule() throws Exception {
    doTest("+:##C_D##/fold/../rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeMaskRule() throws Exception {
    doTest("+:##O_D##/fold/../out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleRelativeMaskRuleDot() throws Exception {
    doTest("+:##C_D##/./rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterRelativeMaskRuleDot() throws Exception {
    doTest("+:##O_D##/./out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleAbsoluteMaskRule() throws Exception {
    doTest("+:##C_D##/rep*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testSingleOuterMaskRule() throws Exception {
    doTest("+:##O_D##/out*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoFiles() throws Exception {
    doTest("report.xml\nresult.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoMaskFiles() throws Exception {
    doTest("rep*.xml\nres*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoAbsoluteFiles() throws Exception {
    doTest("##C_D##/report.xml\n##C_D##/result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeFiles() throws Exception {
    doTest("##C_D##/fold/../report.xml\n##C_D##/fold/../result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeMaskFiles() throws Exception {
    doTest("##C_D##/fold/../rep*.xml\n##C_D##/fold/../res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeFilesDot() throws Exception {
    doTest("##C_D##/./report.xml\n##C_D##/./result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeMaskFilesDot() throws Exception {
    doTest("##C_D##/./rep*.xml\n##C_D##/./res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoAbsoluteMaskFiles() throws Exception {
    doTest("##C_D##/rep*.xml\n##C_D##/res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRules() throws Exception {
    doTest("+:report.xml\n+:result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoMaskRules() throws Exception {
    doTest("+:rep*.xml\n+:res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoAbsoluteRules() throws Exception {
    doTest("+:##C_D##/report.xml\n+:##C_D##/result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeRules() throws Exception {
    doTest("+:##C_D##/fold/../report.xml\n+:##C_D##/fold/../result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeMaskRules() throws Exception {
    doTest("+:##C_D##/fold/../rep*.xml\n+:##C_D##/fold/../res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeRulesDot() throws Exception {
    doTest("+:##C_D##/./report.xml\n+:##C_D##/./result.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoRelativeMaskRulesDot() throws Exception {
    doTest("+:##C_D##/./rep*.xml\n+:##C_D##/./res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoAbsoluteMaskRules() throws Exception {
    doTest("+:##C_D##/rep*.xml\n+:##C_D##/res*.xml", 2, "2 reports found for paths");
  }

  @Test
  public void testTwoDifferentRules() throws Exception {
    doTest("+:report.xml\n-:result.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentMaskRules() throws Exception {
    doTest("+:rep*.xml\n-:res*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentAbsoluteRules() throws Exception {
    doTest("+:##C_D##/report.xml\n-:##C_D##/result.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentRelativeRules() throws Exception {
    doTest("+:##C_D##/fold/../report.xml\n-:##C_D##/fold/../result.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentRelativeMaskRules() throws Exception {
    doTest("+:##C_D##/fold/../rep*.xml\n-:##C_D##/fold/../res*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentRelativeRulesDot() throws Exception {
    doTest("+:##C_D##/./report.xml\n-:##C_D##/./result.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentRelativeMaskRulesDot() throws Exception {
    doTest("+:##C_D##/./rep*.xml\n-:##C_D##/./res*.xml", 1, "1 report found for paths");
  }

  @Test
  public void testTwoDifferentAbsoluteMaskRules() throws Exception {
    doTest("+:##C_D##/rep*.xml\n-:##C_D##/res*.xml", 1, "1 report found for paths");
  }

  // TW-37280
  @Test
  public void testOverlappingBuildFinishedBuildStartedEvents() throws Exception {
    final EventDispatcher<AgentLifeCycleListener> agentEvents = getAgentEvents();
    final XmlReportPlugin reportPlugin = new XmlReportPlugin(getExtensionHolder(),
                                                             agentEvents,
                                                             getExtensionHolder().findSingletonService(InspectionReporter.class),
                                                             getExtensionHolder().findSingletonService(DuplicatesReporter.class),
                                                             getExtensionHolder().findSingletonService(BuildAgentConfiguration.class));
    final BuildTypeEx bt = createBuildType(RUN_TYPE);
    startBuild(bt, true);

    final AgentRunningBuildEx build = getAgentRunningBuild();

    reportPlugin.buildStarted(build);
    reportPlugin.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);

    reportPlugin.buildStarted(build);
    reportPlugin.buildFinished(build,  BuildFinishedStatus.FINISHED_SUCCESS);

    reportPlugin.beforeBuildFinish(build, BuildFinishedStatus.FINISHED_SUCCESS);
    reportPlugin.buildFinished(build,  BuildFinishedStatus.FINISHED_SUCCESS);
  }

  private void doTest(@NotNull String reportDirs, int numberOfTests, String... messages) throws Exception {

    final BuildTypeEx bt = createBuildType(RUN_TYPE);
    bt.setCheckoutDirectory(myCheckoutDir.getAbsolutePath());

    final Map<String, String> fps = new HashMap<String, String>();
    fps.put(XmlReportPluginConstants.REPORT_TYPE, "junit");
    fps.put(XmlReportPluginConstants.REPORT_DIRS, reportDirs.replace("##C_D##", myCheckoutDir.getAbsolutePath()).replace("##O_D##", myOuterDir.getAbsolutePath()));

    bt.addBuildFeature(XmlReportPluginBuildFeature.FEATURE_TYPE, fps);

    final SRunningBuild sb = startBuild(bt, true);
    final SFinishedBuild fb = finishBuild(sb);

    assertEquals(numberOfTests, fb.getBuildStatistics(BuildStatisticsOptions.ALL_TESTS_NO_DETAILS).getAllTestRunCount());

    final String buildLog = getBuildLog(fb);
    for (String m : messages) {
      assertTrue(buildLog.contains(m));
    }
  }
}
