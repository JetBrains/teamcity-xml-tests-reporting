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

package jetbrains.buildServer.xmlReportPlugin.nUnit;

import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.jetbrains.annotations.NotNull;


public class NUnitToJUnitReportTransformer {
  private static final String NUNIT_TO_JUNIT_XSL = "nunit-to-junit.xsl";

  private final Transformer myTransformer;

  public NUnitToJUnitReportTransformer() throws TransformerConfigurationException {
    final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    myTransformer = transformerFactory.newTransformer(new StreamSource(this.getClass().getResourceAsStream(NUNIT_TO_JUNIT_XSL)));
  }

  public void transform(@NotNull File nUnitReport, @NotNull File jUnitReport) throws TransformerException {
    final StreamSource source = new StreamSource(nUnitReport);
    final StreamResult result = new StreamResult(jUnitReport);
    myTransformer.transform(source, result);
  }
}
