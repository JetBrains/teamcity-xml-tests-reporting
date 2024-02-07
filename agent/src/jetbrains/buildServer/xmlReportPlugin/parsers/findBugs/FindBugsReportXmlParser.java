

package jetbrains.buildServer.xmlReportPlugin.parsers.findBugs;

import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.parsers.BaseXmlXppAbstractParser;
import jetbrains.buildServer.xmlReportPlugin.utils.ParserUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 18.02.11
 * Time: 14:38
 */
class FindBugsReportXmlParser extends BaseXmlXppAbstractParser {
  @NotNull
  private final Callback myCallback;

  public FindBugsReportXmlParser(@NotNull Callback callback) {
    myCallback = callback;
  }

  @Override
  protected List<XmlXppAbstractParser.XmlHandler> getRootHandlers() {
    return new ORHandler(elementsPath(new XmlXppAbstractParser.Handler() {
      public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
        return reader.visitChildren(
          elementsPath(new XmlXppAbstractParser.Handler() {
            public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
              final TextHandler textHandler = new TextHandler() {
                public void setText(@NotNull final String text) {
                  myCallback.jarFound(ParserUtils.formatText(text));
                }
              };
              return reader.visitChildren(
                elementsPath(textHandler, "Jar"),
                elementsPath(textHandler, "SrcDir")
              );
            }
          }, "Project"),

          elementsPath(new XmlXppAbstractParser.Handler() {
            public XmlXppAbstractParser.XmlReturn processElement(@NotNull final XmlXppAbstractParser.XmlElementInfo reader) {
              final String type = reader.getAttribute("type");
              final String category = reader.getAttribute("category");
              final int priority = getInt(reader.getAttribute("priority"));

              final String[] clazz = new String[1];
              final String[] message = new String[1];
              final SourceLine sourceLine = new SourceLine();
              final StringBuilder details = new StringBuilder();

              final XmlHandler sourceLineHandler = elementsPath(new Handler() {
                public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                  sourceLine.update(reader.getAttribute("sourcepath"), getInt(reader.getAttribute("start")), reader.getAttribute("primary"));
                  return reader.noDeep();
                }
              }, "SourceLine");

              return reader.visitChildren(
                elementsPatternPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    if (clazz[0] == null) { // accept only first Class tag
                      clazz[0] = reader.getAttribute("classname");
                      return reader.visitChildren(sourceLineHandler);
                    }
                    return reader.noDeep();
                  }
                }, "Class"),

                elementsPatternPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    //noinspection ConstantConditions
                    if (reader.getAttribute("classname").equals(clazz[0])) {
                      details.append(" ").append(reader.getLocalName())
                             .append("[")
                             .append("name=\"").append(reader.getAttribute("name")).append("\" ")
                             .append("signature=\"").append(reader.getAttribute("signature")).append("\"")
                             .append("]");
                      return reader.visitChildren(sourceLineHandler);
                    }
                    return reader.noDeep();
                  }
                }, "(Method)|(Field)"),

                elementsPatternPath(new Handler() {
                  public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
                    details.append(" ").append("LocalVariable[name=\"").append(reader.getAttribute("name")).append("\"]");
                    return reader.noDeep();
                  }
                }, "LocalVariable"),

                sourceLineHandler,

                elementsPath(new TextHandler() {
                  public void setText(@NotNull final String text) {
                    if (message[0] == null) message[0] = ParserUtils.formatText(text);
                  }
                }, "ShortMessage", "LongMessage")
              ).than(new XmlAction() {
                public void apply() {
                  myCallback.bugInstanceFound(sourceLine.getFile(), clazz[0], sourceLine.getLine(), type, category, message[0], details.toString(), priority);
                }
              });
            }
          }, "BugInstance")
        );
      }
    }, "BugCollection")) {
      @Override
      protected void finished(final boolean matched) {
        if (!matched) myCallback.error("Unexpected report format: \"BugCollection\" root element not present. Please check FindBugs sources bugcollection.xsd for the supported schema");
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
    void jarFound(@NotNull String jar);
    void bugInstanceFound(@Nullable String file, @Nullable String clazz, int line,
                          @Nullable String type, @Nullable String category, @Nullable String message, @Nullable String details, int priority);
    void error(@NotNull String message);
  }

  private static class SourceLine {
    private boolean myPrimary = false;
    private String myFile;
    private int myLine;

    public void update(final String file, final int line, final String isPrimary) {
      if (checkAndSet(isPrimary)) {
        myFile = file;
        if (line > 0) myLine = line;
      }
    }

    public String getFile() {
      return myFile;
    }

    public int getLine() {
      return myLine;
    }

    private boolean checkAndSet(String isPrimary) {
      if (Boolean.parseBoolean(isPrimary)) {
        myPrimary = true;
        return true;
      }
      return !myPrimary;
    }
  }
}