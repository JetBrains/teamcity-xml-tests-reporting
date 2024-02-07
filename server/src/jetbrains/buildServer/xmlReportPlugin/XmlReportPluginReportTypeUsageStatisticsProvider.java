

package jetbrains.buildServer.xmlReportPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import jetbrains.buildServer.serverSide.SBuildFeatureDescriptor;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.usageStatistics.impl.providers.BaseBuildTypeBasedExtensionUsageStatisticsProvider;
import jetbrains.buildServer.usageStatistics.presentation.UsageStatisticsGroupPosition;
import jetbrains.buildServer.util.positioning.PositionAware;
import jetbrains.buildServer.util.positioning.PositionConstraint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author maxim.manuylov
 *         Date: 7/3/12
 */
public class XmlReportPluginReportTypeUsageStatisticsProvider extends BaseBuildTypeBasedExtensionUsageStatisticsProvider<String> implements PositionAware {
  public XmlReportPluginReportTypeUsageStatisticsProvider(@NotNull final SBuildServer server) {
    super(server);
  }

  @NotNull
  @Override
  protected PositionAware getGroupPosition() {
    return this;
  }

  @NotNull
  public String getOrderId() {
    return XmlReportPluginReportTypeUsageStatisticsProvider.class.getName();
  }

  @NotNull
  public PositionConstraint getConstraint() {
    return PositionConstraint.after(UsageStatisticsGroupPosition.INVESTIGATION_MUTE.getOrderId());
  }

  @NotNull
  @Override
  protected Collection<String> collectExtensions(@NotNull final SBuildType buildType) {
    final Collection<String> result = new ArrayList<String>();
    for (final SBuildFeatureDescriptor featureDescriptor : buildType.getBuildFeaturesOfType(XmlReportPluginBuildFeature.FEATURE_TYPE)) {
      final Map<String, String> params = featureDescriptor.getParameters();
      if (XmlReportPluginUtil.isParsingEnabled(params)) {
        result.add(XmlReportPluginUtil.getReportType(params));
      }
    }
    return result;
  }

  @NotNull
  @Override
  protected String getExtensionType(@NotNull final String reportType) {
    return reportType;
  }

  @Nullable
  @Override
  protected String getExtensionDisplayName(@NotNull final String reportType, @NotNull final String extensionType) {
    return XmlReportPluginUtil.SUPPORTED_REPORT_TYPES.get(reportType);
  }
}