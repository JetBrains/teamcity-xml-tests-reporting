package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Map;

/**
 * User: vbedrosova
 * Date: 24.01.11
 * Time: 13:54
 */
public interface RulesProcessor {
  void processRules(@NotNull File rulesFile,
                    @NotNull Map<String, String> params);
}
