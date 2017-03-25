/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public abstract class BaseXmlXppAbstractParser extends XmlXppAbstractParser {

  protected abstract class ORHandler implements CloseableHandler, XmlHandler {
    private final List<XmlHandler> myDelegates;
    private boolean myMatched = false;

    public ORHandler(XmlHandler... delegates) {
      myDelegates = Arrays.asList(delegates);
    }

    @Override
    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
      final XmlHandler handler = findHandler(reader.getLocalName());
      if (handler == null) return reader.noDeep();
      myMatched = true;
      return handler.processElement(reader);
    }

    @Override
    public boolean accepts(@NotNull final String name) {
      return findHandler(name) != null;
    }

    @Override
    public void close() {
      finished(myMatched);
    }

    @Nullable
    private XmlHandler findHandler(@NotNull final String name) {
      for (XmlHandler handler : myDelegates) {
        if (handler.accepts(name)) {
          return handler;
        }
      }
      return null;
    }

    protected abstract void finished(boolean matched);

    @NotNull
    public List<XmlHandler> asList() {
      return Collections.<XmlHandler>singletonList(this);
    }
  }

}
