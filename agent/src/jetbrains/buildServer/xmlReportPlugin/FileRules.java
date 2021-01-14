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

package jetbrains.buildServer.xmlReportPlugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * User: Victory.Bedrosova
 * Date: 1/31/12
 * Time: 4:55 PM
 */
public class FileRules implements Rules {
  @NotNull
  private final File myFile;

  public FileRules(@NotNull final File file) {
    myFile = file;
  }

  @NotNull
  public Collection<String> getBody() {
    return Collections.singletonList(myFile.getPath());
  }

  @NotNull
  public Collection<File> getPaths() {
    return Collections.singletonList(myFile);
  }

  @NotNull
  public List<File> collectFiles() {
    return myFile.isDirectory() ? collectFilesInFolder(myFile) : Collections.singletonList(myFile);
  }

  @NotNull
  private List<File> collectFilesInFolder(@NotNull File folder) {
    final File[] files = folder.listFiles();
    return files == null || files.length == 0 ? Collections.<File>emptyList() : Arrays.asList(files);
  }
}
