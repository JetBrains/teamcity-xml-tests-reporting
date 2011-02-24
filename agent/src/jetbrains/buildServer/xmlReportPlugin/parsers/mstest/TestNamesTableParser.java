package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 14:21:37
 */
class TestNamesTableParser extends XmlXppAbstractParser {
  private final Callback myParserCallback;

  public TestNamesTableParser(@NotNull final Callback parserCallback) {
    myParserCallback = parserCallback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(getRootHandler9(), getRootHandler8());
  }

  protected XmlHandler getRootHandler8() {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final String[] id = {null};
        final String[] name = {null, null};

        return reader.visitChildren(
            elementsPath(new TextHandler() {
              public void setText(@NotNull final String text) {
                id[0] = text;
              }
            }, "key", "id"),
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                return reader.visitChildren(
                    elementsPath(new TextHandler() {
                      public void setText(@NotNull final String text) {
                        name[0] = text;
                      }
                    }, "className"),
                    elementsPath(new TextHandler() {
                      public void setText(@NotNull final String text) {
                        name[1] = text;
                      }
                    }, "name")).than(new XmlAction() {
                  public void apply() {
                    if (id[0] != null && name[0] != null && name[1] != null) {
                      String testName = NameUtil.getTestName(name[0], name[1]);
                      if (testName != null) {
                        myParserCallback.testMethodFound(id[0], testName);
                      }
                    }
                    id[0] = null;
                    name[0] = null;
                    name[1] = null;
                  }
                });
              }
            }, "value", "testMethod"));
      }
    }, "Tests", "TestRun", "tests");
  }

  protected XmlHandler getRootHandler9() {
    return elementsPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          return reader.visitChildren(
              getUnitTest2008Handler(),
              getUnknownTest2008Handler(".*Test")
            );
        }
      }
      , "TestRun", "TestDefinitions");
  }

  private XmlHandler getUnitTest2008Handler() {
    return elementsPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String id = reader.getAttribute("id");
          if (id == null) return reader.noDeep();
          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                //<TestMethod codeBase="..test/tests.lib9.dll"
                //            adapterTypeName="Microsoft.VisualStudio.TestTools.TestTypes.Unit.UnitTestAdapter, Microsoft.VisualStudio.QualityTools.Tips.UnitTest.Adapter"
                //            className="Test4.DerivedClass, tests.lib9, Version=0.0.0.0, Culture=neutral, PublicKeyToken=null"
                //            name="testMethod" />

                final String testName = NameUtil.getTestName(
                  reader.getAttribute("className"),
                  reader.getAttribute("name"));

                if (testName != null) {
                  myParserCallback.testMethodFound(id, testName);
                }
                return reader.noDeep();
              }
          }, "TestMethod"));
        }
      }, "UnitTest"
    );
  }

  private XmlHandler getUnknownTest2008Handler(final String pattern) {
    return elementsPatternPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          final String id = reader.getAttribute("id");
          final String name = reader.getAttribute("name");

          if (id != null && name != null) {
            myParserCallback.testMethodFound(id, name);
          }
          return reader.noDeep();
        }
      }, pattern);
  }

  public static interface Callback {
    void testMethodFound(@NotNull String id, @NotNull String testName);
  }
}

