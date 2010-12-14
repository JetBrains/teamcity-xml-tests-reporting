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

import jetbrains.buildServer.agent.FlowLogger;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * User: vbedrosova
 * Date: 14.12.10
 * Time: 0:32
 */
public abstract class XmlReportPluginActivity {
  @NotNull
  private final XmlReportPluginParameters myParameters;

  @NotNull
  private final ReportQueue myQueue;

  private volatile boolean myStopSignaled;

  @NotNull
  private final String myName;

  @Nullable
  private Thread myThread;
  @Nullable
  private FlowLogger myThreadLogger; // initialized on the thread start and dispose on the thread finish

  public XmlReportPluginActivity(@NotNull final String name,
                                 @NotNull final XmlReportPluginParameters parameters,
                                 @NotNull final ReportQueue queue) {
    myName = name;
    myParameters = parameters;
    myQueue = queue;
    myStopSignaled = false;
  }

  @NotNull
  protected XmlReportPluginParameters getParameters() {
    return myParameters;
  }

  @NotNull
  protected ReportQueue getQueue() {
    return myQueue;
  }

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  public Thread getThread() {
    if (myThread == null) throw new IllegalStateException("Thread not initialized");
    return myThread;
  }

  @SuppressWarnings({"NullableProblems"})
  @NotNull
  protected FlowLogger getThreadLogger() {
    if (myThreadLogger == null) throw new IllegalStateException("Thread logger not initialized");
    return myThreadLogger;
  }

  public void start() {
    run();
  }

  private void run() {
    myThread = new Thread(new Runnable() {
      public void run() {
        myThreadLogger = getParameters().getLogger().getThreadLogger();
        try {
          while (!isStopSignaled()) {
            doStep();
            Thread.sleep(getPeriod());
          }
          doPostStep();
        } catch (Throwable e) {
          getThreadLogger().exception(e);
          getLogger().error(e.toString(), e);
        } finally {
          getThreadLogger().disposeFlow();
        }
      }
    }, myName);
    myThread.start();
  }

  protected  boolean isStopSignaled() {
    return myStopSignaled;
  }

  public void signalStop() {
    myStopSignaled = true;
  }

  public void join() {
    try {
      getThread().join();
    } catch (InterruptedException e) {
      // we use thread logger here, as join is called outside watcher thread
      myParameters.getLogger().getThreadLogger().exception(e);
      getLogger().error(e.toString(), e);
    }
  }

  protected abstract void doStep() throws Exception;
  protected abstract void doPostStep() throws Exception;
  protected abstract long getPeriod();
  @NotNull protected abstract Logger getLogger();
}
