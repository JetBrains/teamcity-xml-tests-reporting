package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;

public class DummyReportFileContext implements ReportFileContext {
  private final ImportRequestContext myRequestContext;
  private final File myFile;
  private int myProcessedEvents;
  private long myFileLength;
  private final String myType;

  public DummyReportFileContext(@NotNull final File file, @NotNull final String type, @NotNull final BuildProgressLogger logger) {
    myFile = file;
    myType = type;
    myRequestContext = new DummyImportRequestContext(file, logger);
    myProcessedEvents = 0;
  }

  @NotNull
  public ImportRequestContext getRequestContext() {
    return myRequestContext;
  }

  @NotNull
  public File getFile() {
    return myFile;
  }

  public int getProcessedEvents() {
    return myProcessedEvents;
  }

  public void setProcessedEvents(final int tests) {
    myProcessedEvents = tests;
  }

  public long getFileLength() {
    return myFileLength;
  }

  public void setFileLength(final long fileLength) {
    myFileLength = fileLength;
  }

  @NotNull
  public String getType() {
    return myType;
  }
}
