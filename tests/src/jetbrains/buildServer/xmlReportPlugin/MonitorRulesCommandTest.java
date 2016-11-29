/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 14:43
 */

@Test
public class MonitorRulesCommandTest extends BaseCommandTestCase {
  @NotNull
  private static final String TYPE = "TYPE";
  private static final String FILE_DETECTED_MESSAGE = "DETECTED: ##BASE_DIR##/folder/file.xml";

  private RulesState myRulesState;
  private StringBuilder myResult;
  private File myFile;

  @BeforeMethod
  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFile = writeFile("folder/file.xml", true);
    myRulesState = new RulesState();
    myResult = new StringBuilder();
  }

  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand() {
    return createMonitorRulesCommand(false, myTestStartTime);
  }

  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand(boolean parseOutOfDate, long startTime) {
    return createMonitorRulesCommand(myRulesState, myResult, parseOutOfDate, startTime);
  }
    
  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand(@NotNull ReportStateHolder reportStateHolder,
                                                        @NotNull final StringBuilder result,
                                                        final boolean parseOutOfDate, final long startTime) {
    final List<String> rulesList = Arrays.asList("**/*.xml");
    final Rules rules = new OptimizingIncludeExcludeRules(myBaseFolder, rulesList);
    final MonitorRulesCommand.MonitorRulesParameters parameters = new MonitorRulesCommand.MonitorRulesParameters() {
      @NotNull
      public Rules getRules() {
        return rules;
      }

      @NotNull
      public String getType() {
        return TYPE;
      }

      public boolean isParseOutOfDate() {
        return parseOutOfDate;
      }

      public long getStartTime() {
        return startTime;
      }

      @NotNull
      public BuildProgressLogger getThreadLogger() {
        return new BuildLoggerForTesting(result);
      }

      @Override
      public boolean isReparseUpdated() {
        return true;
      }
    };

    final MonitorRulesCommand.MonitorRulesListener listener = new MonitorRulesCommand.MonitorRulesListener() {
      public void modificationDetected(@NotNull File file) {
        result.append("DETECTED: ").append(file);
      }
    };

    return new MonitorRulesCommand(parameters, reportStateHolder, listener);
  }

  private void assertFileState(@NotNull ReportStateHolder.ReportState state) {
    assertTrue(myRulesState.getReportState(myFile) == state);
  }

  private void assertFileDetected() {
    assertTrue(myFile.isFile());
    assertContains(myResult, FILE_DETECTED_MESSAGE);
  }

  private void assertFileNotDetected() {
    assertNotContains(myResult, FILE_DETECTED_MESSAGE);
  }

  @Test
  public void testWatchingPaths() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertContains(myResult, "MESSAGE: Watching paths:", "MESSAGE: **/*.xml");

    myResult.delete(0, myResult.length());
    command.run();

    assertNotContains(myResult, "MESSAGE: Watching paths:", "##BASE_DIR##/MESSAGE: *.xml", "##BASE_DIR##/MESSAGE: **/*.xml");
  }

  @Test
  public void testFileDetected() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenOutOfDate() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand(false, new Date().getTime());
    command.run();

    assertFileNotDetected();
    assertFileState(ReportStateHolder.ReportState.OUT_OF_DATE);
  }

  // TW-21402
  @Test
  public void testFileDetectedWhenLastModifiedInSeconds() throws Exception {
      final long modificationTime = new Date().getTime() / 1000 * 1000;
      final MonitorRulesCommand command = createMonitorRulesCommand(false, modificationTime);
      myFile.setLastModified(modificationTime);
      command.run();

      assertFileDetected();
      assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileDetectedWhenOutOfDate() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand(true, new Date().getTime());
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenSentToParsing() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);

    myResult.delete(0, myResult.length());
    command.run();

    assertFileNotDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileDetectedWhenChangedProcessed() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);

    myRulesState.setReportState(myFile, ReportStateHolder.ReportState.PROCESSED, EMPTY_RESULT);
    writeFile(myFile, true);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileDetectedWhenChanged() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);

    myRulesState.setReportState(myFile, ReportStateHolder.ReportState.ERROR, EMPTY_RESULT);
    writeFile(myFile, true);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenUnchanged() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(ReportStateHolder.ReportState.ON_PROCESSING);

    myRulesState.setReportState(myFile, ReportStateHolder.ReportState.ERROR, EMPTY_RESULT);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileNotDetected();
    assertFileState(ReportStateHolder.ReportState.ERROR);
  }
}
