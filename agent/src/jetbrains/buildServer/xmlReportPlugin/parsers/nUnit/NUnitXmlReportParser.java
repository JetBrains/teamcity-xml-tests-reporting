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

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 22.02.11
 * Time: 18:19
 */
class NUnitXmlReportParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public NUnitXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final Stack<SuitePartInfo> names = new Stack<SuitePartInfo>();

        return reader.visitChildren(elementsPath(new Handler() {
          public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
            return processTestSuite(reader, names);
          }
        }, "test-suite"));
      }
    }, "test-results"));
  }

  @NotNull
  private XmlReturn processTestSuite(@NotNull XmlElementInfo reader, @NotNull final Stack<SuitePartInfo> names) {
    names.push(new SuitePartInfo(reader.getAttribute("name")));
    return reader.visitChildren(
      elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String[] message = new String[1];
          final String[] trace = new String[1];

          return reader.visitChildren(
            elementsPath(new TextHandler() {
              public void setText(@NotNull final String text) {
                message[0] = text.trim();
              }
            }, "message "),
            elementsPath(new TextHandler() {
              public void setText(@NotNull final String text) {
                trace[0] = text.trim();
              }
            }, "stack-trace")
          ).than(new XmlAction() {
            public void apply() {
              myCallback.suiteFailureFound(getSuiteName(names), message[0], trace[0]);
            }
          });
        }
      }, "failure"),
      elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                return processTestSuite(reader, names);
              }
            }, "test-suite"),
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                final TestData testData = new TestData();

                testData.setName(reader.getAttribute("name"));
                testData.setExecuted(Boolean.parseBoolean(reader.getAttribute("executed")) && !"Inconclusive".equals(reader.getAttribute("result")));
                testData.setDuration(DurationParser.parseTestDuration(reader.getAttribute("time")));

                return reader.visitChildren(
                  elementsPath(new Handler() {
                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                      return reader.visitChildren(
                        elementsPath(new TextHandler() {
                          public void setText(@NotNull final String text) {
                            testData.setFailureMessage(text.trim());
                          }
                        }, "message"),
                        elementsPath(new TextHandler() {
                          public void setText(@NotNull final String text) {
                            testData.setFailureStackTrace(text.trim());
                          }
                        }, "stack-trace")
                      );
                    }
                  }, "failure")
                ).than(new XmlAction() {
                  public void apply() {
                    if (!names.peek().isFound()) {
                      myCallback.suiteFound(getSuiteName(names));
                      names.peek().setFound(true);
                    }
                    myCallback.testFound(testData);
                  }
                });
              }
            }, "test-case")
          );
        }
      }, "results")
    ).than(new XmlAction() {
      public void apply() {
        if (names.peek().isFound()) myCallback.suiteFinished(getSuiteName(names));
        names.pop();
      }
    });
  }

  @Nullable
  private String getSuiteName(@NotNull Stack<SuitePartInfo> parts) {
    if (parts.size() == 0) return null;

    final StringBuilder name = new StringBuilder();
    for (int i = parts.size() - 1; i >=0; --i) {
      if (name.length() > 0) name.append(".");
      name.append(parts.get(i).getName());
    }
    return name.toString();
  }

  private static final class SuitePartInfo {
    @Nullable
    private final String myName;
    private boolean myFound;

    private SuitePartInfo(@Nullable String name) {
      myName = name;
    }

    @Nullable
    public String getName() {
      return myName;
    }

    public boolean isFound() {
      return myFound;
    }

    public void setFound(boolean found) {
      myFound = found;
    }
  }

  public static interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteFailureFound(@Nullable String suiteName, @Nullable String message, @Nullable String trace);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);
  }
}
