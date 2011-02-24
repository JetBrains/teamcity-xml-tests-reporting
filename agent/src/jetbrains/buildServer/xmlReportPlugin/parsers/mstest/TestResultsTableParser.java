package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 15:02:03
 */
class TestResultsTableParser extends XmlXppAbstractParser {
  private final Callback myCallback;
  private final DurationParser myDurationParser = new DurationParser();

  public TestResultsTableParser(final Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(getRootHandler9(), getRootHandler8());
  }

  protected XmlHandler getRootHandler8() {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull XmlElementInfo reader) {
        final TestResult result = new TestResult(TestResult.VS_Version.VS_8);

        return reader.visitChildren(
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.setTestId(text);
            }
          }, "id", "testId", "id"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.setDuration(text);
            }
          }, "duration"),
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull XmlElementInfo reader) {
              return reader.visitChildren(
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    result.setError(text);
                  }
                }, "message"),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    result.setStacktrace(text);
                  }
                }, "stackTrace")
              );
            }
          }, "errorInfo"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.addTrace(text);
            }
          }, "traceInfo", "trace"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.setOutcome(text);
            }
          }, "outcome", "value__"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.setStdOutput(text);
            }
          }, "stdout"),
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              result.setStdError(text);
            }
          }, "stderr")
        ).than(new XmlAction() {
          public void apply() {
            processTest(result);
          }
        });
      }
    }, "Tests", "UnitTestResult");
  }

  private void processRunError(final String text, final String exception) {
    myCallback.warning(text, exception);
  }

  private void processTest(final TestResult result) {
    final TestName testId = result.getTestName();
    if (testId == null) {
      myCallback.warning(testId, "Failed to read testId");
      return;
    }

    if (result.getOutcome() == null) {
      myCallback.warning(testId, "Failed to read testOutcome");
      return;
    }

    TestOutcome testOutcome = null;
    switch (result.getVersion()) {
      case VS_8:
        testOutcome = TestOutcome.parse8(result.getOutcome());
        break;
      case VS_9:
        testOutcome = TestOutcome.parse9(result.getOutcome());
        break;
    }

    myCallback.testFound(testId);

    final String stdOut = result.getStdOutput();
    if (stdOut != null) {
      myCallback.testOutput(testId, stdOut);
    }

    for (String trace : result.getTraces()) {
      if (!StringUtil.isEmptyOrSpaces(trace)) {
        myCallback.testOutput(testId, trace);
      }
    }

    final String stdErr = result.getStdError();
    if (stdErr != null) {
      myCallback.testError(testId, stdErr);
    }

    String error = result.getError();
    String stacktrace = result.getStacktrace();

    if (StringUtil.isEmptyOrSpaces(error) && StringUtil.isEmptyOrSpaces(stacktrace)) {
      error = testOutcome.getOutcomeName();
    }

    if (testOutcome.isIgnored()) {
      myCallback.testIgnored(testId, error, stacktrace);
    } else if (testOutcome.isFailed()) {
      myCallback.testException(testId, error, stacktrace);
    }

    final long duration = parseDuration(result, testId);
    myCallback.testFinished(testId, testOutcome, duration);
  }

  private long parseDuration(final TestResult result, final TestName testId) {
    final String sDuration = result.getDuration();
    final String startTime = result.getStartTime();
    final String endTime = result.getEndTime();

    if (!StringUtil.isEmptyOrSpaces(sDuration)) {
      final long duration = myDurationParser.parseTestDuration(sDuration);
      if (duration >= 0) return duration;
      myCallback.warning(testId, "Failed to parse duration from duration attribute '" + sDuration + "'. 0ms is chosen");
    }

    if (!StringUtil.isEmptyOrSpaces(startTime) && !StringUtil.isEmptyOrSpaces(endTime)) {
      final long duration = myDurationParser.parseTestDuration(startTime, endTime);
      if (duration >= 0) return duration;
      myCallback.warning(testId, "Failed to parse duration from startTime and endTime atributes '" + sDuration + "'. 0ms is chosen");
    }
    
    myCallback.warning(testId, "Failed to find test duration. 0ms is chosen");
    return 0;
  }

  protected XmlHandler getRootHandler9() {
    return elementsPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          return reader.visitChildren(
              elementsPath(new Handler() {
                public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                  final String outcome = reader.getAttribute("outcome");
                  final String[] textAndException = {null, null};

                  final TestOutcome testOutcome = TestOutcome.parse9(outcome);
                  if (testOutcome.isFailed() || testOutcome.isIgnored()) {
                    return reader.visitChildren(
                      elementsPath(new TextHandler() {
                        public void setText(@NotNull final String text) {
                          textAndException[0] = text;
                        }
                      }, "Text"),
                      elementsPath(new TextHandler() {
                        public void setText(@NotNull final String text) {
                          textAndException[1] = text;
                        }
                      }, "Exception")
                    ).than(new XmlAction() {
                      public void apply() {
                        if (textAndException[0] != null || textAndException[1] != null) {
                          processRunError(textAndException[0], textAndException[1]);
                        }
                      }
                    });
                  }
                  return reader.noDeep();
                }
              }, "ResultSummary", "RunInfos", "RunInfo"),
              elementsPath(new Handler() {
                public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                  return reader.visitChildren(getUnknown2008RecursiveResult(".*Result"));
                }
              }, "Results")
            );
        }
      }, "TestRun");
  }
  private XmlHandler[] getUnknown2008RecursiveResult(final String pattern) {
    return new XmlHandler[]{
      getUnknown2008Result(pattern),
      elementsPath(getUnknown2008Result(pattern), "TestResultAggregation"),
    };
  }


  private XmlHandler getUnknown2008Result(final String pattern) {
    return elementsPatternPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final TestResult result = new TestResult(TestResult.VS_Version.VS_9);

        result.setTestId(reader.getAttribute("testId"));
        result.setOutcome(reader.getAttribute("outcome"));
        result.setDuration(reader.getAttribute("duration"));
        result.setDataRowInfo(reader.getAttribute("dataRowInfo"));
        result.setStartTime(reader.getAttribute("startTime"));
        result.setEndTime(reader.getAttribute("endTime"));

        return reader.visitChildren(
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              result.setHasInnerResults(true);
              return reader.visitChildren(getUnknown2008RecursiveResult(pattern));
            }
          }, "InnerResults"),
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull XmlElementInfo reader) {
              return reader.visitChildren(
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    result.setStdOutput(text);
                  }
                }, "StdOut"),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    result.setStdError(text);
                  }
                }, "StdErr"),
                elementsPath(new Handler() {
                  public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                    return reader.visitChildren(
                      elementsPath(new TextHandler() {
                        public void setText(@NotNull final String text) {
                          result.setError(text);
                        }
                      }, "Message"),
                      elementsPath(new TextHandler() {
                        public void setText(@NotNull final String text) {
                          result.setStacktrace(text);
                        }
                      }, "StackTrace")
                    );
                  }
                }, "ErrorInfo"));
            }
          }, "Output")).than(new XmlAction() {
          public void apply() {
            if (!result.hasInnerResults()) {
              processTest(result);
            }
          }
        });
      }
    }, pattern);
  }
  public static interface Callback {
    void testFound(@NotNull TestName testId);

    void testOutput(@NotNull TestName testId, @NotNull String text);

    void testError(@NotNull TestName testId, @NotNull String text);

    void testException(@NotNull TestName testId, @Nullable String message, @Nullable String error);

    void testFinished(@NotNull TestName testId, @NotNull TestOutcome outcome, long duration);

    void testIgnored(@NotNull TestName testId, @Nullable String message, @Nullable String error);

    void warning(@Nullable TestName testId, @NotNull String message);

    void warning(@Nullable String message, @Nullable String exception);
  }
}
