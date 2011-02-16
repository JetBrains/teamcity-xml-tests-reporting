/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.XMLReader;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 18:31
 */
public class ParseReportCommandTest extends BaseCommandTestCase {
  private File myFile;
  private RulesState myRulesState;
  private StringBuilder myResult;
  private Map<File, ParsingResult> myPrevResults;
  private BuildProgressLogger myLogger;
  private ParseParameters myParseParameters;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFile = writeFile("file.xml", false);
    myRulesState = new RulesState();
    myResult = new StringBuilder();
    myPrevResults = new HashMap<File, ParsingResult>();
    myLogger = new BuildLoggerForTesting(myResult);
    myParseParameters = createParseParameters();
  }

  @NotNull
  private ParseReportCommand createParseReportCommand(@NotNull Parser parser) {
    return new ParseReportCommand(myFile, myParseParameters, myRulesState, myPrevResults, createParserFactory(parser));
  }

  @NotNull
  private Parser createParser(final boolean succeed, final boolean throwException) {
    return new Parser() {
      public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
        myResult.append("PARSING: ").append(file).append(" PREVIOUS RESULT: ").append(prevResult).append("\n");
        if (throwException) throw new ParsingException(null);
        return succeed;
      }

      public ParsingResult getParsingResult() {
        return new ParsingResult() {
          public void accumulate(@NotNull ParsingResult parsingResult) {
          }

          public void logAsFileResult(@NotNull File file, @NotNull ParseParameters parameters) {
            myResult.append("PROCESSING RESULT: FILE: ").append(file).append(" RESULT: ").append("EMPTY_RESULT").append("\n");
          }

          public void logAsTotalResult(@NotNull ParseParameters parameters) {
            throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
          }
        };
      }
    };
  }

  private void assertFileState(@NotNull FileStates.FileState state) {
    assertTrue("Wrong file state", myRulesState.getFileState(myFile) == state);
  }

  private void assertNotInPrevState() {
    assertFalse("", myPrevResults.containsKey(myFile));
  }

  private void assertInPrevState() {
    assertTrue("", myPrevResults.containsKey(myFile));
  }

  @Test
  public void testParsedSuccess() throws Exception {
    myRulesState.addFile(myFile);

    final Parser parser = createParser(true, false);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: null",
      "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertNotInPrevState();
    assertFileState(FileStates.FileState.PROCESSED);
  }

  @Test
  public void testParsedWithFailure() throws Exception {
    myRulesState.addFile(myFile);

    final Parser parser = createParser(false, false);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: null");
    assertNotContains(myResult, "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertInPrevState();
    assertFileState(FileStates.FileState.UNKNOWN);
  }

  @Test
  public void testParsedWithException() throws Exception {
    myRulesState.addFile(myFile);

    final Parser parser = createParser(true, true);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: null",
      "ERROR: Failed to parse ##BASE_DIR##/file.xml with null parser",
      "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertNotInPrevState();
    assertFileState(FileStates.FileState.PROCESSED);
  }

  @Test
  public void testReparsedSuccess() throws Exception {
    myRulesState.addFile(myFile);
    myPrevResults.put(myFile, EMPTY_RESULT);

    final Parser parser = createParser(true, false);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: EMPTY_RESULT",
      "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertNotInPrevState();
    assertFileState(FileStates.FileState.PROCESSED);
  }

  @Test
  public void testReparsedWithFailure() throws Exception {
    myRulesState.addFile(myFile);
    myPrevResults.put(myFile, EMPTY_RESULT);

    final Parser parser = createParser(false, false);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: EMPTY_RESULT");
    assertNotContains(myResult, "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertInPrevState();
    assertFileState(FileStates.FileState.UNKNOWN);
  }

  @Test
  public void testReparsedWithException() throws Exception {
    myRulesState.addFile(myFile);
    myPrevResults.put(myFile, EMPTY_RESULT);

    final Parser parser = createParser(false, true);
    final ParseReportCommand parseReportCommand = createParseReportCommand(parser);
    parseReportCommand.run();

    assertContains(myResult, "PARSING: ##BASE_DIR##/file.xml PREVIOUS RESULT: EMPTY_RESULT",
      "ERROR: Failed to parse ##BASE_DIR##/file.xml with null parser",
      "PROCESSING RESULT: FILE: ##BASE_DIR##/file.xml RESULT: EMPTY_RESULT");

    assertNotInPrevState();
    assertFileState(FileStates.FileState.PROCESSED);
  }

  @NotNull
  private ParseParameters createParseParameters() {
    return new
      ParseParameters() {
        public boolean isVerbose() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public BuildProgressLogger getThreadLogger() {
          return myLogger;
        }

        @NotNull
        public BuildProgressLogger getInternalizingThreadLogger() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public InspectionReporter getInspectionReporter() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public DuplicatesReporter getDuplicatesReporter() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public Map<String, String> getParameters() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public XMLReader getXmlReader() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public String getType() {
          return "TYPE";
        }

        @NotNull
        public File getCheckoutDir() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }

        @NotNull
        public File getTempDir() {
          throw new IllegalStateException(UNEXPECTED_CALL_MESSAGE);
        }
      };
  }

  @NotNull
  private static String UNEXPECTED_CALL_MESSAGE = "Unexpected method call";

  @NotNull
  private static ParserFactory createParserFactory(@NotNull final Parser parser) {
    return new ParserFactory() {
      @NotNull
      public Parser createParser(@NotNull ParseParameters parameters) {
        return parser;
      }

      @NotNull
      public ParsingResult createEmptyResult() {
        return EMPTY_RESULT;
      }
    };
  }
}
