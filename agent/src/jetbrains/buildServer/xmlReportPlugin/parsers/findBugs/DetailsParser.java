

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