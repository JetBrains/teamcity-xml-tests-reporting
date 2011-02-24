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

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 17:50
 */
class AntJUnitXmlReportParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public AntJUnitXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    final Handler handler = new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final String name = reader.getAttribute("name");
        final String pack = reader.getAttribute("package");

        final String suiteName = (pack == null || name != null && name.startsWith(pack) ? "" : pack + ".") + name;
        myCallback.suiteFound(suiteName);

        return reader.visitChildren(
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final String type = reader.getAttribute("type");
              final String message = reader.getAttribute("message");

              return reader.visitText(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.suiteFailureFound(suiteName, type, message, text.trim());
                }
              });
            }
          }, "failure"),
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final String type = reader.getAttribute("type");
              final String message = reader.getAttribute("message");

              return reader.visitText(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.suiteErrorFound(suiteName, type, message, text.trim());
                }
              });
            }
          }, "error"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              myCallback.suiteSystemOutFound(suiteName, text.trim());
            }
          }, "system-out"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              myCallback.suiteSystemErrFound(suiteName, text.trim());
            }
          }, "system-err"),
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final String name = reader.getAttribute("name");
              final String className = reader.getAttribute("classname");

              final TestData testData = new TestData();

              testData.setName((className == null || name != null && name.startsWith(className) ? "" : className + ".") + name);
              testData.setDuration(DurationParser.parseTestDuration(reader.getAttribute("time")));
              testData.setExecuted(reader.getAttribute("executed") == null || Boolean.parseBoolean(reader.getAttribute("executed")));

              return reader.visitChildren(
                elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {

                    testData.setFailureType(reader.getAttribute("type"));
                    testData.setFailureMessage(reader.getAttribute("message"));

                    return reader.visitText(new TextHandler() {
                      public void setText(@NotNull final String text) {
                        testData.setFailureStackTrace(text.trim());
                      }
                    });
                  }
                }, "failure"),
                elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {

                    testData.setFailureType(reader.getAttribute("type"));
                    testData.setFailureMessage(reader.getAttribute("message"));

                    return reader.visitText(new TextHandler() {
                      public void setText(@NotNull final String text) {
                        testData.setFailureStackTrace(text.trim());
                      }
                    });
                  }
                }, "error"),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    testData.setStdOut(text.trim());
                  }
                }, "system-out"),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    testData.setStdErr(text.trim());
                  }
                }, "system-err"),
                elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    testData.setExecuted(false);
                    return reader.noDeep();
                  }
                }, "skipped"),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    testData.setDuration(DurationParser.parseTestDuration(text.trim()));
                  }
                }, "time")
              ).than(new XmlAction() {
                public void apply() {
                  myCallback.testFound(testData);
                }
              });
            }
          }, "testcase")
        ).than(new XmlAction() {
          public void apply() {
            myCallback.suiteFinished(suiteName);
          }
        });
      }
    };
    return Arrays.asList(
      elementsPath(handler, "testsuite"),
      elementsPath(handler, "testsuites", "testsuite")
    );
  }

  public static interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteFailureFound(@Nullable String suiteName, @Nullable String type, @Nullable String message, @Nullable String trace);

    void suiteErrorFound(@Nullable String suiteName, @Nullable String type, @Nullable String message, @Nullable String trace);

    void suiteSystemOutFound(@Nullable String suiteName, @Nullable String message);

    void suiteSystemErrFound(@Nullable String suiteName, @Nullable String message);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);
  }
}
