

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
class PatternXmlParser extends XmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public PatternXmlParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return Arrays.asList(elementsPatternPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(elementsPath(new Handler() {
          public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
            final String type = reader.getAttribute("type");

            if (type == null) return reader.noDeep();

            myCallback.patternFound(type);

            return reader.visitChildren(
              elementsPath(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.patternShortDescriptionFound(type, ParserUtils.formatText(text));
                }
              }, "ShortDescription"),

              elementsPath(new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.patternDetailsFound(type, ParserUtils.formatText(text));
                }
              }, "Details")
            );
          }
        }, "BugPattern"));
      }
    }, ".*Collection"));
  }

  public interface Callback {
    void patternFound(@NotNull String type);
    void patternShortDescriptionFound(@NotNull String type, @NotNull String description);
    void patternDetailsFound(@NotNull String type, @NotNull String details);
  }
}