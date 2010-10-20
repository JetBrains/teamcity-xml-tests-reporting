package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.duplicator.DuplicateInfo;
import jetbrains.buildServer.xmlReportPlugin.ReportData;
import jetbrains.buildServer.xmlReportPlugin.XmlReportParser;
import jetbrains.buildServer.xmlReportPlugin.XmlReportPlugin;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 27.08.2010
 * Time: 16:50:03
 */
public class PmdCpdReportParser extends XmlReportParser {
  public static final String TYPE = "pmdCpd";
  private static final String TRAILING_TAG = "</pmd-cpd>";

  private final DuplicatesReporter myDuplicatesReporter;

  private DuplicationInfo myCurrentDuplicate;

  public PmdCpdReportParser(@NotNull final BuildProgressLogger logger,
                            @NotNull final DuplicatesReporter duplicatesReporter) {
    super(logger);
    myDuplicatesReporter = duplicatesReporter;
    myCData = new StringBuilder();
  }

  @Override
  public void parse(@NotNull ReportData data) {
    final File report = data.getFile();
    if (!isReportComplete(report, TRAILING_TAG)) {
      XmlReportPlugin.LOG.debug("The report doesn't finish with " + TRAILING_TAG);
      data.setProcessedEvents(0);
      return;
    }
    try {
      parse(report);
    } catch (SAXParseException spe) {
      myLogger.error(report.getAbsolutePath() + " is not parsable by PMD CPD parser");
      XmlReportPlugin.LOG.error(spe);
    } catch (Exception e) {
      myLogger.exception(e);
    }
    data.setProcessedEvents(-1);
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if ("pmd-cpd".equals(localName)) {
      myDuplicatesReporter.startDuplicates();
    } else if ("duplication".equals(localName)) {
      myCurrentDuplicate = new DuplicationInfo(getInt(attributes.getValue("lines")), getInt(attributes.getValue("tokens")));
    } else if ("file".equals(localName)) {
      myCurrentDuplicate.addFragment(new FragmentInfo(unifySlashes(attributes.getValue("path")), getInt(attributes.getValue("line"))));
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("pmd-cpd".equals(localName)) {
      myDuplicatesReporter.finishDuplicates();
    } else if ("codefragment".equals(localName)) {
      myCurrentDuplicate.setHash(myCData.toString().trim().hashCode());
    } else if ("duplication".equals(localName)) {
      final List<FragmentInfo> fragmentsList = myCurrentDuplicate.getFragments();
      final DuplicateInfo.Fragment[] fragmentsArray = new DuplicateInfo.Fragment[fragmentsList.size()];

      for (int i = 0; i < fragmentsList.size(); ++i) {
        final FragmentInfo fragment = fragmentsList.get(i);
        fragmentsArray[i] = new DuplicateInfo.Fragment(myCurrentDuplicate.getHash(), fragment.getFile(), fragment.getStartLine(),
                                                       new DuplicateInfo.LineOffset(fragment.getStartLine(), fragment.getStartLine() + myCurrentDuplicate.getLines()));
      }

      myDuplicatesReporter.addDuplicate(new DuplicateInfo(myCurrentDuplicate.getHash(), myCurrentDuplicate.getTokens(), fragmentsArray));

      myCurrentDuplicate = null;
    }
    clearCData();
  }

  private static String unifySlashes(String s) {
    return s == null ? "" : s.replace('\\', '/');
  }

  private static int getInt(String val) {
    try {
      return val == null ? 0 : Integer.parseInt(val);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static final class FragmentInfo {
    private final String myFile;
    private final int myStartLine;

    private FragmentInfo(String file, int startLine) {
      myFile = file;
      myStartLine = startLine;
    }

    public String getFile() {
      return myFile;
    }

    public int getStartLine() {
      return myStartLine;
    }
  }

  private static final class DuplicationInfo {
    private final int myLines;
    private final int myTokens;
    private int myHash;

    private final List<FragmentInfo> myFragments = new ArrayList<FragmentInfo>();

    private DuplicationInfo(int lines, int tokens) {
      myLines = lines;
      myTokens = tokens;
    }

    public int getLines() {
      return myLines;
    }

    public int getTokens() {
      return myTokens;
    }

    public void addFragment(final FragmentInfo fragment) {
      myFragments.add(fragment);
    }

    public List<FragmentInfo> getFragments() {
      return myFragments;
    }

    public int getHash() {
      return myHash;
    }

    public void setHash(int hash) {
      myHash = hash;
    }
  }
}
