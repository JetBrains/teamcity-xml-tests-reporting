/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.pmdCpd;

import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.duplicator.DuplicateInfo;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.xmlReportPlugin.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vbedrosova
 * Date: 27.08.2010
 * Time: 16:50:03
 */
public class PmdCpdReportParser extends XmlReportParser implements Parser {
  private static final char SEPARATOR = '/';

  private DuplicatesReporter myDuplicatesReporter;
  private String myCheckoutDirectory;

  private DuplicationInfo myCurrentDuplicate;

  public PmdCpdReportParser(@NotNull XMLReader reader,
                            @NotNull DuplicatesReporter duplicatesReporter,
                            @NotNull File checkoutDirectory) {
    super(reader, true);
    myDuplicatesReporter = duplicatesReporter;
    myCheckoutDirectory = unifySlashes(checkoutDirectory.getAbsolutePath());
  }

  public boolean parse(@NotNull File file, @Nullable ParsingResult prevResult) throws ParsingException {
    if (!ParserUtils.isReportComplete(file, "pmd-cpd")) {
      return false;
    }
    parse(file);
    return true;
  }

  public ParsingResult getParsingResult() {
    return new PmdCpdParsingResult();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    if ("pmd-cpd".equals(localName)) {
      myDuplicatesReporter.startDuplicates();
    } else if ("duplication".equals(localName)) {
      myCurrentDuplicate = new DuplicationInfo(getInt(attributes.getValue("lines")), getInt(attributes.getValue("tokens")));
    } else if ("file".equals(localName)) {
      myCurrentDuplicate.addFragment(new FragmentInfo(resolvePath(attributes.getValue("path")), getInt(attributes.getValue("line"))));
    }
  }

  private String resolvePath(String path) {
    if (path == null) return "";

    path = unifySlashes(path);

    String resolved = FileUtil.getRelativePath(myCheckoutDirectory, path, SEPARATOR);

    if (resolved == null) return path;

    if (resolved.startsWith("./")) {
      resolved = resolved.substring(2);
    }

    return resolved;
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if ("pmd-cpd".equals(localName)) {
      myDuplicatesReporter.finishDuplicates();
    } else if ("codefragment".equals(localName)) {
      myCurrentDuplicate.setHash(getCData().toString().trim().hashCode());
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
    return s == null ? "" : s.replace('\\', SEPARATOR);
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