/*
 * Copyright 2000-2019 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.pmdCpd;

import java.util.List;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicatingFragment;
import jetbrains.buildServer.xmlReportPlugin.duplicates.DuplicationResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.utils.PathUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 07.02.11
 * Time: 19:04
 */
class PmdCpdXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;
  private final String myRootPath;

  public PmdCpdXmlReportParser(@NotNull Callback callback, @NotNull String rootPath) {
    myCallback = callback;
    myRootPath = rootPath;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return new ORHandler(elementsPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          myCallback.startDuplicates();

          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                final DuplicationResult duplicationResult
                  = new DuplicationResult(getInt(reader.getAttribute("lines")), getInt(reader.getAttribute("tokens")));

                return reader.visitChildren(
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull String s) {
                      duplicationResult.setHash(s.trim().hashCode());
                    }
                  }, "codefragment"),

                  elementsPath(new Handler() {
                    public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                      duplicationResult.addFragment(new DuplicatingFragment(getRelativePath(reader.getAttribute("path")), getInt(reader.getAttribute("line"))));
                      return reader.noDeep();
                    }
                  }, "file")
                ).than(new XmlAction() {
                  public void apply() {
                    duplicationResult.setFragmentHashes();
                    myCallback.reportDuplicate(duplicationResult);
                  }
                });
              }
            }, "duplication")
          ).than(new XmlAction() {
            public void apply() {
              myCallback.finishDuplicates();
            }
          });
        }
      },
      "pmd-cpd")) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.error("Unexpected report format: root \"pmd-cpd\" element not present. Please check PMD CPD sources for the supported XML Schema");
      }
    }.asList();
  }

  public static interface Callback {
    void startDuplicates();

    void finishDuplicates();

    void reportDuplicate(@NotNull DuplicationResult duplicate);

    void error(@NotNull String message);
  }

  @NotNull
  private String getRelativePath(final String path) {
    return PathUtils.getRelativePath(myRootPath, path);
  }

  private static int getInt(@Nullable String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
