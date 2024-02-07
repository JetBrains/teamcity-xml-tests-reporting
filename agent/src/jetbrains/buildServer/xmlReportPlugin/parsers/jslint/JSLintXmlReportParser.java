

package jetbrains.buildServer.xmlReportPlugin.parsers.jslint;

import java.util.List;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionResult;
import jetbrains.buildServer.xmlReportPlugin.inspections.InspectionTypeResult;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 06.05.11
 * Time: 18:35
 */
class JSLintXmlReportParser extends BaseXmlXppAbstractParser {
  private static final String INSPECTION_ID = "JSLint";
  private static final InspectionTypeResult INSPECTION_TYPE = new InspectionTypeResult(INSPECTION_ID, INSPECTION_ID, INSPECTION_ID, INSPECTION_ID);

  @NotNull
  private final Callback myCallback;

  public JSLintXmlReportParser(@NotNull Callback callback) {
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
                myCallback.reportInspectionType(INSPECTION_TYPE);

                myCallback.reportInspection(
                  new InspectionResult(file, INSPECTION_ID, getMessage(reader.getAttribute("reason"), reader.getAttribute("evidence")),
                                       getInt(reader.getAttribute("line")), 2));

                return reader.noDeep();
              }
            }, "issue"));
          }
        }, "file"));
      }
    }, "jslint")) {
      @Override
      protected void finished(final boolean matched) {
        if (matched) {
          myCallback.markBuildAsInspectionsBuild();
        } else {
          myCallback.error("Unexpected report format: \"jslint\" root element missing. Please see JSLint sources for the supported format");
        }
      }
    }.asList();
  }

  @Nullable
  private static String getMessage(@Nullable String reason, @Nullable String evidence) {
    final boolean hasReason = reason != null && reason.length() > 0;
    final boolean hasEvidence = evidence != null && evidence.length() > 0;
    if (hasReason && hasEvidence) {
      return "REASON: " + removeTrailingDot(reason) + ", EVIDENCE: " + evidence;
    }
    if (hasReason) return reason;
    if (hasEvidence) return evidence;

    return null;
  }

  private static String removeTrailingDot(final String reason) {
    return reason.endsWith(".") ? reason.substring(0, reason.length() - 1) : reason;
  }

  private static int getInt(@Nullable String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public interface Callback {
    void markBuildAsInspectionsBuild();
    void reportInspection(@NotNull InspectionResult inspection);
    void reportInspectionType(@NotNull InspectionTypeResult inspectionType);
    void error(@NotNull String message);
  }
}