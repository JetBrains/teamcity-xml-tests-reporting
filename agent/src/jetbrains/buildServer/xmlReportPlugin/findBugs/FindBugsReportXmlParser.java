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

package jetbrains.buildServer.xmlReportPlugin.findBugs;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 18.02.11
 * Time: 14:38
 */
public class FindBugsReportXmlParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public FindBugsReportXmlParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlXppAbstractParser.XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPath(new XmlXppAbstractParser.Handler() {
      public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
        final String version = reader.getAttribute("version");
        if (version != null) myCallback.findBugsVersionFound(version);

        return reader.visitChildren(
          elementsPath(new XmlXppAbstractParser.Handler() {
            public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
              final TextHandler textHandler = new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.jarFound(ParserUtils.formatText(text));
                }
              };
              return reader.visitChildren(
                elementsPath(textHandler, "Jar"),
                elementsPath(textHandler, "SrcDir")
              );
            }
          }, "Project"),

          elementsPath(new XmlXppAbstractParser.Handler() {
            public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
              final String type = reader.getAttribute("type");
              final String category = reader.getAttribute("category");
              final int priority = getInt(reader.getAttribute("priority"));

              final String[] clazz = new String[1];
              final String[] message = new String[1];
              final int[] line = new int[1];

              return reader.visitChildren(
                elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    clazz[0] = reader.getAttribute("classname");
                    return reader.visitChildren(elementsPath(new Handler() {
                      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                        line[0] = getInt(reader.getAttribute("start"));
                        return reader.noDeep();
                      }
                    }, "SourceLine")).than(new XmlAction() {
                      public void apply() {
                        myCallback.bugInstanceFound(reader.getAttribute("sourcepath"), clazz[0], line[0], type,
                                                    category, message[0], priority);
                      }
                    });
                  }
                }, "Class"),

                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    if (message[0] == null) message[0] = ParserUtils.formatText(text);
                  }
                }, "ShortMessage", "LongMessage")
              );
            }
          }, "BugInstance")
        );
      }
    }, "BugCollection"));
  }

  private static int getInt(@Nullable String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static interface Callback {
    void findBugsVersionFound(@NotNull String version);
    void jarFound(@NotNull String jar);
    void bugInstanceFound(@Nullable String file, @Nullable String clazz, int line,
                          @Nullable String type, @Nullable String category, @Nullable String message, int priority);
  }
}
