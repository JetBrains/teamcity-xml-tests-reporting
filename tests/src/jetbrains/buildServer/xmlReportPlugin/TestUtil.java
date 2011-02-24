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

import java.io.*;
import java.util.*;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatesReporter;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatingFragment;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionReporter;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.tests.TestReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class TestUtil {
  static public List<String> readFileToList(@NotNull final File file) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    final List<String> lines = new ArrayList<String>();
    String line = reader.readLine();
    while (line != null) {
      lines.add(line.replace("/", File.separator).replace("\\", File.separator));
      line = reader.readLine();
    }
    Collections.sort(lines);
    return lines;
  }

  public static String getTestDataPath(final String fileName, final String folderName) throws FileNotFoundException {
    return getTestDataFile(fileName, folderName).getPath();
  }

  public static String getAbsoluteTestDataPath(@Nullable final String fileName, @NotNull final String folderName) throws FileNotFoundException {
    return getTestDataFile(fileName, folderName).getAbsolutePath();
  }

  public static File getTestDataFile(final String fileName, final String folderName) throws FileNotFoundException {
    final String relativeFileName = "testData" + (folderName != null ? File.separator + folderName : "") + (fileName != null ? File.separator + fileName : "");
    final File file1 = new File(relativeFileName);
    if (file1.exists()) {
      return file1;
    }
    final File file2 = new File("tests" + File.separator + relativeFileName);
    if (file2.exists()) {
      return file2;
    }
    final File file3 = new File("svnrepo" + File.separator + "xml-tests-reporting" + File.separator + "tests" + File.separator + relativeFileName);
    if (file3.exists()) {
      return file3;
    }
    return new File(getTestDataPath("", folderName), fileName);
  }

  public static InspectionReporter createInspectionReporter(final StringBuilder results) {
    return new InspectionReporter() {
      public void reportInspection(@NotNull final InspectionResult inspection) {
        results.append(inspection.toString()).append("\n");
      }

      public void reportInspectionType(@NotNull final InspectionTypeResult inspectionType) {
        results.append(inspectionType.toString()).append("\n");
      }

      public void info(@NotNull final String message) {
        results.append("MESSAGE: ").append(message).append("\n");
      }

      public void warning(@NotNull final String message) {
        results.append("WARNING: ").append(message).append("\n");
      }

      public void error(@NotNull final String message) {
        results.append("ERROR: ").append(message).append("\n");
      }
    };
  }

  public static DuplicatesReporter createDuplicatesReporter(final StringBuilder results) {
    return new DuplicatesReporter() {
      public void startDuplicates() {
      }

      public void reportDuplicate(@NotNull DuplicationResult duplicate) {
        results.append("[Cost: ").append(duplicate.getTokens());
        results.append(" Hash: ").append(duplicate.getHash());
        results.append(" Lines: ").append(duplicate.getLines()).append("\n");
        for (final DuplicatingFragment fragment : duplicate.getFragments()) {
          results.append("[File: ").append(fragment.getPath());
          results.append(" Line: ").append(fragment.getLine());
          results.append("]\n");
        }
        results.append("]\n\n");
      }

      public void finishDuplicates() {
      }
    };
  }

  public static TestReporter createTestResultsWriter(final StringBuilder sb) {
    return new TestReporter() {
      public void openTestSuite(@NotNull final String name) {
        sb.append("TestSuite:").append(name).append("\n");
      }

      public void openTest(@NotNull final String name) {
        sb.append("  Test:").append(name).append("\n");
      }

      public void testStdOutput(@NotNull final String text) {
        sb.append("    StdOutput:").append(text).append("\n");
      }

      public void testErrOutput(@NotNull final String text) {
        sb.append("    ErrOutput:").append(text).append("\n");
      }

      public void testFail(final String error, @Nullable final String stacktrace) {
        sb.append("    Fail:").append(error).append(" Message: ").append(stacktrace).append("\n");
      }

      public void testIgnored(@NotNull final String message) {
        sb.append("    Ignored:").append(message).append("\n");
      }

      public void closeTest(final long duration) {
        sb.append("  EndTest:").append(duration).append("\n------------------------\n");
      }

      public void closeTestSuite() {
        sb.append("EndSuite").append("\n");
      }

      public void warning(@NotNull final String s) {
        sb.append("-->Warning: ").append(s).append("\n");
      }

      public void error(@NotNull final String s) {
        sb.append("-->Error: ").append(s).append("\n");
      }

      public void info(@NotNull final String message) {
        sb.append("-->Info: ").append(message).append("\n");
      }
    };
  }
}
