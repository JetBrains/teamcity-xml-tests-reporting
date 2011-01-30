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

import jetbrains.buildServer.xmlReportPlugin.checkstyle.CheckstyleReportParser;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * User: vbedrosova
 * Date: 25.12.2009
 * Time: 15:01:28
 */
public class CheckstyleReportParserTest extends BaseParserTestCase {
  private static final String TYPE = "checkstyle";

  @NotNull
  @Override
  protected Parser getParser() {
    return new CheckstyleReportParser(getXMLReader(), getInspectionReporter(), getBaseDir(), getLogger());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return TYPE;
  }

  private void runTest(final String fileName) throws Exception {
    parse(fileName);
    assertResultEquals(getExpectedResult(fileName + ".gold"));
  }

  @Test
  public void testNoInspections() throws Exception {
    runTest("noInspections.xml");
  }

  @Test
  public void testOneErrorOneWarningOneInfo() throws Exception {
    runTest("oneErrorOneWarningOneInfo.xml");
  }

  @Test
  public void testException() throws Exception {
    runTest("exception.xml");
  }

  @Test
  public void testBig() throws Exception {
    runTest("big.xml");
  }
}
