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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @author Eugene Petrenko
 *         Created: 27.10.2008 12:10:02
 */
@Test
public class DurationParserTest {
  @Test
  public void test_ones() {
    doTest("01:01:01.00", 60*60*1000 + 60 * 1000 + 1000);
  }

  @Test
  public void test_hour() {
    doTest("01:00:00.00", 60*60*1000);
    doTest("01:00:00", 60*60*1000);
  }

  @Test
  public void test_minute() {
    doTest("00:01:00.00", 60*1000);
    doTest("00:01:00", 60*1000);
  }

  @Test
  public void test_second() {
    doTest("00:00:01.00", 1000);
    doTest("00:00:01", 1000);
  }

  @Test
  public void test_millis() {
    doTest("00:00:00.001", 1);
    doTest("00:00:00.001000", 1);
    doTest("00:00:00.000999", 1);
  }
  
  @Test
  public void test_all() {
    doTest("00:00:00.0002279", 1);
    doTest("00:00:00.0111223", 12);
    doTest("00:00:00.0086064", 9);
    doTest("00:00:01.0074699", 1008);
  }

  @Test
  public void test_parseRange() {
    doTest("2010-02-12T14:44:43.8081661+00:00", "2010-02-12T14:44:45.9393792+00:00", 2000);
    doTest("2010-02-12T14:44:43.8081661+01:00", "2010-02-12T14:44:45.9393792+01:00", 2000);
    doTest("2010-02-12T14:44:43.8081661-01:00", "2010-02-12T14:44:45.9393792-01:00", 2000);
    doTest("2010-02-12T14:44:43.8081661-01:00", "-1", -1);

    doTest("2010-www02-12T14:44:43.8081661+00:00", "2010-02-12T14:44:45.9393792+00:00", -1);
    doTest("2010-www02-12T14:44:43.8081661+00:00", "2010-Z!@@#$02-12T14:44:45.9393792+00:00", -1);
    doTest(" ", "2010-02-12T14:44:45.9393792-01:00", -1);
    doTest(" ", "", -1);
  }

  private void doTest(final String str, final long expected) {
    long v = new DurationParser().parseTestDuration(str);
    assertEquals(v, expected,"Parsing " + str);
  }

  private void doTest(final String start, final String stop, final long expected) {
    long v = new DurationParser().parseTestDuration(start, stop);
    assertEquals(v, expected, "Parsing " + start + " -> " + stop);
  }
}
