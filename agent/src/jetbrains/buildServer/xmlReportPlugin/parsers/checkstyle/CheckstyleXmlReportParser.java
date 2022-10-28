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

package jetbrains.buildServer.xmlReportPlugin.parsers.checkstyle;

import java.util.List;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 16:44
 */
class CheckstyleXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public CheckstyleXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return new ORHandler(elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(
          elementsPath(new Handler() {
            public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
              final String file = reader.getAttribute("name");

              return reader.visitChildren(elementsPath(new Handler() {
                public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                  myCallback.reportInspectionType(
                    new InspectionTypeResult(reader.getAttribute("source"), reader.getAttribute("source"),
                                           "From " + reader.getAttribute("source"), reader.getAttribute("severity")));

                  myCallback.reportInspection(
                    new InspectionResult(file, reader.getAttribute("source"), reader.getAttribute("message"),
                                       getInt(reader.getAttribute("line")),
                                       getPriority(reader.getAttribute("severity"))));

                  return reader.noDeep();
                }
              }, "error"));
            }
          }, "file"),

          elementsPath(new TextHandler() {
            public void setText(@NotNull final String text) {
              myCallback.reportException(text.trim());
            }
          }, "exception"));
      }
    }, "checkstyle")) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.error("Unexpected report format: \"checkstyle\" root element not present. Please see checkstyle sources XMLLogger.java for the supported format");
      }
    }.asList();
  }

  private int getPriority(@Nullable String severity) {
    if ("error".equals(severity)) {
      return 1;
    } else if ("warning".equals(severity)) {
      return 2;
    }
    return 3;
  }

  private static int getInt(@Nullable String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public interface Callback {
    void reportInspection(@NotNull InspectionResult inspection);
    void reportInspectionType(@NotNull InspectionTypeResult inspectionType);
    void reportException(@NotNull String message);
    void error(@NotNull String message);
  }
}
