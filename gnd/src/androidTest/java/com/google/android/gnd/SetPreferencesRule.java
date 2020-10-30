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

package com.google.android.gnd;

import static com.google.android.gnd.persistence.local.LocalValueStore.ACTIVE_PROJECT_ID_KEY;

import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.android.EntryPointAccessors;
import dagger.hilt.android.components.ApplicationComponent;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/**
 * Rule to be used in tests that sets the SharedPreferences needed to avoid login and project
 * selection.
 */
class SetPreferencesRule extends TestWatcher {

  @InstallIn(ApplicationComponent.class)
  @EntryPoint
  interface SetPreferencesRuleEntryPoint {

    SharedPreferences preferenceStorage();
  }

  @Override
  public void starting(Description description) {
    super.starting(description);

    SharedPreferences prefs = EntryPointAccessors.fromApplication(
        ApplicationProvider.getApplicationContext(),
        SetPreferencesRuleEntryPoint.class
    ).preferenceStorage();

    prefs
        .edit()
        .clear()
        .putString(ACTIVE_PROJECT_ID_KEY, FakeData.PROJECT_ID_WITH_LAYER_AND_NO_FORM)
        .apply();
  }
}
