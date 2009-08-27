/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class FileFinder {
  private List<Entry> myJars;

  public FileFinder() {
    myJars = new LinkedList<Entry>();
  }

  public void addJars(List<String> jars) {
    for (String jar : jars) {
      addJar(jar);
    }
  }

  public void addJar(String jar) {
    System.out.println("Adding jar " + jar);
    if (jar.endsWith(".zip") || jar.endsWith(".jar")) {
      try {
        myJars.add(new ArchiveEntry(new ZipFile(jar)));
      } catch (IOException e) {
        //just ignore
      }
    } else if (jar.endsWith(".class")) {
      myJars.add(new ClassEntry(jar));
    } else {
      myJars.add(new DirectoryEntry(jar));
    }
  }

  public String getVeryFullFilePath(String filePath) {
    if (filePath == null) {
      return "";
    }
    if (filePath.contains("$") && filePath.endsWith(".java")) {
      filePath = filePath.substring(0, filePath.indexOf("$")) + ".class";
    }
    for (Entry jar : myJars) {
      final String veryFullFilePath = jar.getFilePath(filePath);
      if (veryFullFilePath != null) {
        return veryFullFilePath;
      }
    }
    return "";
  }

  public void close() {
    for (Entry jar : myJars) {
      jar.close();
    }
  }

  private static String getOSPath(String path) {
    return path.replace("\\", File.separator).replace("/", File.separator);
  }

  private static String getDependentPath(String path, String separator) {
    return path.replace("\\", separator).replace("/", separator);
  }

  private static abstract class Entry {
    public abstract String getFilePath(String fileName);

    public void close() {
    }
  }

  private static final class DirectoryEntry extends Entry {
    private final String myRoot;

    public DirectoryEntry(String root) {
      myRoot = root;
    }

    public String getFilePath(String fileName) {
      return getFilePathRecursive(new File(myRoot).listFiles(), getOSPath(fileName));
    }

    private String getFilePathRecursive(File[] files, String relativePath) {
      if (files == null) {
        return null;
      }
      int i = 0;
      while (i < files.length) {
        if (files[i].isFile()) {
          final String path = files[i].getAbsolutePath();
          if (path.endsWith(relativePath)) {
            return path;
          }
        } else if (files[i].isDirectory()) {
          final String path = getFilePathRecursive(files[i].listFiles(), relativePath);
          if (path != null) {
            return path;
          }
        }
        ++i;
      }
      return null;
    }
  }

  private static final class ArchiveEntry extends Entry {
    private final ZipFile myArchive;

    public ArchiveEntry(ZipFile archive) {
      myArchive = archive;
    }

    public String getFilePath(String fileName) {
      for (Enumeration<? extends ZipEntry> e = myArchive.entries(); e.hasMoreElements();) {
        final String entry = e.nextElement().getName();
        if (entry.endsWith(getDependentPath(fileName, "/"))) {
          final String path = myArchive.getName() + File.separator + getOSPath(entry);
          return path;
        }
      }
      return null;
    }

    public void close() {
      try {
        myArchive.close();
      } catch (IOException e) {
        //TODO: log somehow
      }
    }
  }

  private static final class ClassEntry extends Entry {
    private final String myFile;

    public ClassEntry(String file) {
      myFile = file;
    }

    public String getFilePath(String fileName) {
      System.out.println("getFilePath with fileName=" + fileName + "(OS path is" + getOSPath(fileName) + ")");
      if (myFile.endsWith(getOSPath(fileName))) {
        return myFile;
      }
      return null;
    }
  }
}
