/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import jetbrains.buildServer.xmlReportPlugin.BaseParserTestCase;
import jetbrains.buildServer.xmlReportPlugin.Parser;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Rassokhin
 */
public class CTestReportParserTest extends BaseParserTestCase {

  public static final String REPORT_DIR = "ctest";

  @NotNull
  @Override
  protected Parser getParser() {
    return new CTestReportParser(getTestReporter());
  }

  @NotNull
  @Override
  protected String getReportDir() {
    return REPORT_DIR;
  }

  private void runTest(final String dirName) throws Exception {
    String fileName = dirName + "/Test.xml";
    parse(fileName);
    assertResultEquals(getExpectedResult(fileName + ".gold"));
  }

  public void testSample1() throws Exception {
    runTest("sample1");
  }

  public void testFailedOne1() throws Exception {
    runTest("failedOne1");
  }

  public void testFailedOne2() throws Exception {
    runTest("failedOne2");
  }

  public void testEmpty() throws Exception {
    runTest("empty");
  }

  public void testOutputParsing() throws Exception {
    runTest("outputParsing");
  }

  public void test2Suites() throws Exception {
    String s = "2suites/";
    parse(s + "A/Test.xml");
    parse(s + "B/Test.xml");
    assertResultEquals(getExpectedResult(s + "result" + ".gold"));
  }
}
