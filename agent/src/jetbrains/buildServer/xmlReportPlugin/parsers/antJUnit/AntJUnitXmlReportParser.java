

package jetbrains.buildServer.xmlReportPlugin.parsers.antJUnit;

import java.util.*;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.DurationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 17:50
 */
class AntJUnitXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;
  @NotNull
  private final DurationParser myDurationParser;

  public AntJUnitXmlReportParser(@NotNull Callback callback, @NotNull DurationParser durationParser) {
    myCallback = callback;
    myDurationParser = durationParser;
  }

  private static final Set<String> EXECUTED_STATUSES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
    "run", "passed", "success", "failure", "failed", "error"
  )));

  @Override
  protected List<XmlHandler> getRootHandlers() {
    final Handler handler = getSuiteHandler();
    return new ORHandler(
      elementsPath(handler, "testsuite"),
      elementsPath(handler, "testsuites", "testsuite")
    ) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.unexpectedFormat("\"testsuites\" or \"testsuite\" root element expected");
      }
    }.asList();
  }

  private Handler getSuiteHandler() {
    return new Handler() {
      @Override
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String name = reader.getAttribute("name");
          final String pack = reader.getAttribute("package");

          final String suiteName = (pack == null || name != null && name.startsWith(pack) ? "" : pack + ".") + name;
          myCallback.suiteFound(suiteName);

          return reader.visitChildren(
            elementsPath(new Handler() {
              @Override
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
              @Override
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
              @Override
              public void setText(@NotNull final String text) {
                myCallback.suiteSystemOutFound(suiteName, text.trim());
              }
            }, "system-out"),
            elementsPath(new TextHandler() {
              @Override
              public void setText(@NotNull final String text) {
                myCallback.suiteSystemErrFound(suiteName, text.trim());
              }
            }, "system-err"),
            elementsPath(new Handler() {
              @Override
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                final String name = reader.getAttribute("name");
                final String className = reader.getAttribute("classname");

                final TestData testData = new TestData();

                testData.setName((className == null || name != null && name.startsWith(className) ? "" : className + ".") + name);
                testData.setDuration(myDurationParser.parseTestDuration(reader.getAttribute("time")));
                testData.setExecuted(isExecuted(reader));

                return reader.visitChildren(
                  elementsPath(new Handler() {
                    @Override
                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                      testData.setExecuted(true);
                      return processTestFailure(reader, testData);
                    }
                  }, "failure"),
                  elementsPath(new Handler() {
                    @Override
                    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                      return processTestFailure(reader, testData);
                    }
                  }, "error"),
                  elementsPath(new TextHandler() {
                    @Override
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
                    @Override
                    public void setText(@NotNull final String text) {
                      testData.setDuration(myDurationParser.parseTestDuration(text.trim()));
                    }
                  }, "time")
                ).than(new XmlAction() {
                  @Override
                  public void apply() {
                    myCallback.testFound(testData);
                  }
                });
              }
            }, "testcase"),
            elementsPath(getSuiteHandler(), "testsuite")
          ).than(new XmlAction() {
            @Override
            public void apply() {
              myCallback.suiteFinished(suiteName);
            }
          });
        }
      };
  }

  @NotNull
  private XmlReturn processTestFailure(@NotNull XmlElementInfo reader, @NotNull final TestData testData) {
    if (testData.getFailureType() != null || testData.getFailureMessage() != null) {
      return reader.noDeep();
    }

    testData.setFailureType(reader.getAttribute("type"));
    testData.setFailureMessage(reader.getAttribute("message"));

    return reader.visitText(new TextHandler() {
      @Override
      public void setText(@NotNull final String text) {
        testData.setFailureStackTrace(text.trim());
      }
    });
  }

  private static boolean isExecuted(@NotNull XmlElementInfo reader) {
    final String executed = reader.getAttribute("executed");
    if (executed != null) return Boolean.parseBoolean(executed);
    final String rawStatus = reader.getAttribute("status");
    return StringUtil.isEmptyOrSpaces(rawStatus) || EXECUTED_STATUSES.contains(rawStatus.toLowerCase());
  }

  public interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteFailureFound(@Nullable String suiteName, @Nullable String type, @Nullable String message, @Nullable String trace);

    void suiteErrorFound(@Nullable String suiteName, @Nullable String type, @Nullable String message, @Nullable String trace);

    void suiteSystemOutFound(@Nullable String suiteName, @Nullable String message);

    void suiteSystemErrFound(@Nullable String suiteName, @Nullable String message);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);

    void unexpectedFormat(@NotNull String msg);
  }
}