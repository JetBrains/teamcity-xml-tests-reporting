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
  private final FileStateHolder myFileStateHolder;

  @NotNull
  private final Map<File, ParsingResult> myPrevResults;

  @NotNull
  private final ParserFactory myParserFactory;

  public ParseReportCommand(@NotNull File file,
                            @NotNull ParseParameters parameters,
                            @NotNull FileStateHolder fileStateHolder,
                            @NotNull Map<File, ParsingResult> prevResults,
                            @NotNull ParserFactory parserFactory) {
    myFile = file;
    myParameters = parameters;
    myFileStateHolder = fileStateHolder;
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
      myParserFactory.createResultsProcessor().processResult(myFile, parsingResult, myParameters);
      myPrevResults.remove(myFile);
      myFileStateHolder.setFileProcessed(myFile, parsingResult);
    } else {
      //todo: log file not processed
      myPrevResults.put(myFile, parsingResult);
      myFileStateHolder.removeFile(myFile);
    }
  }

  private void logFailedToParse(@NotNull ParsingException e) {
    LoggingUtils.logFailedToParse(myFile, myParameters.getType(), e.getCause(), myParameters.getThreadLogger());
  }
}