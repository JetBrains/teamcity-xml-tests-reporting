/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 20:28
 */
class CategoryXmlParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public CategoryXmlParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPatternPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(elementsPath(new Handler() {
          public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
            final String category = reader.getAttribute("category");

            if (category == null) return reader.noDeep();

            myCallback.categoryFound(category);

            return reader.visitChildren(
              elementsPath(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.categoryDescriptionFound(category, ParserUtils.formatText(text));
                }
              }, "Description"),

              elementsPath(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.categoryDetailsFound(category, ParserUtils.formatText(text));
                }
              }, "Details")
            );
          }
        }, "BugCategory"));
      }
    }, ".*Collection"));
  }

  public static interface Callback {
    void categoryFound(@NotNull String category);
    void categoryDescriptionFound(@NotNull String category, @NotNull String description);
    void categoryDetailsFound(@NotNull String category, @NotNull String details);
  }
}
