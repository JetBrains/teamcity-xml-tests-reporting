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

package jetbrains.buildServer.xmlReportPlugin.nUnit;

import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerConfigurationException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.ImportRequestContext;
import jetbrains.buildServer.xmlReportPlugin.ReportFileContext;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;


public class NUnitReportParser extends AntJUnitReportParser {
  public static final String TYPE = "nunit";
  private static final String TMP_REPORT_DIRECTORY = File.separator + "junit_reports";
  private static final String TRAILING_TAG = "</test-results>";

  private NUnitToJUnitReportTransformer myReportTransformer;
  private final File myTmpReportDir;

  public NUnitReportParser(BuildProgressLogger logger, String tmpDir, String schema) {
    try {
      myReportTransformer = new NUnitToJUnitReportTransformer(schema);
    } catch (TransformerConfigurationException e) {
      logger.warning("NUnit report parser couldn't instantiate transformer");
    }
    myTmpReportDir = new File(tmpDir + TMP_REPORT_DIRECTORY);
    myTmpReportDir.mkdirs();
  }

  @Override
  public void parse(@NotNull final ReportFileContext data) {
    final File report = data.getFile();
    if (!isReportComplete(data, TRAILING_TAG)) {
      XmlReportPlugin.LOG.debug("The report doesn't finish with " + TRAILING_TAG);
      data.setProcessedEvents(0);
      return;
    }
    final File junitReport = new File(myTmpReportDir.getPath() + File.separator + report.getName());
    try {
      myReportTransformer.transform(report, junitReport);
    } catch (IOException ioe) {
      data.getRequestContext().getLogger().exception(ioe);
      data.setProcessedEvents(-1);
      return;
    } catch (Exception e) {
      data.getRequestContext().getLogger().exception(e);
      junitReport.delete();
      data.setProcessedEvents(-1);
      return;
    }
    final MyJUnitReportFileContext newContext = new MyJUnitReportFileContext(data, junitReport);
    super.parse(newContext);
    data.setProcessedEvents(newContext.getProcessedEvents());
    //jUnitData.getFile().delete();
  }

  private static class MyJUnitReportFileContext implements ReportFileContext {
    private int myProcessedEvents;
    private final ReportFileContext myData;
    private final File myJunitReport;

    public MyJUnitReportFileContext(final ReportFileContext data, final File junitReport) {
      myData = data;
      myJunitReport = junitReport;
      myProcessedEvents = 0;
    }

    @NotNull
    public ImportRequestContext getRequestContext() {
      return myData.getRequestContext();
    }

    @NotNull
    public File getFile() {
      return myJunitReport;
    }

    public int getProcessedEvents() {
      return myProcessedEvents;
    }

    public void setProcessedEvents(final int tests) {
      myProcessedEvents = tests;
    }

    public long getFileLength() {
      return 0;
    }

    @NotNull
    public String getType() {
      return "nunit";
    }
  }
}
