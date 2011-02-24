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

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class FileFinder {
  @NotNull
  private final List<Entry> myJars = new LinkedList<Entry>();

  public void addJar(@NotNull String jar) {
    jar = getDependentPath(jar, File.separator);
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

  @Nullable
  public String getVeryFullFilePath(@Nullable String filePath) {
    if (filePath == null) return null;

    filePath = getDependentPath(filePath, File.separator);

    for (Entry jar : myJars) {
      final String veryFullFilePath = jar.getFilePath(filePath);
      if (veryFullFilePath != null) {
        return veryFullFilePath;
      }
    }
    return null;
  }

  public void close() {
    for (Entry jar : myJars) {
      jar.close();
    }
  }

  @NotNull
  private static String getDependentPath(@NotNull String path, @NotNull String separator) {
    return path.replace("\\", separator).replace("/", separator);
  }

  private static abstract class Entry {
    @Nullable
    public abstract String getFilePath(@NotNull String fileName);

    public void close() {}
  }

  private static final class DirectoryEntry extends Entry {
    private final String myRoot;

    public DirectoryEntry(String root) {
      myRoot = root;
    }

    @Override
    public String getFilePath(@NotNull String fileName) {
      return getFilePathRecursive(new File(myRoot).listFiles(), fileName);
    }

    private String getFilePathRecursive(File[] files, @NotNull String relativePath) {
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

    @Override
    public String getFilePath(@NotNull String fileName) {
      final String filePathInZip = getDependentPath(fileName, "/");
      for (Enumeration<? extends ZipEntry> e = myArchive.entries(); e.hasMoreElements();) {
        final String entry = e.nextElement().getName();
        if (entry.endsWith(filePathInZip)) {
          return myArchive.getName() + File.separator + getDependentPath(entry, File.separator);
        }
      }
      return null;
    }

    @Override
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

    @Override
    public String getFilePath(@NotNull String fileName) {
      return myFile.endsWith(fileName) ? myFile : null;
    }
  }
}
