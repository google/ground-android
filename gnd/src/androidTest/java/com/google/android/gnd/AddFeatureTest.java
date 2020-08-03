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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import org.junit.Rule;
import org.junit.Test;

/**
 * Given: - a logged in user - with an active project which doesn't direct the user to add an
 * observation when adding a feature. When: - they tap the "Add feature" FAB - choose a feature type
 * Then: - the observation marker is displayed on the map screen
 */
@UninstallModules()
@HiltAndroidTest
public class AddFeatureTest {

  // Ensures that the Hilt component is initialized before running the ActivityScenarioRule
  public @Rule(order = 0)
  HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  public @Rule(order = 1)
  ActivityScenarioRule scenarioRule =
    new ActivityScenarioRule(MainActivity.class);

  @Test
  public void addFeature() {

    ActivityScenario<MainActivity> scenario = scenarioRule.getScenario();
    onView(withId(R.id.add_feature_btn)).perform(click());

    // With the feature type list click on the first one

  }
}
