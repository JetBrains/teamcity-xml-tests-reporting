/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.tests;

import jetbrains.buildServer.xmlReportPlugin.MessageLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 24.10.2008 18:27:35
 */
public interface TestReporter extends MessageLogger {
  void openTestSuite(@NotNull String name);
  void openTest(@NotNull String name);

  void testStdOutput(@NotNull String text);
  void testErrOutput(@NotNull String text);
  void testFail(@Nullable String error, @Nullable String stacktrace);
  void testIgnored(@NotNull String message);

  void closeTest(long duration);
  void closeTestSuite();
}
