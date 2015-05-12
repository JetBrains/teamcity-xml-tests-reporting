/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
import org.jetbrains.annotations.NotNull;

/**
 * Parser for "Test.xml" CTest for Dart reports.
 *
 * @author Vladislav.Rassokhin
 */
class TestXmlReportParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;
  @NotNull
  private final SecondDurationParser myDurationParser;

  public TestXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
    myDurationParser = new SecondDurationParser();
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(testingHandler());
      }
    }, "Site"));
  }

  @NotNull
  private XmlHandler testingHandler() {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(
//                // This does not needed
//                elementsPath(new TextHandler() {
//                  public void setText(@NotNull String text) {
//                  }
//                }, "StartDateTime"),
//                elementsPath(new TextHandler() {
//                  public void setText(@NotNull String text) {
//                  }
//                }, "StartTestTime"),
//                elementsPath(new TextHandler() {
//                  public void setText(@NotNull String text) {
//                  }
//                }, "EndDateTime"),
//                elementsPath(new TextHandler() {
//                  public void setText(@NotNull String text) {
//                  }
//                }, "EndTestTime"),
//                elementsPath(new TextHandler() {
//                  public void setText(@NotNull String text) {
//                  }
//                }, "ElapsedMinutes"),
            testsListHandler(), testHandler());
      }
    }, "Testing");
  }

  @NotNull
  private XmlHandler testsListHandler() {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull XmlElementInfo reader) {
        return reader.visitChildren(elementsPath(new Handler() {
          public XmlReturn processElement(@NotNull XmlElementInfo reader1) {
            return reader1.visitText(new TextHandler() {
              public void setText(@NotNull String text) {
//                myCallback.testInList(text);
              }
            });
          }
        }, "Test"));
      }
    }, "TestList");
  }


  @NotNull
  private XmlHandler testHandler() {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final TestData testData = new TestData(reader.getAttribute("Status"));
        return reader.visitChildren(
            elementsPath(new TextHandler() {
              public void setText(@NotNull String text) {
                testData.setName(text);
              }
            }, "Name"),
            elementsPath(new TextHandler() {
              public void setText(@NotNull String text) {
                testData.setPath(text);
              }
            }, "Path"),
            elementsPath(new TextHandler() {
              public void setText(@NotNull String text) {
                testData.setFullName(text);
              }
            }, "FullName"),
            elementsPath(new TextHandler() {
              public void setText(@NotNull String text) {
                testData.setFullCommandLine(text);
              }
            }, "FullCommandLine"),
            testResultsHandler(testData)).than(new XmlAction() {
          public void apply() {
            myCallback.testFound(testData);
          }
        });
      }
    }, "Test");
  }

  @NotNull
  private XmlHandler testResultsHandler(@NotNull final TestData testData) {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull XmlElementInfo reader) {
        return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                final String type = reader.getAttribute("type");
                final String name = reader.getAttribute("name");
                final String[] value = new String[1];
                return reader.visitChildren(elementsPath(new TextHandler() {
                  public void setText(@NotNull String text) {
                    value[0] = text;
                  }
                }, "Value")).than(new XmlAction() {
                  public void apply() {
                    if ("Execution Time".equals(name)) {
                      testData.setDuration(myDurationParser.parseTestDuration(value[0]));
                    } else if ("Exit Code".equals(name)) {
                      testData.setExitCode(value[0]);
                    } else if ("Exit Value".equals(name)) {
                      try {
                        testData.setExitValue(Integer.parseInt(value[0]));
                      } catch (NumberFormatException ignored) {
                      }
                    } else if ("Pass Reason".equals(name) || "Fail Reason".equals(name)) {
                      testData.setReason(value[0]);
                    } else if ("Completion Status".equals(name)) {
                      testData.setCompletionStatus(value[0]);
                    } else if ("Command Line".equals(name)) {
                      // don't do anything since it should already be in the FullCommandLine
                    } else {  // explicit measurement
                      testData.addNamedMeasurement(name, type, value[0]); // Just for logging other measurements
                    }
                  }
                });
              }
            }, "NamedMeasurement"),
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                final String[] compression = new String[1];
                final String[] value = new String[1];
                return reader.visitChildren(elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                    compression[0] = reader.getAttribute("compression");
                    return reader.visitText(new TextHandler() {
                      public void setText(@NotNull String text) {
                        value[0] = text;
                      }
                    });
                  }
                }, "Value")).than(new XmlAction() {
                  public void apply() {
                    testData.setLog(value[0], compression[0]);
                  }
                });
              }
            }, "Measurement"));
      }
    }, "Results");
  }


  public static interface Callback {
    void testFound(@NotNull TestData testData);

//    void testInList(@NotNull String name);
  }
}