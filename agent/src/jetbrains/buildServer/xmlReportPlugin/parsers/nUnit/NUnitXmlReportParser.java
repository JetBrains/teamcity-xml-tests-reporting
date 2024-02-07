

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;

import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.util.StringUtil.isEmpty;
import static jetbrains.buildServer.util.StringUtil.isNotEmpty;

/**
 * User: vbedrosova
 * Date: 22.02.11
 * Time: 18:19
 */
class NUnitXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;
  @NotNull
  private final SecondDurationParser myDurationParser;

  public NUnitXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
    myDurationParser = new SecondDurationParser();
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return new ORHandler(
      new Version2Handler().getRootHandler(),
      new Version3Handler().getRootHandler(),
      getGeneralFailureHandler()) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.error("must contain \"test-results\", \"test-run\" or \"stack-trace\" root element\nPlease check the NUnit sources for the supported XML Schema");
      }
    }.asList();
  }

  @NotNull
  private XmlHandler getGeneralFailureHandler() {
    return elementsPath(new TextHandler() {
      @Override
      public void setText(@NotNull final String text) {
        myCallback.failure("general failure:\n" + text);
      }
    }, "stack-trace");
  }

  private final class Version2Handler {
    @NotNull
    public XmlHandler getRootHandler() {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          return reader.visitChildren(suiteHandler(true));
        }
      }, "test-results");
    }

    @NotNull
    private XmlHandler suiteHandler(final boolean addLogging) {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String name = getSuiteName(reader.getAttribute("name"));
          final boolean ignored = ignored(reader);
          final boolean failed = !success(reader);

          if (addLogging) myCallback.suiteFound(name);

          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                return reader.visitChildren(suiteHandler(false), testHandler());
              }
            }, "results"),
            failureAndReasonHandler(name, ignored, failed, true)
          ).than(new XmlAction() {
            public void apply() {
              if (addLogging) myCallback.suiteFinished(name);
            }
          });
        }
      }, "test-suite");
    }

    @NotNull
    private XmlHandler testHandler() {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final TestData testData = new TestData();

          testData.setName(reader.getAttribute("name"));
          testData.setIgnored(ignored(reader));
          testData.setSuccess(success(reader));
          testData.setDuration(myDurationParser.parseTestDuration(reader.getAttribute("time")));

          return reader.visitChildren(
            elementsPatternPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                if ("failure".equals(reader.getLocalName())) testData.setSuccess(false);
                return reader.visitChildren(
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull final String text) {
                      testData.setMessage(text.trim());
                    }
                  }, "message"),
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull final String text) {
                      testData.setFailureStackTrace(text.trim());
                    }
                  }, "stack-trace")
                );
              }
            }, "failure|reason")
          ).than(new XmlAction() {
            public void apply() {
              myCallback.testFound(testData);
            }
          });
        }
      }, "test-case");
    }

    @Nullable
    private String getSuiteName(@Nullable String name) {
      if (name == null) return null;
      final String patchedName = name.replace('\\', '/');
      if (patchedName.contains("/")) {
        return patchedName.substring(patchedName.lastIndexOf('/') + 1);
      }
      return name;
    }

    private boolean ignored(@NotNull final XmlElementInfo reader) {
      final String result = reader.getAttribute("result");
      return "False".equalsIgnoreCase(reader.getAttribute("executed")) ||
             "Inconclusive".equals(result) ||
             "Ignored".equals(result) ||
             "Skipped".equals(result) ||
             "NotRunnable".equals(result);
    }

    private boolean success(@NotNull final XmlElementInfo reader) {
      final String result = reader.getAttribute("result");
      return "Success".equals(result) || Boolean.parseBoolean(reader.getAttribute("success")) && !ignored(reader);
    }
  }

  private final class FailureDetails {
    private String message; private String stackTrace;
  }

  private XmlHandler failureAndReasonHandler(final String name, final boolean ignored, final boolean failed, final boolean failOnFailure) {
    return elementsPatternPath(new Handler() {
      @Override
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final FailureDetails details = new FailureDetails();
        return reader.visitChildren(
          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              details.message = text.trim();
            }
          }, "message"),
          elementsPath(new TextHandler() {
            @Override
            public void setText(@NotNull final String text) {
              details.stackTrace = text.trim();
            }
          }, "stack-trace")
        ).than(new XmlAction() {
          @Override
          public void apply() {
            final String msg = getMessage();
            if (ignored) {
              myCallback.warning("suite " + name + " ignored" + msg);
            } else if (failed) {
              final String err = "suite " + name + " failure" + msg;
              if (failOnFailure) {
                myCallback.failure(err);
              } else {
                myCallback.warning(err);
              }
            } else if (isNotEmpty(msg)) {
              myCallback.message("suite " + name + msg);
            }
          }

          private String getMessage() {
            if (isEmpty(details.message) && isEmpty(details.stackTrace)) return "";
            return ": " + details.message + (isEmpty(details.stackTrace) ? "" : "\n" + details.stackTrace);
          }
        });
      }
    }, "failure|reason");
  }

  private final class Version3Handler {
    @NotNull
    public XmlHandler getRootHandler() {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          return reader.visitChildren(suiteHandler());
        }
      }, "test-run");
    }

    @NotNull
    private XmlHandler suiteHandler() {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String name = StringUtil.emptyIfNull(reader.getAttribute("name"));
          final String fullName = reader.getAttribute("fullname");
          final boolean addLogging = StringUtil.isEmpty(fullName) || fullName.endsWith(name);
          if (addLogging) myCallback.suiteFound(name);

          final boolean ignored = ignored(reader);
          final boolean failed = !success(reader);

          return reader.visitChildren(
            suiteHandler(),
            testHandler(),
            failureAndReasonHandler(name, ignored, failed, false)
          ).than(new XmlAction() {
            public void apply() {
              if (addLogging) myCallback.suiteFinished(name);
            }
          });
        }
      }, "test-suite");
    }

    @NotNull
    private XmlHandler testHandler() {
      return elementsPath(new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final TestData testData = new TestData();

          testData.setName(reader.getAttribute("name"));
          testData.setIgnored(ignored(reader));
          testData.setSuccess(success(reader));
          testData.setDuration(myDurationParser.parseTestDuration(reader.getAttribute("duration")));

          return reader.visitChildren(
            elementsPath(new TextHandler() {
              @Override
              public void setText(@NotNull final String text) {
                testData.setOutput(text.trim());
              }
            }, "output"),
            elementsPatternPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                if ("failure".equals(reader.getLocalName())) testData.setSuccess(false);
                return reader.visitChildren(
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull final String text) {
                      testData.setMessage(text.trim());
                    }
                  }, "message"),
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull final String text) {
                      testData.setFailureStackTrace(text.trim());
                    }
                  }, "stack-trace")
                );
              }
            }, "failure|reason")
          ).than(new XmlAction() {
            public void apply() {
              myCallback.testFound(testData);
            }
          });
        }
      }, "test-case");
    }

    private boolean ignored(@NotNull final XmlElementInfo reader) {
      final String result = reader.getAttribute("result");
      return "Inconclusive".equals(result) || "Skipped".equals(result);
    }

    private boolean success(@NotNull final XmlElementInfo reader) {
      return "Passed".equals(reader.getAttribute("result"));
    }
  }

  public interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);

    void failure(@NotNull String msg);

    void error(@NotNull String msg);

    void warning(@NotNull String msg);

    void message(@NotNull String msg);
  }
}

