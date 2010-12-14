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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXParseException;

import java.util.HashSet;
import java.util.Set;

public class XmlReportProcessor extends XmlReportPluginActivity {
  public static final Logger LOG = Logger.getLogger(XmlReportProcessor.class);
  
  @NotNull
  private final XmlReportDirectoryWatcher myWatcher;
  @NotNull
  private final Parsers myParsers;

  private final Set<String> myFailedReportTypes;

  public XmlReportProcessor(@NotNull final XmlReportPluginParameters parameters,
                            @NotNull final ReportQueue queue,
                            @NotNull final XmlReportDirectoryWatcher watcher) {
    super("xml-report-plugin-ReportProcessor", parameters, queue);
    myWatcher = watcher;
    myParsers = new Parsers(parameters);
    myFailedReportTypes = new HashSet<String>();
  }

  @Override
  protected void doStep() throws Exception {
    processReport(takeNextReport(false));
  }

  @Override
  protected void doPostStep() throws Exception {
    myWatcher.join();

    while (!getQueue().isEmpty()) {
      processReport(takeNextReport(true));
    }

    logParsingTotals();
    logFailedToProcess();
  }

  @Override
  protected long getPeriod() {
    return 500L;
  }

  @NotNull
  @Override
  protected Logger getLogger() {
    return LOG;
  }

  private void logFailedToProcess() {
    if (!myFailedReportTypes.isEmpty()) {
      final StringBuffer types = new StringBuffer();
      for (final String t : myFailedReportTypes) {
        types.append(t).append(", ");
      }
      getThreadLogger().error("Failed to process some " + types.substring(0, types.length() - 2) + " reports");
    }
  }

  private void logParsingTotals() {
    myParsers.doWithParsers(new Parsers.Processor() {
      public void process(@NotNull XmlReportParser parser) {
        parser.logParsingTotals(new SessionContext() {
          @NotNull
          public BuildProgressLogger getLogger() {
            return getThreadLogger();
          }
        }, getParameters().getRunnerParameters(), getParameters().isVerbose());
      }
    });
  }

  private void processReport(@Nullable final ReportData data) {
    if (data == null) return;

    final boolean logAsInternal = getParameters().getPathParameters(data.getImportRequestPath()).isLogAsInternal();

    final BuildProgressLogger requestLogger =
      logAsInternal ?
        ((MessageTweakingSupport)getThreadLogger()).getTweakedLogger(MessageInternalizer.MESSAGE_INTERNALIZER) :
        getThreadLogger();

    // TODO it's not efficient to create a context object each time a file is processed. Needs refactoring

    ImportRequestContextImpl requestContext = new ImportRequestContextImpl(data.getImportRequestPath(), requestLogger);
    ReportFileContextImpl reportContext = new ReportFileContextImpl(data, requestContext);

    final XmlReportParser parser = myParsers.getParser(data.getType());
    final String typeName = XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(data.getType());

    try {
      LOG.debug("Parsing " + data.getFile().getAbsolutePath() + " with " + data.getType() + " parser.");
      parser.parse(reportContext);
    } catch (SAXParseException e) {
      getThreadLogger().error(data.getFile().getAbsolutePath() + " is not parsable with " + typeName + " parser");
      if (getParameters().isVerbose()) requestLogger.exception(e);
      return;
    } catch (Exception e) {
      getThreadLogger().error("Exception occurred while parsing " + data.getFile().getAbsolutePath());
      if (getParameters().isVerbose()) requestLogger.exception(e);
      return;
    }

    if (data.getProcessedEvents() != -1) {
      if (isStopSignaled()) {
        final String message = "Failed to parse " + data.getFile().getAbsolutePath() + " with " + typeName + " parser";
        LOG.error(message);
        getThreadLogger().error(message);
        myFailedReportTypes.add(typeName);
      } else {
        getQueue().put(data);
      }
    } else {
      parser.logReportTotals(reportContext, getParameters().isVerbose());
    }
  }

  @Nullable
  private ReportData takeNextReport(boolean finalParsing) {
    try {
      final ReportData data = getQueue().poll(!finalParsing);
      if (data != null) {
        final long len = data.getFile().length();
        try {
          if (len > data.getFileLength() || finalParsing) {
            if (myParsers.getParser(data.getType()).supportOnTheFlyParsing() || finalParsing || (!reportGrows(data) && isReportComplete(data))) {
              return data;
            }
          }
          getQueue().put(data);
          return null;
        } finally {
          data.setFileLength(len);
        }
      }
    } catch (InterruptedException e) {
      getThreadLogger().exception(e);
    }
    return null;
  }

  private boolean reportGrows(@NotNull ReportData data) throws InterruptedException {
    if (!getParameters().checkReportGrows())
      return false;
    final long oldLength = data.getFile().length();
    for (int i = 0; i < 10; ++i) {
      Thread.sleep(10);
      final long newLength = data.getFile().length();
      if (newLength > oldLength) {
        return true;
      }
    }
    return false;
  }

  private boolean isReportComplete(@NotNull ReportData data) {
    return !getParameters().checkReportComplete() || myParsers.getParser(data.getType()).isReportComplete(data.getFile());
  }
}
