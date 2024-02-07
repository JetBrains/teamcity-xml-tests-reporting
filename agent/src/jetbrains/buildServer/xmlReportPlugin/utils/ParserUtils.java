

package jetbrains.buildServer.xmlReportPlugin.utils;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.XmlUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * User: vbedrosova
 * Date: 22.01.11
 * Time: 15:23
 */
public class ParserUtils {

  private static final Pattern HTML_SPACE = Pattern.compile("&nbsp;", Pattern.LITERAL);
  private static final Pattern CARRIAGE_RETURN = Pattern.compile("\r", Pattern.LITERAL);
  private static final Pattern NEW_LINE = Pattern.compile("\n", Pattern.LITERAL);
  private static final Pattern SPACES = Pattern.compile("\\s+");
  private static final Pattern HTML_TAGS = Pattern.compile("<[a-z]>|</[a-z]>");

  @NotNull
  public static XMLReader createXmlReader(@NotNull ContentHandler contentHandler,
                                          @NotNull ErrorHandler errorHandler,
                                          boolean validate) throws SAXException {
    final XMLReader xmlReader = XmlUtil.createXMLReader(validate);

    xmlReader.setContentHandler(contentHandler);
    xmlReader.setErrorHandler(errorHandler);
    return xmlReader;
  }

  @NotNull
  public static String formatText(@NotNull String s) {
    s = replaceHtmlSpace(s);
    s = removeCarriageReturn(s);
    s = replaceNewLine(s);
    s = trimSpaces(s);
    s = removeHtmlTags(s);
    return s.trim();
  }

  private static String replaceHtmlSpace(@NotNull String s) {
    return HTML_SPACE.matcher(s).replaceAll(Matcher.quoteReplacement(" "));
  }

  private static String removeCarriageReturn(@NotNull String s) {
    return CARRIAGE_RETURN.matcher(s).replaceAll(Matcher.quoteReplacement(""));
  }

  private static String replaceNewLine(@NotNull String s) {
    return NEW_LINE.matcher(s).replaceAll(Matcher.quoteReplacement(" "));
  }

  private static String trimSpaces(@NotNull String s) {
    return SPACES.matcher(s).replaceAll(" ");
  }

  private static String removeHtmlTags(@NotNull String s) {
    return HTML_TAGS.matcher(s).replaceAll("");
  }

  public static boolean isReportComplete(@NotNull final File report, @Nullable String rootTag) {
    // here we pre-parse the report to check it's complete
    final CompleteReportHandler handler = new CompleteReportHandler(rootTag);
    try {
      final XMLReader reader = createXmlReader(handler, handler, false);
      reader.parse(new InputSource(report.toURI().toString()));
      return handler.isReportComplete();
    } catch (SAXParseException e) {
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  @Contract("null -> false")
  public static boolean isNumber(@Nullable final String str) {
    if(StringUtil.isEmptyOrSpaces(str)) return false;
    int position = 0;
    char ch = str.charAt(position);
    if (ch == '+' || ch == '-') position++;
    if (position >= str.length()) return false;

    while (position < str.length()) {
      if (!Character.isDigit(str.charAt(position++))) return false;
    }
    return true;
  }

  private static final class CompleteReportHandler extends DefaultHandler {
    private String myRootTag;
    private int myDepth = 0;
    private boolean myRightStart = false;
    private boolean myRightEnd = false;

    private CompleteReportHandler(@Nullable String rootTag) {
      myRootTag = rootTag;
    }

    public boolean isReportComplete() {
      return myRightStart && myRightEnd && myDepth == 0;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) {
      if (myDepth == 0) {
        if (myRootTag == null) myRootTag = localName;
        if (myRootTag.equals(localName)) myRightStart = true;
      }
      myDepth++;
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      myDepth--;
      if (myDepth == 0 && myRootTag != null && myRootTag.equals(localName)) myRightEnd = true;
    }
  }
}