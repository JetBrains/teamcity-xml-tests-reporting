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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.xmlReportPlugin.ReportContext;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;

import javax.xml.transform.TransformerConfigurationException;
import java.io.File;


public class NUnitReportParser extends AntJUnitReportParser {
  public static final String TYPE = "nunit";
  private static final String TMP_REPORT_DIRECTORY = File.separator + "junit_reports";

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
  public void parse(@NotNull final ReportContext context) throws Exception {
    final File report = context.getFile();
    final File junitReport = new File(myTmpReportDir.getPath() + File.separator + report.getName());
    try {
      myReportTransformer.transform(report, junitReport);
    } catch (Exception e) {
      XmlReportPlugin.LOG.debug("xslt transformation failed for " + report.getAbsolutePath(), e);
      junitReport.delete();
      context.setProcessedEvents(0);
      return;
    }
    final ReportContext jUnitContext = new ReportContext(junitReport, "nunit", context.getPathParameters());
    super.parse(jUnitContext);
    context.setProcessedEvents(jUnitContext.getProcessedEvents());
  }

  @NotNull
  @Override
  protected String getRootTag() {
    return "test-results";
  }
}
