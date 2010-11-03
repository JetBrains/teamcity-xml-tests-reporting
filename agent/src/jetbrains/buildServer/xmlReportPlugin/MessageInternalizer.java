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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import jetbrains.buildServer.agent.impl.BuildMessageTweaker;
import jetbrains.buildServer.messages.BuildMessage1;
import jetbrains.buildServer.messages.DefaultMessagesInfo;

class MessageInternalizer implements BuildMessageTweaker {
  public static final MessageInternalizer MESSAGE_INTERNALIZER = new MessageInternalizer();

  public void tweak(final BuildMessage1 message) {
    final Collection<String> tags = message.getTags();
    if(tags == null || tags.isEmpty())
      message.updateTags(Arrays.asList(DefaultMessagesInfo.TAG_INTERNAL));
    else if(!tags.contains(DefaultMessagesInfo.TAG_INTERNAL)) {
      final ArrayList<String> newTags = new ArrayList<String>(tags.size()+1);
      newTags.addAll(tags);
      newTags.add(DefaultMessagesInfo.TAG_INTERNAL);
      message.updateTags(newTags);
    }
  }
}
