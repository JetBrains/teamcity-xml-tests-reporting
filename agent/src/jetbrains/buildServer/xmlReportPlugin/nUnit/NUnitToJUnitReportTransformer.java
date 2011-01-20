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

package jetbrains.buildServer.xmlReportPlugin.nUnit;

import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;


class NUnitToJUnitReportTransformer {
  private final Templates myTemplates;

  public NUnitToJUnitReportTransformer(String schema) throws TransformerConfigurationException {
    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    myTemplates = transformerFactory.newTemplates(new StreamSource(getClass().getResourceAsStream(schema)));
  }

  public void transform(@NotNull File nUnitReport, @NotNull File jUnitReport) throws Exception {
    final InputStream nUnitInputStream = new FileInputStream(nUnitReport);
    final OutputStream jUnitOutputStream = new FileOutputStream(jUnitReport);
    try {
      final StreamSource source = new StreamSource(nUnitInputStream);
      final StreamResult result = new StreamResult(jUnitOutputStream);
      myTemplates.newTransformer().transform(source, result);
    } finally {
      FileUtil.close(nUnitInputStream);
      FileUtil.close(jUnitOutputStream);
    }
  }
}
