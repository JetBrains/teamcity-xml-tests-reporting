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

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 16.12.10
 * Time: 18:06
 */
public class ParseReportCommand implements Runnable {
  @NotNull
  private final File myFile;

  @NotNull
  private final ParseParameters myParameters;

  @NotNull
  private final FileStates myFileStates;

  @NotNull
  private final Map<File, ParsingResult> myPrevResults;

  @NotNull
  private final ParserFactory myParserFactory;

  public ParseReportCommand(@NotNull File file,
                            @NotNull ParseParameters parameters,
                            @NotNull FileStates fileStates,
                            @NotNull Map<File, ParsingResult> prevResults,
                            @NotNull ParserFactory parserFactory) {
    myFile = file;
    myParameters = parameters;
    myFileStates = fileStates;
    myPrevResults = prevResults;
    myParserFactory = parserFactory;
  }

  public void run() {
    final Parser parser = myParserFactory.createParser(myParameters);

    boolean finished;
    try {
      finished = parser.parse(myFile, myPrevResults.get(myFile));
    } catch (ParsingException e) {
      finished = true;
      logFailedToParse(e);
    }

    final ParsingResult parsingResult = parser.getParsingResult();
    assert parsingResult != null;

    if (finished) { // file processed
      parsingResult.logAsFileResult(myFile, myParameters);
      myPrevResults.remove(myFile);
      myFileStates.setFileProcessed(myFile, parsingResult);
    } else {
      //todo: log file not processed
      myPrevResults.put(myFile, parsingResult);
      myFileStates.removeFile(myFile);
    }
  }

  private void logFailedToParse(@NotNull ParsingException e) {
    LoggingUtils.logError("Failed to parse " + myFile + " with " + LoggingUtils.getTypeDisplayName(myParameters.getType())
      + " parser", e.getCause(), myParameters.getThreadLogger());
  }
}