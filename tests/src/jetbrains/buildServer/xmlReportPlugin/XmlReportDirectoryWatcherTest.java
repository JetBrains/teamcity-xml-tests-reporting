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

package jetbrains.buildServer.xmlReportPlugin;

import com.intellij.openapi.util.Pair;
import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.getAbsoluteTestDataPath;
import static jetbrains.buildServer.xmlReportPlugin.TestUtil.readFile;
import junit.framework.TestCase;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@RunWith(JMock.class)
public class XmlReportDirectoryWatcherTest extends TestCase {
  private Mockery myContext;

  private XmlReportPlugin createTestReportParserPlugin(final BaseServerLoggerFacade logger) {
    final XmlReportPlugin plugin = myContext.mock(XmlReportPlugin.class);
    myContext.checking(new Expectations() {
      {
        allowing(plugin).isStopped();
        will(returnValue(true));
        allowing(plugin).getLogger();
        will(returnValue(logger));
        ignoring(plugin);
      }
    });
    return plugin;
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    new File("workingDirForTesting").delete();
  }

  private static class LinkedBlockingQueueMock<E> extends LinkedBlockingQueue<E> {
    private final StringBuilder myBuffer;

    public LinkedBlockingQueueMock(StringBuilder b) {
      myBuffer = b;
    }

    public void put(E o) throws InterruptedException {
      final File f;
      if (o instanceof Pair) {
        final Pair<String, File> p = (Pair<String, File>) o;
        f = p.getSecond();
      } else {
        myBuffer.append("Trying to put illegal object to queue: ").append(o.toString());
        return;
      }
      myBuffer.append(f.getAbsolutePath()).append("\n");
    }
  }

  private static File getFile(String name) {
    return new File("workingDirForTesting" + File.separator + name);
  }

  private static File createFile(String name) {
    final File f = getFile(name);
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return f;
  }

  private static File createDir(String name) {
    final File f = getFile(name);
    f.mkdir();
    return f;
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

  private void runTest(final String fileName, List<File> input) throws Exception {
    final String expectedFile = getAbsoluteTestDataPath(fileName + ".gold", "watcher");
    final String resultsFile = expectedFile.replace(".gold", ".tmp");

    new File(resultsFile).delete();

    final StringBuilder results = new StringBuilder();
    final BuildLoggerForTesting logger = new BuildLoggerForTesting(results);
    final XmlReportPlugin plugin = createTestReportParserPlugin(logger);
    final LinkedBlockingQueue<Pair<String, File>> queue = new LinkedBlockingQueueMock<Pair<String, File>>(results);

    final XmlReportDirectoryWatcher watcher = new XmlReportDirectoryWatcher(plugin, input, "junit", queue);

//    final Thread stopper = new Thread(new Runnable() {
//      public void run() {
//        try {
//          Thread.sleep(2000);
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//        myContext.checking(new Expectations() {
//          {
//            allowing(plugin).isStopped();
//            will(returnValue(true));
//          }
//        });
//      }
//    });
//    stopper.start();
    watcher.run();
    watcher.logTotals();

    final File expected = new File(expectedFile);
    if (!readFile(expected).equals(results.toString())) {
      final FileWriter resultsWriter = new FileWriter(resultsFile);
      resultsWriter.write(results.toString());
      resultsWriter.close();

      assertEquals(readFile(expected), results.toString());
    }
  }

  @Test
  public void testEmpty() throws Exception {
    runTest("empty", new ArrayList<File>());
  }

  @Test
  public void testOneFile() throws Exception {
    final List<File> files = new ArrayList<File>();
    final File f = createFile("file");
    files.add(f);
    runTest("oneFile", files);
    f.delete();
  }

  @Test
  public void testOneEmptyDir() throws Exception {
    final List<File> files = new ArrayList<File>();
    final File f = createDir("dir");
    files.add(f);
    runTest("oneEmptyDir", files);
    f.delete();
  }

  @Test
  public void testOneNotExists() throws Exception {
    final List<File> files = new ArrayList<File>();
    final File f = getFile("smth");
    files.add(f);
    runTest("oneNotExists", files);
    f.delete();
  }

  @Test
  public void testOneEmptyMask() throws Exception {
    final List<File> files = new ArrayList<File>();
    final File f = getFile("mask*");
    files.add(f);
    runTest("oneEmptyMask", files);
    f.delete();
  }

  @Test
  public void testOneDirWithFiles() throws Exception {
    final List<File> files = new ArrayList<File>();
    final File f = createDir("dir");
    files.add(f);
    final File f1 = createFileInDir(f, "f1");
    final File f2 = createFileInDir(f, "f2");
    final File f3 = createFileInDir(f, "f3");
    runTest("oneDirWithFiles", files);
    f1.delete();
    f2.delete();
    f3.delete();
    f.delete();
  }
}
