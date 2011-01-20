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

package jetbrains.buildServer.xmlReportPlugin;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: vbedrosova
 * Date: 14.12.10
 * Time: 15:45
 */
public class ReportQueue {
  @NotNull
  private final LinkedBlockingQueue<ReportContext> myQueue = new LinkedBlockingQueue<ReportContext>();

  public void put(@NotNull final ReportContext context) throws InterruptedException {
    myQueue.put(context);
  }

  public ReportContext poll() throws InterruptedException {
    return myQueue.poll();
  }

  public boolean isEmpty() {
    return myQueue.isEmpty();
  }
}
