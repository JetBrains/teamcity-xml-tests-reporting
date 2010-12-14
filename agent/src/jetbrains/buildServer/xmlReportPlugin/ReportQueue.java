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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * User: vbedrosova
 * Date: 14.12.10
 * Time: 15:45
 */
public class ReportQueue {
  private static final long TIMEOUT = 500;

  @NotNull
  private final LinkedBlockingQueue<ReportData> myQueue = new LinkedBlockingQueue<ReportData>();

  public void put(@NotNull final ReportData reportData) {
    try {
      myQueue.put(reportData);
    } catch (InterruptedException e) {
    }
  }

  public ReportData poll(boolean performWaiting) {
    try {
      return performWaiting ? myQueue.poll(TIMEOUT, TimeUnit.MILLISECONDS) : myQueue.poll();
    } catch (InterruptedException e) {
      return null;
    }
  }

  public boolean isEmpty() {
    return myQueue.isEmpty();
  }
}
