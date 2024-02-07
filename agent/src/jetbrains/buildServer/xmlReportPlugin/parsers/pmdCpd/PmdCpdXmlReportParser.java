

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

  public interface Callback {
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