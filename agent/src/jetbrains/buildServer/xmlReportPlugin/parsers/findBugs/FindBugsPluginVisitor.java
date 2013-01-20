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

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 19:04
 */
class FindBugsPluginVisitor {
  @NotNull
  public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(FindBugsPluginVisitor.class);

  @NotNull
  private final Callback myCallback;

  public FindBugsPluginVisitor(@NotNull Callback callback) {
    myCallback = callback;
  }

  public void visit(@NotNull final File findBugsHome) {
    LOG.info("Visiting FindBugs plugins under " + findBugsHome);

    final File corePlugin = new File(findBugsHome, "lib/findbugs.jar");
    if (!corePlugin.isFile()) {
      LOG.info("Couldn't find core plugin " + corePlugin + ". Ensure specified FindBugs home path is correct");
    } else {
      load(corePlugin);
    }
    final File pluginFolder = new File(findBugsHome, "plugin");
    final File[] plugins = pluginFolder.listFiles();
    if ((plugins == null) || (plugins.length == 0)) {
      return;
    }
    for (File p : plugins) {
      if (p.getAbsolutePath().endsWith(".jar")) {
        load(p);
      }
    }
  }

  private void load(@NotNull File file) {
    JarFile jar = null;
    try {
      jar = new JarFile(file);

      final JarEntry messages = jar.getJarEntry("messages.xml");

      if (messages == null) {
        LOG.warn("Couldn't find messages.xml in plugin " + file);
        return;
      }

      final File tempFile = FileUtil.createTempFile("", "");
      InputStream jarFileStream = null;
      FileOutputStream tempFileStream = null;
      try {
        jarFileStream = jar.getInputStream(messages);
        tempFileStream = new FileOutputStream(tempFile);

        FileUtil.copy(jarFileStream, tempFileStream);
      } finally {
        FileUtil.close(jarFileStream);
        FileUtil.close(tempFileStream);
      }

      myCallback.pluginFound(tempFile);

      FileUtil.delete(tempFile);
    } catch (Exception e) {
      LOG.warn("Couldn't copy out messages.xml from plugin " + file, e);
    } finally {
      try {
        if (jar != null) {
          jar.close();
        }
      } catch (IOException e) {
        LOG.warn("Couldn't close plugin " + file, e);
      }
    }
  }

  public static interface Callback {
    void pluginFound(@NotNull File messages);
  }
}
