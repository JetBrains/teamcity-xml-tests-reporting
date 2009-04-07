/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.xmlReportPlugin.ReportData;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;
import java.io.IOException;


public class NUnitReportParser extends AntJUnitReportParser {
  public static final String TYPE = "nunit";
  private static final String TMP_REPORT_DIRECTORY = "/junit_reports";

  private NUnitToJUnitReportTransformer myReportTransformer;
  private File myTmpReportDir;

  public NUnitReportParser(BaseServerLoggerFacade logger, String tmpDir) {
    super(logger);
    try {
      myReportTransformer = new NUnitToJUnitReportTransformer();
    } catch (TransformerConfigurationException e) {
      myLogger.warning("NUnit report parser couldn't instantiate transformer");
    }
    myTmpReportDir = new File(tmpDir + TMP_REPORT_DIRECTORY);
    myTmpReportDir.mkdirs();
  }

  public void parse(@NotNull final ReportData data) {
    final File report = data.getFile();
    final File junitReport = new File(myTmpReportDir.getPath() + "/" + report.getName());
    try {
      myReportTransformer.transform(report, junitReport);
    } catch (IOException ioe) {
      myLogger.exception(ioe);
      data.setProcessedEvents(-1);
      return;
    } catch (Exception e) {
      myLogger.exception(e);
      junitReport.delete();
      data.setProcessedEvents(-1);
      return;
    }
    final ReportData jUnitData = new ReportData(junitReport, "nunit");
    super.parse(jUnitData);
    data.setProcessedEvents(jUnitData.getProcessedEvents());
    //jUnitData.getFile().delete();
  }
}
