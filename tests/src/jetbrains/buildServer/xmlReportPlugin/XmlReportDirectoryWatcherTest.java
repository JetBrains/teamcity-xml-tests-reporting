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

import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.util.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;

@SuppressWarnings({"ResultOfMethodCallIgnored"})
public class XmlReportDirectoryWatcherTest extends TestCase {
  @NotNull
  private static final String JUNIT = "junit";
  @NotNull
  private static final List<String> TYPES = Arrays.asList(JUNIT);
  @NotNull
  private static final String METHOD_NOT_IMPLEMENTED = "Method not implemented";

  private TempFiles myTempFiles;
  private File myWorkDir;

  @NotNull
  private Set<String> getPathsStrings(@NotNull final Collection<File> paths) {
    final Set<String> pathsStr = new HashSet<String>(paths.size());
    for (final File path : paths) {
      if (path.getPath().startsWith("-:") || path.getPath().startsWith("+:")) {
        pathsStr.add(path.getPath());
        continue;
      }
      pathsStr.add(FileUtil.getRelativePath(myWorkDir, path));
    }
    return pathsStr;
  }

  @Override
  @Before
  public void setUp() throws IOException {
    myTempFiles = new TempFiles();
    myWorkDir = myTempFiles.createTempDir();
  }

  @Override
  @After
  public void tearDown() throws Exception {
    myTempFiles.cleanup();
    super.tearDown();
  }

  private static class ReportQueueMock extends ReportQueue {
    @NotNull
    private final StringBuilder myBuffer;

    public ReportQueueMock(@NotNull final StringBuilder b) {
      myBuffer = b;
    }

    @Override
    public void put(@NotNull final ReportContext context) {
      myBuffer.append(context.getFile().getAbsolutePath()).append("\n");
    }
  }

  private File getFile(String name) {
    return new File(myWorkDir, name);
  }

  private File createFile(String name) {
    final File f = getFile(name);
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return f;
  }

  private File createDir(String name) {
    final File f = getFile(name);
    f.mkdir();
    return f;
  }

  private void runTest(final String fileName, Set<File> input) throws Exception {
    final String expectedFile = getAbsoluteTestDataPath(fileName + ".gold", "watcher");
    final String resultsFile = expectedFile.replace(".gold", ".tmp");

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();
    final BuildLoggerForTesting logger = new BuildLoggerForTesting(results);
    final XmlReportPluginParameters parameters = TestUtil.createParameters(logger, Arrays.asList("junit"), input, myWorkDir, null);
    final ReportQueue queue = new ReportQueueMock(results);
    final XmlReportDirectoryWatcher watcher = new XmlReportDirectoryWatcher(parameters, queue);

    watcher.signalStop();
    watcher.start();
    watcher.join();
    watcher.logTotals(logger);

    final File expected = new File(expectedFile);
    String baseDir = myWorkDir.getCanonicalPath();
    String actual = getActualString(results, baseDir);
    String expectedContent = readFile(expected, true).trim();
    if (!expectedContent.equals(actual)) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(actual);
      resultsWriter.close();

      assertEquals(actual, expectedContent, actual);
    }
  }

  private String getActualString(StringBuilder results, String baseDir) {
    return results.toString().replace(baseDir, "##BASE_DIR##").replaceAll("file\\d?.xml", "##FILE_XML##").replace("/", File.separator).replace("\\", File.separator).trim();
  }

  @Test
  public void testOneFile() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = createFile("file.xml");
    files.add(f);
    runTest("oneFile", files);
    f.delete();
  }

  @Test
  public void testOneEmptyDir() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = createDir("dir");
    files.add(f);
    runTest("oneEmptyDir", files);
    f.delete();
  }

  @Test
  public void testOneNotExists() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = getFile("smth");
    files.add(f);
    runTest("oneNotExists", files);
    f.delete();
  }

  @Test
  public void testOneEmptyMask() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = getFile("mask*");
    files.add(f);
    runTest("oneEmptyMask", files);
    f.delete();
  }

  @Test
  public void testOneDirWithFiles() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = createDir("dir");
    files.add(f);
    final File f1 = createFileInDir(f, "file1.xml");
    final File f2 = createFileInDir(f, "file2.xml");
    final File f3 = createFileInDir(f, "file3.xml");
    runTest("oneDirWithFiles", files);
    f1.delete();
    f2.delete();
    f3.delete();
    f.delete();
  }

  @Test
  public void testOneDirWithFilesExcludeOne() throws Exception {
    final Set<File> files = new HashSet<File>();
    final File f = createDir("dir");
    files.add(f);
    files.add(new File("-:dir/file3.xml"));
    final File f1 = createFileInDir(f, "file1.xml");
    final File f2 = createFileInDir(f, "file2.xml");
    final File f3 = createFileInDir(f, "file3.xml");
    runTest("oneDirWithFilesExcludeOne", files);
    f1.delete();
    f2.delete();
    f3.delete();
    f.delete();
  }

  private static File createFileInDir(File dir, String file) {
    final File f = new File(dir, file);
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return f;
  }
}
