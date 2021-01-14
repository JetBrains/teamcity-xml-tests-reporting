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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmd;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

@Test
public class PmdReportParserTest extends BaseParserTestCase {
  private static final String TYPE = "pmd";
  @NotNull
  @Override
  protected Parser getParser() {
    return new PmdReportParser(getInspectionReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return TYPE;
  }

  @Test
  public void testSimple() throws Exception {
    runTest("simple.xml");
  }

  @Test
  public void testInner() throws Exception {
    runTest("inner.xml");
  }

  @Test(timeOut = 5 * 1000)
  public void testXmlBomb() throws Exception {
    // Should not be parsed at all.
    // So nothing would be found
    runTest("xml-bomb.xml");
  }

  @Test
  public void testXmlXXE_File() throws Exception {
    // Should be parsed, but external resource is not loaded
    runTest("xml-xxe-file.xml");
  }

  @Test
  public void testXmlXXE_File_First() throws Exception {
    // Should be parsed, but external resource is not loaded
    runTest("xml-xxe-file-first.xml");
  }

  @Test
  public void testXmlXXE_URL() throws Exception {
    // Should be parsed, but external resource is not loaded
    runTest("xml-xxe-url.xml");
  }

  private void runTest(final String reportName) throws Exception {
    parse(reportName);
    assertResultEquals(getExpectedResult(reportName + ".gold"));
  }
}
