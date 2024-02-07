

package jetbrains.buildServer.xmlReportPlugin.parsers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.util.XmlXppAbstractParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author vbedrosova
 */
public abstract class BaseXmlXppAbstractParser extends XmlXppAbstractParser {

  protected abstract class ORHandler implements CloseableHandler, XmlHandler {
    private final List<XmlHandler> myDelegates;
    private boolean myMatched = false;

    public ORHandler(XmlHandler... delegates) {
      myDelegates = Arrays.asList(delegates);
    }

    @Override
    public XmlReturn processElement(@NotNull final XmlElementInfo reader) {
      final XmlHandler handler = findHandler(reader.getLocalName());
      if (handler == null) return reader.noDeep();
      myMatched = true;
      return handler.processElement(reader);
    }

    @Override
    public boolean accepts(@NotNull final String name) {
      return findHandler(name) != null;
    }

    @Override
    public void close() {
      finished(myMatched);
    }

    @Nullable
    private XmlHandler findHandler(@NotNull final String name) {
      for (XmlHandler handler : myDelegates) {
        if (handler.accepts(name)) {
          return handler;
        }
      }
      return null;
    }

    protected abstract void finished(boolean matched);

    @NotNull
    public List<XmlHandler> asList() {
      return Collections.singletonList(this);
    }
  }

}