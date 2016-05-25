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

package jetbrains.buildServer.xmlReportPlugin.parsers.nUnit;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.tests.SecondDurationParser;
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
  @NotNull
  private final SecondDurationParser myDurationParser;

  public NUnitXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
    myDurationParser = new SecondDurationParser();
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(suiteHandler(true));
      }
    }, "test-results"));
  }

  @NotNull
  private XmlHandler suiteHandler(final boolean addLogging) {
    return elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        final String name = getSuiteName(reader.getAttribute("name"));

        if (addLogging) myCallback.suiteFound(name);

        return reader.visitChildren(
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              return reader.visitChildren(suiteHandler(false), testHandler());
            }
          }, "results")
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
        final String result = reader.getAttribute("result");
        final boolean executed = Boolean.parseBoolean(reader.getAttribute("executed"));
        testData.setSuccess("Success".equals(result) || (executed && Boolean.parseBoolean(reader.getAttribute("success"))));
        testData.setIgnored(!executed || "Inconclusive".equals(result));
        testData.setDuration(myDurationParser.parseTestDuration(reader.getAttribute("time")));

        return reader.visitChildren(
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              testData.setSuccess(false);
              return reader.visitChildren(
                getMessageHandler(testData),
                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    testData.setFailureStackTrace(text.trim());
                  }
                }, "stack-trace")
              );
            }
          }, "failure"),
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              return reader.visitChildren(
                getMessageHandler(testData)
              );
            }
          }, "reason")
        ).than(new XmlAction() {
          public void apply() {
            myCallback.testFound(testData);
          }
        });
      }

      private XmlHandler getMessageHandler(final TestData testData) {
        return elementsPath(new TextHandler() {
          public void setText(@NotNull final String text) {
            testData.setMessage(text.trim());
          }
        }, "message");
      }
    }, "test-case");
  }

  @Nullable
  private String getSuiteName(@Nullable String name) {
    if (name == null) return null;
    if (name.contains("\\")) return name.substring(name.lastIndexOf("\\") + 1);
    if (name.contains("/")) return name.substring(name.lastIndexOf("/") + 1);
    return name;
  }

  public static interface Callback {
    void suiteFound(@Nullable String suiteName);

    void suiteFinished(@Nullable String suiteName);

    void testFound(@NotNull TestData testData);
  }
}