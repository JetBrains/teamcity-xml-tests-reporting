/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package jetbrains.buildServer.xmlReportPlugin.inspections;

import jetbrains.buildServer.xmlReportPlugin.MessageLogger;
import org.jetbrains.annotations.NotNull;

/**
 * User: vbedrosova
 * Date: 17.02.11
 * Time: 13:24
 */
public interface InspectionReporter extends MessageLogger {
  /**
   * Report inspection instance
   *
   * @param inspection Inspection description
   */
  void reportInspection(@NotNull InspectionResult inspection);

  /**
   * Report inspection description
   *
   * @param inspectionType Inspection type description
   */
  void reportInspectionType(@NotNull InspectionTypeResult inspectionType);
}
