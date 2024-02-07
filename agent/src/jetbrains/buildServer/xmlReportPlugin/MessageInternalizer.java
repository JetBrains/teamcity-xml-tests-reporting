

package jetbrains.buildServer.xmlReportPlugin;

import jetbrains.buildServer.agent.impl.BuildMessageTweaker;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.messages.DefaultMessagesInfo;

class MessageInternalizer implements BuildMessageTweaker {
  public static final MessageInternalizer MESSAGE_INTERNALIZER = new MessageInternalizer();

  public void tweak(final BuildMessage1 message) {
    DefaultMessagesInfo.internalize(message);
  }
}