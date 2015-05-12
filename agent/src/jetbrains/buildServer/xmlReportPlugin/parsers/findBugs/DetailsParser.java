/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import java.io.Reader;
import javax.swing.text.html.parser.DTD;
import javax.swing.text.html.parser.Parser;
import javax.swing.text.html.parser.TagElement;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 21.02.11
 * Time: 9:30
 */
class DetailsParser extends Parser {
  @NotNull
  private final StringBuffer myStringBuffer = new StringBuffer();

  public DetailsParser(@NotNull DTD dtd) {
    super(dtd);
  }

  @Override
  public void parse(Reader in) throws java.io.IOException {
    myStringBuffer.delete(0, myStringBuffer.length());
    super.parse(in);
  }

  @Override
  protected void handleText(char text[]) {
    myStringBuffer.append(text);
  }

  @Override
  protected void handleEndTag(TagElement tag) {
    myStringBuffer.append(" ");
  }

  public String getText() {
    return myStringBuffer.toString().trim();
  }
}
