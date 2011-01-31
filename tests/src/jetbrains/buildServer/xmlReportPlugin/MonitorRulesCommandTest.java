package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 14:43
 */
public class MonitorRulesCommandTest extends BaseCommandTestCase {
  @NotNull
  private static final String TYPE = "TYPE";
  private static final String FILE_DETECTED_MESSAGE = "DETECTED: ##BASE_DIR##/folder/file.xml";

  private RulesFileStateHolder myRulesFilesState;
  private StringBuilder myResult;
  private File myFile;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFile = writeFile("folder/file.xml");
    myRulesFilesState = new RulesFileStateHolder();
    myResult = new StringBuilder();
    Thread.sleep(10L);
  }

  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand() {
    return createMonitorRulesCommand(false, myTestStartTime);
  }

  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand(boolean parseOutOfDate, long startTime) {
    return createMonitorRulesCommand(myRulesFilesState, myResult, parseOutOfDate, startTime);
  }
    
  @NotNull
  private MonitorRulesCommand createMonitorRulesCommand(@NotNull FileStateHolder fileStateHolder,
                                                        @NotNull final StringBuilder result,
                                                        final boolean parseOutOfDate, final long startTime) {
    final List<String> rulesList = Arrays.asList("*.xml", "**/*.xml");
    final Rules rules = new Rules(rulesList, myBaseFolder);
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
    };

    final MonitorRulesCommand.MonitorRulesListener listener = new MonitorRulesCommand.MonitorRulesListener() {
      public void modificationDetected(@NotNull File file) {
        result.append("DETECTED: ").append(file);
      }
    };

    return new MonitorRulesCommand(parameters, fileStateHolder, listener);
  }

  private void assertFileState(@NotNull FileStateHolder.FileState state) {
    assertTrue("Wrong file state", myRulesFilesState.getFileState(myFile) == state);
  }

  private void assertFileDetected() {
    assertTrue("File doesn't exist", myFile.isFile());
    assertContains(myResult, FILE_DETECTED_MESSAGE);
  }

  private void assertFileNotDetected() {
    assertNotContains(myResult, FILE_DETECTED_MESSAGE);
  }

  @Test
  public void testWatchingPaths() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertContains(myResult, "MESSAGE: Watching paths:", "MESSAGE: *.xml", "MESSAGE: **/*.xml");

    myResult.delete(0, myResult.length());
    command.run();

    assertNotContains(myResult, "MESSAGE: Watching paths:", "MESSAGE: *.xml", "MESSAGE: **/*.xml");
  }

  @Test
  public void testFileDetected() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenOutOfDate() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand(false, new Date().getTime());
    command.run();

    assertFileNotDetected();
    assertFileState(FileStateHolder.FileState.UNKNOWN);
  }

  @Test
  public void testFileDetectedWhenOutOfDate() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand(true, new Date().getTime());
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenSentToParsing() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);

    myResult.delete(0, myResult.length());
    command.run();

    assertFileNotDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenProcessed() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);

    myRulesFilesState.setFileProcessed(myFile, EMPTY_RESULT);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileNotDetected();
    assertFileState(FileStateHolder.FileState.PROCESSED);
  }

  @Test
  public void testFileDetectedWhenChanged() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);

    myRulesFilesState.removeFile(myFile);
    writeFile(myFile);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);
  }

  @Test
  public void testFileNotDetectedWhenUnchanged() throws Exception {
    final MonitorRulesCommand command = createMonitorRulesCommand();
    command.run();

    assertFileDetected();
    assertFileState(FileStateHolder.FileState.ON_PROCESSING);

    myRulesFilesState.removeFile(myFile);
    myResult.delete(0, myResult.length());
    command.run();

    assertFileNotDetected();
    assertFileState(FileStateHolder.FileState.UNKNOWN);
  }
}
