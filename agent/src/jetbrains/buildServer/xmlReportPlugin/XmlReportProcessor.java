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
  protected void doFinalStep() throws Exception {
    myWatcher.join();

    while (!getQueue().isEmpty()) {
      processReport(takeNextReport(true));
    }

    logParsingTotals();
    logFailedToProcess();
  }

  @Override
  protected long getPeriod() {
    return 100L;
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
        parser.logParsingTotals(getParameters());
      }
    });
  }

  private void processReport(@Nullable final ReportContext context) throws Exception {
    if (context == null) return;

    final XmlReportParser parser = myParsers.getParser(context.getType());
    final String typeName = XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(context.getType());

    try {
      LOG.debug("Parsing " + context.getFile().getAbsolutePath() + " with " + context.getType() + " parser.");
      parser.parse(context);
    } catch (SAXParseException e) {
      getThreadLogger().error(context.getFile().getAbsolutePath() + " is not parsable with " + typeName + " parser");
      if (getParameters().isVerbose()) getThreadLogger().exception(e);
      return;
    } catch (Exception e) {
      getThreadLogger().error("Exception occurred while parsing " + context.getFile().getAbsolutePath());
      if (getParameters().isVerbose()) getThreadLogger().exception(e);
      return;
    }

    if (context.getProcessedEvents() != -1) {
      if (isStopSignaled()) {
        final String message = "Failed to parse " + context.getFile().getAbsolutePath() + " with " + typeName + " parser";
        LOG.error(message);
        getThreadLogger().error(message);
        myFailedReportTypes.add(typeName);
      } else {
        getQueue().put(context);
      }
    } else {
      parser.logReportTotals(context, getParameters().isVerbose());
    }
  }

  @Nullable
  private ReportContext takeNextReport(boolean finalParsing) throws Exception {
    final ReportContext context = getQueue().poll(!finalParsing);
    if (context != null) {
      final long len = context.getFile().length();
      try {
        if (len > context.getFileLength() || finalParsing) {
          if (myParsers.getParser(context.getType()).supportOnTheFlyParsing() || finalParsing || (!reportGrows(context) && isReportComplete(context))) {
            return context;
          }
        }
        getQueue().put(context);
        return null;
      } finally {
        context.setFileLength(len);
      }
    }
    return null;
  }

  private boolean reportGrows(@NotNull ReportContext context) throws InterruptedException {
    if (!getParameters().checkReportGrows())
      return false;
    final long oldLength = context.getFile().length();
    for (int i = 0; i < 10; ++i) {
      Thread.sleep(10);
      final long newLength = context.getFile().length();
      if (newLength > oldLength) {
        return true;
      }
    }
    return false;
  }

  private boolean isReportComplete(@NotNull ReportContext context) {
    return !getParameters().checkReportComplete() || myParsers.getParser(context.getType()).isReportComplete(context.getFile());
  }
}
