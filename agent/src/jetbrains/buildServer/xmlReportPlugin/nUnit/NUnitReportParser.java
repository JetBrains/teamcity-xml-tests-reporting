/*
 * Copyright 2008 JetBrains s.r.o.
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
import jetbrains.buildServer.xmlReportPlugin.antJUnit.AntJUnitReportParser;
import org.jetbrains.annotations.NotNull;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;


public class NUnitReportParser extends AntJUnitReportParser {
  public static final String TYPE = "nunit";
  private static final String TMP_REPORT_DIRECTORY = "/junit_reports";

  private NUnitToJUnitReportTransformer myReportTransformer;
  private File myTmpReportDir;

  public NUnitReportParser(BaseServerLoggerFacade logger, String checkoutDir) {
    super(logger);
    try {
      myReportTransformer = new NUnitToJUnitReportTransformer();
    } catch (TransformerConfigurationException e) {
      myLogger.warning("NUnit report parser couldn't instantiate transformer");
    }
    myTmpReportDir = new File(checkoutDir + TMP_REPORT_DIRECTORY);
    myTmpReportDir.mkdirs();
  }

  public int parse(@NotNull File report, int testsToSkip) {
    final File junitReport = new File(myTmpReportDir.getPath() + "/" + report.getName());
    try {
      myReportTransformer.transform(report, junitReport);
    } catch (TransformerException e) {
//      myLogger.debugToAgentLog("Couldn't transform NUnit report, report may be uncomplete");
      junitReport.delete();
      return 0;
    }
    return super.parse(junitReport, testsToSkip);
  }
}
