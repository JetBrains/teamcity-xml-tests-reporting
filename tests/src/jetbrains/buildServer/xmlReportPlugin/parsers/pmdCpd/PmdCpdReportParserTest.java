/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

/**
 * User: vbedrosova
 * Date: 07.09.2010
 * Time: 15:08:19
 */
@Test
public class PmdCpdReportParserTest extends BaseParserTestCase {
  @NotNull
  @Override
  protected Parser getParser() {
    return new PmdCpdReportParser(getDuplicationReporter(), getBaseDir());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return "pmdCpd";
  }

  @Test
  public void testSimple() throws Exception {
    runTest("result.xml");
  }

  @Test
  @TestFor(issues = "TW-43120")
  public void testSimpleJavaEncoding() throws Exception {
    runTest("result2.xml");
  }

  private void runTest(final String reportName) throws Exception {
    parse(reportName);
    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }
}
