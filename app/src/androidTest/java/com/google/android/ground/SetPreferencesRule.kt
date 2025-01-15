/*
 * Copyright 2020 Google LLC
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
package com.google.android.ground

import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.google.android.ground.persistence.local.LocalValueStore
import com.sharedtest.FakeData
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule to be used in tests that sets the SharedPreferences needed to avoid login and survey
 * selection.
 */
class SetPreferencesRule : TestWatcher() {
  @InstallIn(SingletonComponent::class)
  @EntryPoint
  internal interface SetPreferencesRuleEntryPoint {
    fun preferenceStorage(): SharedPreferences
  }

  public override fun starting(description: Description) {
    super.starting(description)
    EntryPointAccessors.fromApplication(
        getApplicationContext(),
        SetPreferencesRuleEntryPoint::class.java,
      )
      .preferenceStorage()
      .apply {
        edit().clear().putString(LocalValueStore.ACTIVE_SURVEY_ID_KEY, FakeData.SURVEY.id).apply()
      }
  }
}
