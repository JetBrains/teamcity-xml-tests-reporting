/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

          final String[] nameParts = new String[3]; // classname, name, Description

          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                //<TestMethod codeBase="..test/tests.lib9.dll"
                //            adapterTypeName="Microsoft.VisualStudio.TestTools.TestTypes.Unit.UnitTestAdapter, Microsoft.VisualStudio.QualityTools.Tips.UnitTest.Adapter"
                //            className="Test4.DerivedClass, tests.lib9, Version=0.0.0.0, Culture=neutral, PublicKeyToken=null"
                //            name="testMethod" />
                nameParts[0] = reader.getAttribute("className");
                nameParts[1] = reader.getAttribute("name");
                return reader.noDeep();
              }
            }, "TestMethod"),
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                // <Description>warn on high std dev</Description>
                return reader.visitText(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    nameParts[2] = text;
                  }
                });
              }
            }, "Description")).than(
            new XmlAction() {
              public void apply() {
                final String testName = getTestName(nameParts);

                if (testName != null) {
                  myParserCallback.testMethodFound(id, testName);
                }
              }
            });
        }
      }, "UnitTest"
    );
  }

  @Nullable
  private String getTestName(@NotNull String[] nameParts) {
    final String testName = NameUtil.getTestName(nameParts[0], nameParts[1]);
    final String testDescription = nameParts[2];

    if (StringUtil.isNotEmpty(testName) && StringUtil.isNotEmpty(testDescription)) return testName + " (" + testDescription + ")";
    if (StringUtil.isNotEmpty(testName)) return testName;
    if (StringUtil.isNotEmpty(testDescription)) return testDescription;

    return null;
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

