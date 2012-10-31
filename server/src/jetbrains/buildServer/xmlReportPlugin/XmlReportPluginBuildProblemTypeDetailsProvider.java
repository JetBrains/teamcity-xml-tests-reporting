package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * User: Victory.Bedrosova
 * Date: 10/31/12
 * Time: 12:53 PM
 */
public class XmlReportPluginBuildProblemTypeDetailsProvider extends BaseBuildProblemTypeDetailsProvider {
  @NotNull
  public String getType() {
    return XmlReportPluginConstants.BUILD_PROBLEM_TYPE;
  }

  @Override
  public String getStatusText(@NotNull final BuildProblemData buildProblem, @NotNull final SBuild build) {
    return StringUtil.EMPTY;
  }
}
