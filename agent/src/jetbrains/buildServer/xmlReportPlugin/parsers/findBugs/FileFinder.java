/*
 * Copyright 2000-2022 JetBrains s.r.o.
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
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import jetbrains.buildServer.util.fileLookup.MemorizingFileLookup;
import jetbrains.buildServer.util.fileLookup.MemorizingLookup;
import jetbrains.buildServer.util.fileLookup.MemorizingZipFileLookup;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


class FileFinder {
  @NotNull
  private final List<Entry> myJars = new ArrayList<Entry>();
  @Nullable
  private MemorizingLookup<String, String, Entry> myLookup;

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
  @Contract("null -> null")
  public String getVeryFullFilePath(@Nullable String filePath) {
    if (filePath == null) return null;

    filePath = getDependentPath(filePath, File.separator);

    if (myLookup == null) {
      myLookup = new MemorizingLookup<String, String, Entry>(myJars) {
        @Override
        protected String lookupInside(@NotNull final Entry entry, @NotNull final String path) {
          return entry.getFilePath(path);
        }
      };
    }
    return myLookup.lookup(filePath);
  }

  public void close() {
    for (Entry jar : myJars) {
      jar.close();
    }
    myJars.clear();
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
    @NotNull
    private final MemorizingFileLookup myLookup;

    public DirectoryEntry(@NotNull String root) {
      myLookup = new MemorizingFileLookup(new File(root));
    }

    @Override
    public String getFilePath(@NotNull String fileName) {
      final File found = myLookup.lookup(myLookup.createFileInfo(fileName));
      return found == null ? null : found.getPath();
    }
  }

  private static final class ArchiveEntry extends Entry {
    @NotNull
    private final ZipFile myArchive;
    @NotNull
    private final MemorizingZipFileLookup myLookup;

    public ArchiveEntry(@NotNull ZipFile archive) {
      myArchive = archive;
      myLookup = new MemorizingZipFileLookup(archive);
    }

    @Override
    public String getFilePath(@NotNull String fileName) {
      return myLookup.lookup(MemorizingZipFileLookup.createFileInfo(fileName));
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
