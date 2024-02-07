

package jetbrains.buildServer.xmlReportPlugin.parsers.ctest;

import java.util.List;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
import org.jetbrains.annotations.NotNull;

/**
 * Parser for "Test.xml" CTest for Dart reports.
 *
 * @author Vladislav.Rassokhin
 */
class TestXmlReportParser extends BaseXmlXppAbstractParser {
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
    // TODO: support for Coverage.xml & other reports produced by CTest
    return new ORHandler(elementsPath(getSiteHandler(), "Site")) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.unexpectedFormat("\"Site\" root element expected.");
      }
    }.asList();
  }

  @NotNull
  private Handler getSiteHandler() {
    return new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(testingHandler());
      }
    };
  }

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
                    if (name == null) return;
                    switch (name) {
                      case "Execution Time":
                        testData.setDuration(myDurationParser.parseTestDuration(value[0]));
                        break;
                      case "Exit Code":
                        testData.setExitCode(value[0]);
                        break;
                      case "Exit Value":
                        try {
                          testData.setExitValue(Integer.parseInt(value[0]));
                        } catch (NumberFormatException ignored) {
                        }
                        break;
                      case "Pass Reason":
                      case "Fail Reason":
                        testData.setReason(value[0]);
                        break;
                      case "Completion Status":
                        testData.setCompletionStatus(value[0]);
                        break;
                      case "Command Line":
                        // don't do anything since it should already be in the FullCommandLine
                        break;
                      default: // explicit measurement
                        testData.addNamedMeasurement(name, type == null ? "" : type, value[0]); // Just for logging other measurements
                        break;
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


  public interface Callback {
    void testFound(@NotNull TestData testData);
    void error(@NotNull String message);

    void unexpectedFormat(@NotNull String msg);
//    void testInList(@NotNull String name);
  }
}