/*

Currently supported NUnit 2 schema is (for the NUnit 3 supported format see the NUNit 3 sources):

<?xml version="1.0" encoding="UTF-8" ?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">
    <xs:complexType name="failureType">
        <xs:sequence>
            <xs:element ref="message" />
            <xs:element ref="stack-trace" />
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="reasonType">
        <xs:sequence>
            <xs:element ref="message" />
        </xs:sequence>
    </xs:complexType>
    <xs:element name="message" type="xs:string" />
    <xs:complexType name="resultsType">
        <xs:choice>
            <xs:element name="test-suite" type="test-suiteType" maxOccurs="unbounded" />
            <xs:element name="test-case" type="test-caseType" maxOccurs="unbounded" minOccurs="0" />
        </xs:choice>
    </xs:complexType>
    <xs:element name="stack-trace" type="xs:string" />
    <xs:element name="test-results" type="resultType" />
    <xs:complexType name="categoriesType">
        <xs:sequence>
            <xs:element name="category" type="categoryType" maxOccurs="unbounded" minOccurs="1" />
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="categoryType">
        <xs:attribute name="name" type="xs:string" use="required" />
    </xs:complexType>
    <xs:complexType name="propertiesType">
        <xs:sequence>
            <xs:element name="property" type="propertyType" maxOccurs="unbounded" minOccurs="1" />
        </xs:sequence>
    </xs:complexType>
    <xs:complexType name="propertyType">
        <xs:attribute name="name" type="xs:string" use="required" />
        <xs:attribute name="value" type="xs:string" use="required" />
    </xs:complexType>
    <xs:complexType name="environmentType">
        <xs:attribute name="nunit-version" type="xs:string" use="required" />
        <xs:attribute name="clr-version" type="xs:string" use="required" />
        <xs:attribute name="os-version" type="xs:string" use="required" />
        <xs:attribute name="platform" type="xs:string" use="required" />
        <xs:attribute name="cwd" type="xs:string" use="required" />
        <xs:attribute name="machine-name" type="xs:string" use="required" />
        <xs:attribute name="user" type="xs:string" use="required" />
        <xs:attribute name="user-domain" type="xs:string" use="required" />
    </xs:complexType>
    <xs:complexType name="culture-infoType">
        <xs:attribute name="current-culture" type="xs:string" use="required" />
        <xs:attribute name="current-uiculture" type="xs:string" use="required" />
    </xs:complexType>
    <xs:complexType name="resultType">
        <xs:sequence>
            <xs:element name="environment" type="environmentType" />
            <xs:element name="culture-info" type="culture-infoType" />
            <xs:element name="test-suite" type="test-suiteType" />
        </xs:sequence>
        <xs:attribute name="name" type="xs:string" use="required" />
        <xs:attribute name="total" type="xs:decimal" use="required" />
    <xs:attribute name="errors" type="xs:decimal" use="required" />
    <xs:attribute name="failures" type="xs:decimal" use="required" />
    <xs:attribute name="inconclusive" type="xs:decimal" use="required" />
    <xs:attribute name="not-run" type="xs:decimal" use="required" />
    <xs:attribute name="ignored" type="xs:decimal" use="required" />
    <xs:attribute name="skipped" type="xs:decimal" use="required" />
    <xs:attribute name="invalid" type="xs:decimal" use="required" />
    <xs:attribute name="date" type="xs:string" use="required" />
        <xs:attribute name="time" type="xs:string" use="required" />
    </xs:complexType>
    <xs:complexType name="test-caseType">
        <xs:sequence>
            <xs:element name="categories" type="categoriesType" minOccurs="0" maxOccurs="1" />
            <xs:element name="properties" type="propertiesType" minOccurs="0" maxOccurs="1" />
            <xs:choice>
                <xs:element name="failure" type="failureType" minOccurs="0" />
                <xs:element name="reason" type="reasonType" minOccurs="0" />
            </xs:choice>
        </xs:sequence>
        <xs:attribute name="name" type="xs:string" use="required" />
        <xs:attribute name="description" type="xs:string" use="optional" />
        <xs:attribute name="success" type="xs:string" use="optional" />
        <xs:attribute name="time" type="xs:string" use="optional" />
        <xs:attribute name="executed" type="xs:string" use="required" />
        <xs:attribute name="asserts" type="xs:string" use="optional" />
    <xs:attribute name="result" type="xs:string" use="required" />
  </xs:complexType>
    <xs:complexType name="test-suiteType">
        <xs:sequence>
            <xs:element name="categories" type="categoriesType" minOccurs="0" maxOccurs="1" />
            <xs:element name="properties" type="propertiesType" minOccurs="0" maxOccurs="1" />
            <xs:choice>
                <xs:element name="failure" type="failureType" minOccurs="0" />
                <xs:element name="reason" type="reasonType" minOccurs="0" />
            </xs:choice>
            <xs:element name="results" type="resultsType" minOccurs="0" maxOccurs="1"/>
        </xs:sequence>
    <xs:attribute name="type" type="xs:string" use="required" />
        <xs:attribute name="name" type="xs:string" use="required" />
        <xs:attribute name="description" type="xs:string" use="optional" />
        <xs:attribute name="success" type="xs:string" use="optional" />
    <xs:attribute name="time" type="xs:string" use="optional" />
        <xs:attribute name="executed" type="xs:string" use="required" />
        <xs:attribute name="asserts" type="xs:string" use="optional" />
    <xs:attribute name="result" type="xs:string" use="required" />
  </xs:complexType>
</xs:schema>
 */