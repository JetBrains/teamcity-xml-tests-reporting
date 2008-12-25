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

package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.testReportParserPlugin.nUnit.NUnitToJUnitReportTransformer;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class NUnitToJUnitReportTransformerTest {
  private static final String REPORT_DIR = "Tests/testData/nunit/";
  private static final String NUNIT_REPORT_NAME = REPORT_DIR + "nunit";
  private static final String JUNIT_REPORT_NAME = REPORT_DIR + "junit";
  private static final String JUNIT_EXPECTED_REPORT_NAME = REPORT_DIR + "junit_exp";

  private NUnitToJUnitReportTransformer myTransformer;

  @Before
  public void setUp() {
    try {
      myTransformer = new NUnitToJUnitReportTransformer();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    }
  }


  @Test
  public void test() {
    for (int i = 1; i < 2; ++i) {
      try {
        myTransformer = new NUnitToJUnitReportTransformer();
      } catch (TransformerConfigurationException e) {
        e.printStackTrace();
      }
      final File nUnitReport = new File(NUNIT_REPORT_NAME + "_" + i + ".xml");
      final File jUnitReport = new File(JUNIT_REPORT_NAME + "_" + i + ".xml");
      final File jUnitExpectedReport = new File(JUNIT_EXPECTED_REPORT_NAME + "_" + i + ".xml");

      assert nUnitReport.isFile();
      assert jUnitExpectedReport.isFile();
      jUnitReport.delete();

      try {
        myTransformer.transform(nUnitReport, jUnitReport);
      } catch (TransformerException e) {
        e.printStackTrace();
      }

//      assertTrue("Transformed report doesn't match expected", FileComparator.compare(jUnitReport, jUnitExpectedReport));
    }
  }

  public static class FileComparator {
    public static boolean compare(@NotNull File file1, @NotNull File file2) {
      int len = 0;
      if (file1.length() != file2.length()) {
        System.out.println("Files: " + file1.getPath() + " and " + file2.getPath() + " have different length");
        return false;
      } else {
        len = (int) (file1.length() / 2.0 + 1);
      }

      try {
        final FileReader reader2 = new FileReader(file2);
        final FileReader reader1 = new FileReader(file1);

        char[] buf1 = new char[len];
        char[] buf2 = new char[len];

        try {
          reader1.read(buf1);
          reader2.read(buf2);
        } catch (IOException e) {
          e.printStackTrace();
          return false;
        }

        final String s1 = new String(buf1);
        final String s2 = new String(buf2);

        return s1.equals(s2);

      } catch (FileNotFoundException e) {
        e.printStackTrace();
        return false;
      }
    }
  }

//  public static class XmlFileComparator {
//    public static boolean compare(@NotNull File file1, @NotNull File file2) {
//
//    }
//  }
}
