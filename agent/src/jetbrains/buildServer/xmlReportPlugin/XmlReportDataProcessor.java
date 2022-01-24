/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.agent.BuildStepDataProcessor;
import jetbrains.buildServer.agent.DataProcessorContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//"##teamcity[importData type='sometype' file='somedir']"
// service message activates watching "somedir" directory for reports of sometype type
//"##teamcity[importData type='sometype' file='somedir' verbose='true']"
// does the same and sets output verbose
//"##teamcity[importData type='sometype' file='somedir' verbose='true' parseOutOfDate='true']"
// does the same and enables parsing out-of-date reports

//"##teamcity[importData type='pmd' file='somedir' errorLimit='100' warningLimit='200']"
//starts watching somedir directory for pmd reports, build will fail if some report has
//more than errorLimit errors or more than warningLimit warnings

//"##teamcity[importData type='findBugs' file='somedir' findBugsHome='somepath']"
//starts watching somedir directory for findBugs reports, findBugs report processor needs findBugsHome
//attribute

public abstract class XmlReportDataProcessor implements BuildStepDataProcessor {
  public static final String VERBOSE_ARGUMENT = "verbose";
  public static final String PARSE_OUT_OF_DATE_ARGUMENT = "parseOutOfDate";
  public static final String ERRORS_LIMIT_ARGUMENT = "errorLimit";
  public static final String WARNINGS_LIMIT_ARGUMENT = "warningLimit";
  public static final String FINDBUGS_HOME_ARGUMENT = "findBugsHome";
  public static final String FAIL_BUILD_IF_PARSING_FAILED = "failBuildIfParsingFailed";
  public static final String WHEN_NO_DATA_PUBLISHED_ARGUMENT = "whenNoDataPublished";
  public static final String LOG_AS_INTERNAL_ARGUMENT = "logAsInternal";

  @NotNull
  private final RulesProcessor myRulesProcessor;

  XmlReportDataProcessor(@NotNull RulesProcessor rulesProcessor) {
    myRulesProcessor = rulesProcessor;
  }

  @Override
  public void processData(@NotNull final DataProcessorContext context) {
    myRulesProcessor.processRules(context.getFile(), getParams(context.getArguments()));
  }

  @NotNull
  protected Map<String, String> getParams(@NotNull final Map<String, String> arguments) {
    final Map<String, String> params = new HashMap<String, String>();
    params.put(XmlReportPluginConstants.REPORT_TYPE, getType());
    params.put(XmlReportPluginConstants.VERBOSE_OUTPUT, getOrDefault(arguments, VERBOSE_ARGUMENT, "false"));
    params.put(XmlReportPluginConstants.PARSE_OUT_OF_DATE, getOrDefault(arguments, PARSE_OUT_OF_DATE_ARGUMENT, "false"));
    params.put(XmlReportPluginConstants.WHEN_NO_DATA_PUBLISHED, getOrDefault(arguments, WHEN_NO_DATA_PUBLISHED_ARGUMENT, "error"));
    params.put(XmlReportPluginConstants.FAIL_BUILD_IF_PARSING_FAILED, getOrDefault(arguments, FAIL_BUILD_IF_PARSING_FAILED, "true"));
    params.put(XmlReportPluginConstants.LOG_AS_INTERNAL, getOrDefault(arguments, LOG_AS_INTERNAL_ARGUMENT, null));
    params.put(XmlReportPluginConstants.MAX_ERRORS, getOrDefault(arguments, ERRORS_LIMIT_ARGUMENT, null));
    params.put(XmlReportPluginConstants.MAX_WARNINGS, getOrDefault(arguments, WARNINGS_LIMIT_ARGUMENT, null));
    return params;
  }

  @Nullable
  protected String getOrDefault(@NotNull final Map<String, String> source,
                           @NotNull final String sourceKey,
                           @Nullable final String defaultValue) {
    return source.containsKey(sourceKey) ? source.get(sourceKey) : defaultValue;
  }

  public static final class JUnitDataProcessor extends XmlReportDataProcessor {
    public JUnitDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "junit";
    }
  }

  public static final class TestNGDataProcessor extends XmlReportDataProcessor {
    public TestNGDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "testng";
    }
  }

  public static final class NUnitDataProcessor extends XmlReportDataProcessor {
    public NUnitDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "nunit";
    }
  }

  public static final class SurefireDataProcessor extends XmlReportDataProcessor {
    public SurefireDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "surefire";
    }
  }

  public static final class FindBugsDataProcessor extends XmlReportDataProcessor {
    public FindBugsDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "findBugs";
    }

    @NotNull
    @Override
    protected Map<String, String> getParams(@NotNull final Map<String, String> arguments) {
      final Map<String, String> params = super.getParams(arguments);
      params.put(XmlReportPluginConstants.FINDBUGS_HOME, getOrDefault(arguments, FINDBUGS_HOME_ARGUMENT, null));
      return params;
    }
  }

  public static final class PmdDataProcessor extends XmlReportDataProcessor {
    public PmdDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "pmd";
    }
  }

  public static final class CheckstyleDataProcessor extends XmlReportDataProcessor {
    public CheckstyleDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "checkstyle";
    }
  }

  public static final class PmdCpdDataProcessor extends XmlReportDataProcessor {
    public PmdCpdDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "pmdCpd";
    }
  }

  public static final class MSTestDataProcessor extends XmlReportDataProcessor {
    public MSTestDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "mstest";
    }
  }

  public static final class VSTestDataProcessor extends XmlReportDataProcessor {
    public VSTestDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "vstest";
    }
  }

  public static final class TRXDataProcessor extends XmlReportDataProcessor {
    public TRXDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "trx";
    }
  }

  public static final class GTestDataProcessor extends XmlReportDataProcessor {
    public GTestDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "gtest";
    }
  }

  public static final class JSLintDataProcessor extends XmlReportDataProcessor {
    public JSLintDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "jslint";
    }
  }

  public static final class CTestDataProcessor extends XmlReportDataProcessor {
    public CTestDataProcessor(RulesProcessor plugin) {
      super(plugin);
    }

    @NotNull
    @Override
    public String getType() {
      return "ctest";
    }
  }
}
