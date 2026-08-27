/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.groundplatform.testing

import org.groundplatform.domain.system.CrashLogger

/** In-memory test double for [CrashLogger]. */
class FakeCrashLogger : CrashLogger {
  var lastSelectedSurveyId: String? = null
  var lastScreenName: String? = null

  override fun setSelectedSurveyId(surveyId: String?) {
    lastSelectedSurveyId = surveyId
  }

  override fun setScreenName(viewClass: String) {
    lastScreenName = viewClass
  }
}
