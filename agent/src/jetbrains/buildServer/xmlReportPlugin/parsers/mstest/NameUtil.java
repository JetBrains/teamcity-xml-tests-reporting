/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.parsers.mstest;

import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Petrenko
 *         Created: 20.01.2009 14:49:47
 */
class NameUtil {
  @Nullable
  public static String getTestName(String clazzFQ, String name) {
    if (clazzFQ != null && name != null) {
      final int fq = clazzFQ.indexOf(",");
      return (fq > 0 ? clazzFQ.substring(0, fq) : clazzFQ) + "." + name;
    }
    return null;
  }
}
