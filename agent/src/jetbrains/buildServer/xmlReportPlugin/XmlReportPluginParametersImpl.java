/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.duplicates.DuplicatesReporter;
import jetbrains.buildServer.agent.impl.MessageTweakingSupport;
import jetbrains.buildServer.agent.inspections.InspectionReporter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

import static jetbrains.buildServer.xmlReportPlugin.XmlReportPluginUtil.*;

/**
 * User: vbedrosova
 * Date: 15.11.10
 * Time: 19:51
 */
public class XmlReportPluginParametersImpl implements XmlReportPluginParameters {
  @NotNull
  private final Map<String, String> myParameters = new HashMap<String, String>();
  @NotNull
  private final Map<File, PathParametersImpl> myPathParameters = new HashMap<File, PathParametersImpl>();
  @NotNull
  private final Map<String, XmlReportPluginRules> myPaths = new HashMap<String, XmlReportPluginRules>();

  @NotNull
  private final BuildProgressLogger myLogger;
  @NotNull
  private final InspectionReporter myInspectionReporter;
  @NotNull
  private final DuplicatesReporter myDuplicatesReporter;

  @Nullable
  private ParametersListener myListener;

  protected XmlReportPluginParametersImpl(@NotNull BuildProgressLogger logger,
                                          @NotNull InspectionReporter inspectionReporter,
                                          @NotNull DuplicatesReporter duplicatesReporter) {
    myLogger = logger;
    myInspectionReporter = inspectionReporter;
    myDuplicatesReporter = duplicatesReporter;
  }

  public synchronized void updateParameters(@NotNull final Set<File> paths,
                                            @NotNull final Map<String, String> parameters) {
    final String type = getReportType(parameters);

    myParameters.putAll(parameters);

    if (!myPaths.containsKey(type)) {
      if (isInspection(type) && hasInspections()) {
        getThreadLogger().warning("Only one report of Code Inspection type is supported per build, skipping " + SUPPORTED_REPORT_TYPES.get(type) + " reports");
        getListener().pathsSkipped(type, paths);
        return;
      }
      myPaths.put(type, new XmlReportPluginRules(getPaths(paths), getCheckoutDir()));
    } else {
      final Set<String> updatedPaths = new HashSet<String>(myPaths.get(type).getBody());
      updatedPaths.addAll(getPaths(paths));
      myPaths.put(type, new XmlReportPluginRules(updatedPaths, getCheckoutDir()));
    }

    final boolean parseOutOfDate = isParseOutOfDateReports(parameters);
    final String whenNoDataPublished = whenNoDataPublished(parameters);
    final boolean logAsInternal = isLogIsInternal(parameters);
    final boolean verbose = isOutputVerbose(parameters);

    final PathParametersImpl pathParameters = new PathParametersImpl(parseOutOfDate, PathParameters.LogAction.getAction(whenNoDataPublished), logAsInternal, verbose);

    for (final File path : myPaths.get(type).getRootIncludePaths()) {
      myPathParameters.put(path, pathParameters);
    }

    getListener().pathsAdded(type, myPaths.get(type).getRootIncludePaths());
  }

  @NotNull
  private static Set<String> getPaths(@NotNull final Set<File> paths) {
    final Set<String> pathsStr = new HashSet<String>(paths.size());
    for (final File path : paths) {
      pathsStr.add(path.getPath());
    }
    return pathsStr;
  }

  @SuppressWarnings({"NullableProblems"})
  public void setListener(@NotNull ParametersListener listener) {
    myListener = listener;
  }

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  private ParametersListener getListener() {
    if (myListener == null) {
      throw new RuntimeException("No listener specified for xml-report-plugin parameters");
    }
    return myListener;
  }

  private synchronized boolean hasInspections() {
    for (String type : myPaths.keySet()) {
      if (isInspection(type)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  public synchronized Collection<String> getTypes() {
    return Collections.unmodifiableSet(myPaths.keySet());
  }

  @NotNull
  public synchronized Collection<File> getPaths(@NotNull String type) {
    return Collections.unmodifiableSet(myPaths.get(type).getRootIncludePaths());
  }

  @NotNull
  public synchronized XmlReportPluginRules getRules(@NotNull String type) {
    return myPaths.get(type);
  }

  public synchronized boolean isVerbose() {
    return isOutputVerbose(myParameters);
  }

  @NotNull
  public synchronized String getCheckoutDir() {
    return getCheckoutDirPath(myParameters);
  }

  @Nullable
  public synchronized String getFindBugsHome() {
    return getFindBugsHomePath(myParameters);
  }

  public synchronized long getBuildStartTime() {
    return getBuildStart(myParameters);
  }

  @NotNull
  public synchronized String getTmpDir() {
    return getTempFolder(myParameters);
  }

  @NotNull
  public synchronized String getNUnitSchema() {
    return getNUnitSchemaPath(myParameters);
  }

  public synchronized boolean checkReportComplete() {
    return isCheckReportComplete(myParameters);
  }

  public synchronized boolean checkReportGrows() {
    return isCheckReportGrows(myParameters);
  }

  @NotNull
  public synchronized Map<String, String> getRunnerParameters() {
    return Collections.unmodifiableMap(myParameters);
  }

  @NotNull
  public InspectionReporter getInspectionReporter() {
    return myInspectionReporter;
  }

  @NotNull
  public DuplicatesReporter getDuplicatesReporter() {
    return myDuplicatesReporter;
  }

  @NotNull
  public FlowLogger getThreadLogger() {
    return myLogger.getThreadLogger();
  }

  @NotNull
  public synchronized PathParameters getPathParameters(@NotNull File path) {
    if (!myPathParameters.containsKey(path)) {
      throw new IllegalStateException("Path is unknown");
    }
    return myPathParameters.get(path);
  }

  protected class PathParametersImpl implements PathParameters {
    private final boolean myParseOutOfDate;
    @NotNull
    private final LogAction myWhenNoDataPublished;
    private final boolean myLogAsInternal;
    private final boolean myVerbose;

    private PathParametersImpl(boolean parseOutOfDate,
                               @NotNull LogAction whenNoDataPublished,
                               boolean logAsInternal,
                               boolean verbose) {
      myParseOutOfDate = parseOutOfDate;
      myWhenNoDataPublished = whenNoDataPublished;
      myLogAsInternal = logAsInternal;
      myVerbose = verbose;
    }

    public boolean isParseOutOfDate() {
      return myParseOutOfDate;
    }

    @NotNull
    public LogAction getWhenNoDataPublished() {
      return myWhenNoDataPublished;
    }

    public boolean isVerbose() {
      return myVerbose;
    }

    @NotNull
    public BuildProgressLogger getPathLogger() {
      return myLogAsInternal ?
        ((MessageTweakingSupport) getThreadLogger()).getTweakedLogger(MessageInternalizer.MESSAGE_INTERNALIZER)
        : getThreadLogger();
    }
  }
}
