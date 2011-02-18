/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.DuplicatesReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 07.02.11
 * Time: 19:04
 */
public class PmdCpdXmlReportParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public PmdCpdXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPath(
      new Handler() {
        public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
          myCallback.startDuplicates();

          return reader.visitChildren(
            elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                final DuplicatesReporter.DuplicationInfo duplicationInfo
                  = new DuplicatesReporter.DuplicationInfo(getInt(reader.getAttribute("lines")), getInt(reader.getAttribute("tokens")));

                return reader.visitChildren(
                  elementsPath(new TextHandler() {
                    public void setText(@NotNull String s) {
                      duplicationInfo.setHash(s.trim().hashCode());
                    }
                  }, "codefragment"),

                  elementsPath(new Handler() {
                    public XmlReturn processElement(@NotNull XmlElementInfo reader) {
                      duplicationInfo.addFragment(new DuplicatesReporter.FragmentInfo(reader.getAttribute("path"), getInt(reader.getAttribute("line"))));
                      return reader.noDeep();
                    }
                  }, "file")
                ).than(new XmlAction() {
                  public void apply() {
                    myCallback.reportDuplicate(duplicationInfo);
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
      "pmd-cpd"));
  }

  public static interface Callback {
    void startDuplicates();

    void finishDuplicates();

    void reportDuplicate(@NotNull DuplicatesReporter.DuplicationInfo duplicate);
  }

  private static int getInt(@Nullable String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
