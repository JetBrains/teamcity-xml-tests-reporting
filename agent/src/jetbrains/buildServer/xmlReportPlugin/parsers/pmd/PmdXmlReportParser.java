

package jetbrains.buildServer.xmlReportPlugin.parsers.pmd;

import java.util.List;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 13:29
 */
class PmdXmlReportParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public PmdXmlReportParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlHandler> getRootHandlers() {
    return new ORHandler(elementsPath(new Handler() {
      public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
        return reader.visitChildren(elementsPath(new Handler() {
          public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
            final String file = reader.getAttribute("name");

            return reader.visitChildren(elementsPath(new Handler() {
              public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                final String rule = reader.getAttribute("rule");
                final String ruleset = reader.getAttribute("ruleset");
                // 'beginline' and 'priority' MUST be evaluated here (before visitText)
                // because reader may be changed later (when reading text, underlying buffer could be changed)
                final int beginline = getInt(reader.getAttribute("beginline"));
                final int priority = getInt(reader.getAttribute("priority"));

                myCallback.reportInspectionType(new InspectionTypeResult(rule, rule, ruleset, ruleset));

                return reader.visitText(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    myCallback.reportInspection(new InspectionResult(file, rule, text.trim(), beginline, priority));
                  }
                });
              }
            }, "violation"));
          }
        }, "file"));
      }
    }, "pmd")) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.error("Unexpected report format: \"pmd\" root element missing. Please check PMD sources for the supported XML Schema");
      }
    }.asList();
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
    void error(@NotNull String message);
  }
}