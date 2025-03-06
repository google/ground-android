/*
 * Copyright 2024 Google LLC
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
package org.groundplatform.android

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.KeyValueBuilder
import javax.inject.Inject
import javax.inject.Singleton
import org.groundplatform.android.Config.isReleaseBuild

@Singleton
class FirebaseCrashLogger @Inject constructor() {

  fun recordException(priority: Int, message: String, t: Throwable?) {
    val crashlytics = FirebaseCrashlytics.getInstance()
    crashlytics.log(message)
    if (t != null && priority == Log.ERROR) {
      crashlytics.recordException(t)
    }
  }

  fun setSelectedSurveyId(surveyId: String?) {
    setCustomKeys { key("selectedSurveyId", surveyId ?: "") }
  }

  fun setScreenName(viewClass: String) {
    setCustomKeys { key("screenName", viewClass) }
  }

  private fun setCustomKeys(init: KeyValueBuilder.() -> Unit) {
    if (isReleaseBuild()) {
      KeyValueBuilder(FirebaseCrashlytics.getInstance()).init()
    }
  }
}
