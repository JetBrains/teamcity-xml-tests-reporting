/*
 * Copyright 2008 JetBrains s.r.o.
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
package jetbrains.buildServer.testReportParserPlugin;

import jetbrains.buildServer.agent.BaseServerLoggerFacade;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import static jetbrains.buildServer.testReportParserPlugin.TestUtil.WORKING_DIR;
import jetbrains.buildServer.testReportParserPlugin.findBugs.FindBugsReportParser;
import junit.framework.TestCase;
import org.jdom.input.JDOMParseException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileNotFoundException;


@RunWith(JMock.class)
public class FindBugsReportParserTest extends TestCase {
  private static final String REPORT_DIR = "Tests/testData/findbugs/";

  private TestReportParser myParser;
  private BaseServerLoggerFacade myLogger;
  private InspectionReporter myInspectionReporter;

  private Mockery myContext;
  private Sequence mySequence;

  private BaseServerLoggerFacade createBaseServerLoggerFacade() {
    return myContext.mock(BaseServerLoggerFacade.class);
  }

  private InspectionReporter createInspectionReporter() {
    final InspectionReporter reporter = myContext.mock(InspectionReporter.class);
    myContext.checking(new Expectations() {
      {
        oneOf(reporter).markBuildAsInspectionsBuild();
        oneOf(reporter).flush();
      }
    });
    return reporter;
  }

  private File report(String name) {
    return new File(REPORT_DIR + name);
  }

  @Before
  public void setUp() {
    myContext = new JUnit4Mockery() {
      {
        setImposteriser(ClassImposteriser.INSTANCE);
      }
    };
    myLogger = createBaseServerLoggerFacade();
    myInspectionReporter = createInspectionReporter();
    myParser = new FindBugsReportParser(new TestReportLogger(myLogger, true), myInspectionReporter, WORKING_DIR);
    mySequence = myContext.sequence("Log Sequence");
//    myContext.checking(new Expectations() {
//      {
//        oneOf(myLogger).message(with("Start processing FindBugs report"));
//        inSequence(mySequence);
//      }
//    });
  }

  @Test
  public void testUnexistingReport() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).exception(with(any(FileNotFoundException.class)));
        inSequence(mySequence);
      }
    });

    myParser.parse(new File("unexisting"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testEmptyReport() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).exception(with(any(JDOMParseException.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("empty.xml"), 0);
    myContext.assertIsSatisfied();
  }

  @Test
  public void testWrongFormatReport() {
    myContext.checking(new Expectations() {
      {
        oneOf(myLogger).exception(with(any(JDOMParseException.class)));
        inSequence(mySequence);
      }
    });
    myParser.parse(report("wrongFormat"), 0);
    myContext.assertIsSatisfied();
  }

//  @Test
//  public void testMinimumXml() {
//    myContext.checking(new Expectations() {
//      {
//        oneOf(myLogger).message(with("Version: 1.1.1"));
//        oneOf(myLogger).message(with("Sequence: 0"));
//        oneOf(myLogger).message(with("Timestamp: 1"));
//        oneOf(myLogger).message(with("Analysis timestamp: 2"));
//        oneOf(myLogger).message(with("Release: "));
//        oneOf(myLogger).message(with("[Summary]"));
//        oneOf(myLogger).message(with("Total classes: 0"));
//        oneOf(myLogger).message(with("Total bugs: 0"));
//        oneOf(myLogger).message(with("Total size: 0"));
//        oneOf(myLogger).message(with("Packages number: 0"));
//        oneOf(myLogger).message(with("Timestamp: Sun, 14 Oct 2007 14:23:30 -0400"));
//        inSequence(mySequence);
//      }
//    });
//    myParser.parse(report("miminum.xml"), 0);
//    myContext.assertIsSatisfied();
//  }
//
//  @Test
//  public void testIllegalXml() {
//    myContext.checking(new Expectations() {
//      {
//        oneOf(myLogger).message(with("Version: 1.1.1"));
//        oneOf(myLogger).message(with("Sequence: 0"));
//        oneOf(myLogger).message(with("Timestamp: 1"));
//        oneOf(myLogger).message(with("Analysis timestamp: 2"));
//        oneOf(myLogger).message(with("Release: "));
//        oneOf(myLogger).exception(with(any(NullPointerException.class)));
//        inSequence(mySequence);
//      }
//    });
//    myParser.parse(report("illegalXml.xml"), 0);
//    myContext.assertIsSatisfied();
//  }
}