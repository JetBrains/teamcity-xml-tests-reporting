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

import jetbrains.buildServer.agent.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.pmd.PmdReportParser;
import org.junit.Test;

import java.util.Map;


public class PmdReportParserTest extends ParserTestCase {
  @Override
  protected String getType() {
    return PmdReportParser.TYPE;
  }

  @Override
  protected XmlReportParser getParser(StringBuilder results) {
    final InspectionReporter reporter = TestUtil.createInspectionReporter(results);

    return new PmdReportParser(reporter, "##BASE_DIR##");
  }

  @Override
  protected void prepareParams(Map<String, String> paramsMap) {
    XmlReportPluginUtil.enableXmlReportParsing(paramsMap, getType());
    XmlReportPluginUtil.setMaxErrors(paramsMap, 5);
    XmlReportPluginUtil.setMaxWarnings(paramsMap, 5);
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.xml");
  }
}
