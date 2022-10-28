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

package jetbrains.buildServer.xmlReportPlugin.parsers.testng;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestNGXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;
  @NotNull
  private final DurationParser myDurationParser;

  public TestNGXmlReportParser(@NotNull final Callback callback, @NotNull final DurationParser durationParser) {
    myCallback = callback;
    myDurationParser = durationParser;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    final Handler handler = getSuiteHandler();
    return new ORHandler(
      elementsPath(handler, "testng-results")
    ) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.unexpectedFormat("\"testng-results\" root element expected");
      }
    }.asList();
  }

  private Handler getSuiteHandler() {
    return new Handler() {

      @Override
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(
          elementsPath(new Handler() {
            @Override
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final List<String> message = new ArrayList<String>();
              return reader.visitChildren(
                elementsPath(new TextHandler() {
                  @Override
                  public void setText(@NotNull final String text) {
                    message.add(text.trim());
                  }
                }, "line")
              ).than(new XmlAction() {
                @Override
                public void apply() {
                  myCallback.suiteSystemOutFound("TestNG", StringUtil.join(message, "\n"));
                }
              });
            }
          }, "reporter-output"),
          elementsPath(new Handler() {
            @Override
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final Suite suite = new Suite(reader.getAttribute("name"), SuiteSource.SUITE);
              if (suite.isValid(SuiteSource.SUITE)) {
                myCallback.suiteFound(suite.getName(SuiteSource.SUITE));
              }
              return reader.visitChildren(
                elementsPath(new Handler() {
                  @Override
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    suite.set(reader.getAttribute("name"), SuiteSource.TEST);
                    if (suite.isValid(SuiteSource.TEST)) {
                      myCallback.suiteFound(suite.getName(SuiteSource.TEST));
                    }
                    return reader.visitChildren(
                      elementsPath(new Handler() {
                        @Override
                        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                          final String className = reader.getAttribute("name");
                          suite.set(className, SuiteSource.CLASS);
                          if (suite.isValid(SuiteSource.CLASS)) {
                            myCallback.suiteFound(suite.getName(SuiteSource.CLASS));
                          }

                          return reader.visitChildren(
                            elementsPath(new Handler() {
                              @Override
                              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                                final TestData testData = new TestData();
                                testData.setClassName(className);
                                testData.setMethodName(reader.getAttribute("name"));
                                testData.setDuration(myDurationParser.parseTestDuration(reader.getAttribute("duration-ms")));
                                testData.setStatus(reader.getAttribute("status"));
                                return reader.visitChildren(
                                  elementsPath(new Handler() {
                                    @Override
                                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                                      return reader.visitChildren(
                                        elementsPath(new Handler() {
                                          @Override
                                          public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                                            final String index = reader.getAttribute("index");
                                            return reader.visitChildren(
                                              elementsPath(new TextHandler() {
                                                @Override
                                                public void setText(@NotNull final String text) {
                                                  testData.addParam(index, text.trim());
                                                }
                                              }, "value"));
                                          }
                                        }, "param"));
                                    }
                                  }, "params"),
                                  elementsPath(new Handler() {
                                    @Override
                                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                                      return reader.visitChildren(
                                        elementsPath(new TextHandler() {
                                          @Override
                                          public void setText(@NotNull final String text) {
                                            testData.appendMessageLine(text.trim());
                                          }
                                        }, "line")
                                      );
                                    }
                                  }, "reporter-output"),
                                  elementsPath(new Handler() {
                                    @Override
                                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                                      testData.setFailureType(reader.getAttribute("class"));
                                      return reader.visitChildren(
                                        elementsPath(new TextHandler() {
                                          @Override
                                          public void setText(@NotNull final String text) {
                                            testData.setFailureMessage(text.trim());
                                          }
                                        }, "message"),
                                        elementsPath(new TextHandler() {
                                          @Override
                                          public void setText(@NotNull final String text) {
                                            testData.setFailureStackTrace(text.trim());
                                          }
                                        }, "short-stacktrace"),
                                        elementsPath(new TextHandler() {
                                          @Override
                                          public void setText(@NotNull final String text) {
                                            testData.setFailureStackTrace(text.trim());
                                          }
                                        }, "full-stacktrace")
                                      );
                                    }
                                  }, "exception")
                                ).than(new XmlAction() {
                                  @Override
                                  public void apply() {
                                      myCallback.testFound(testData);
                                  }
                                });
                              }
                            }, "test-method")
                          ).than(new XmlAction() {
                            @Override
                            public void apply() {
                              if (suite.isValid(SuiteSource.CLASS)) {
                                myCallback.suiteFinished(suite.remove(SuiteSource.CLASS));
                              }
                            }
                          });
                        }
                      }, "class")
                    ).than(new XmlAction() {
                      @Override
                      public void apply() {
                        if (suite.isValid(SuiteSource.TEST)) {
                          myCallback.suiteFinished(suite.remove(SuiteSource.TEST));
                        }
                      }
                    });
                  }
                }, "test")
              ).than(new XmlAction() {
                @Override
                public void apply() {
                  if (suite.isValid(SuiteSource.SUITE)) {
                    myCallback.suiteFinished(suite.remove(SuiteSource.SUITE));
                  }
                }
              });
            }
          }, "suite")
        );
      }
    };
  }

  public interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteSystemOutFound(@Nullable String suiteName, @Nullable String message);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);

    void unexpectedFormat(@NotNull String msg);
  }

  private static class Suite {
    private final Map<SuiteSource, String> data = new EnumMap<SuiteSource, String>(SuiteSource.class);

    Suite(@Nullable final String name, @NotNull final SuiteSource source) {
      set(name, source);
    }

    boolean isValid(@NotNull final SuiteSource source) {
      return data.get(source) != null;
    }

    @Nullable
    public String getName(final SuiteSource source) {
      return data.get(source);
    }

    public void set(@Nullable final String name, @NotNull final SuiteSource source) {
      data.put(source, name);
    }

    @Nullable
    public String remove(@NotNull final SuiteSource source) {
        return data.remove(source);
    }

  }

  private enum SuiteSource {
    SUITE, TEST, CLASS
  }

}
