package com.google.android.ground

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.KeyValueBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseCrashLogging @Inject constructor() {

  fun setSelectedSurveyId(surveyId: String?) {
    setCustomKeys { key("selectedSurveyId", surveyId ?: "") }
  }

  private fun setCustomKeys(init: KeyValueBuilder.() -> Unit) {
    val builder = KeyValueBuilder(FirebaseCrashlytics.getInstance())
    builder.init()
  }
}
