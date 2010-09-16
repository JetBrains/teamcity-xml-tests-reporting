package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.FlowLogger;
import jetbrains.buildServer.agent.FlowManager;
import jetbrains.buildServer.agent.FlowManagerImpl;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 06.09.2010
 * Time: 16:18:50
 */
public class FlowManagerFactory {
  public static final String RUNNING_TESTS = "running.tests";

  public static FlowManager createFlowManager(@NotNull final BuildProgressLogger logger) {
    if ("true".equalsIgnoreCase(System.getProperty(RUNNING_TESTS))) {
      return new FlowManager() {
        public String getFlowId() {
          return "";
        }

        public void buildFinished() {
        }

        public void logInFlow(String s, Runnable runnable) {
          runnable.run();
        }

        public void ensureFlow(FlowLogger flowLogger) {
        }

        public String generateNewFlow() {
          return "";
        }

        public void releaseFlow(String s) {
        }

        public void setFlowId(String s) {
        }
      };
    } else {
//      final FlowManager flowManager = LoggerFactory.createFlowManager(BuildProgressLogger.class.getClassLoader());
      return FlowManagerImpl.getInstance(); // it's a temporary solution
    }
  }
}
