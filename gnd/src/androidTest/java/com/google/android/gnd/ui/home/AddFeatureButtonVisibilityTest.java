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

package com.google.android.gnd.ui.home;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.lifecycle.Lifecycle.State;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import com.google.android.gnd.DataBindingIdlingResource;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.persistence.local.LocalDatabaseModule;
import com.google.android.gnd.persistence.remote.FakeRemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteDataStore;
import com.google.android.gnd.persistence.remote.RemoteStorageModule;
import com.google.android.gnd.rx.SchedulersModule;
import com.google.android.gnd.system.auth.AuthenticationModule;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@UninstallModules({
    AuthenticationModule.class,
    RemoteStorageModule.class,
    LocalDatabaseModule.class,
    SchedulersModule.class
})
@HiltAndroidTest
public class AddFeatureButtonVisibilityTest {

  // Create an idling resource which can be used to wait for databindings to complete.
  private final DataBindingIdlingResource dataBindingIdlingResource =
      new DataBindingIdlingResource();

  @Rule(order = 0)
  public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Inject RemoteDataStore remoteDataStore;

  private static ViewAssertion isVisible() {
    return matches(ViewMatchers.withEffectiveVisibility(Visibility.VISIBLE));
  }

  private static ViewAssertion isGone() {
    return matches(ViewMatchers.withEffectiveVisibility(Visibility.GONE));
  }

  /**
   * Idling resources tell Espresso that the app is idle or busy. This is needed when operations are
   * not scheduled in the main Looper (for example when executed on a different thread).
   */
  @Before
  public void registerIdlingResource() {
    // Register the databinding idling resource. If a test is dependent on a databinding then it
    // MUST monitor the activity, otherwise it will timeout waiting for the databinding to
    // complete. See tests below for examples.
    IdlingRegistry.getInstance().register(dataBindingIdlingResource);

    // Inject dependencies
    hiltRule.inject();
  }

  /**
   * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
   */
  @After
  public void unregisterIdlingResource() {
    IdlingRegistry.getInstance().unregister(dataBindingIdlingResource);
  }

  private void setActiveProject(String projectId) {
    ((FakeRemoteDataStore) remoteDataStore).setActiveProjectId(projectId);
  }

  @Test
  public void addFeatureButton_shouldBeVisible_whenLayersArePresent() {
    setActiveProject(FakeData.PROJECT_ID_WITH_LAYER_AND_NO_FORM);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

      dataBindingIdlingResource.monitorActivity(scenario);
      onView(withId(R.id.add_feature_btn)).check(isVisible());

      scenario.moveToState(State.DESTROYED);
    }
  }

  @Test
  public void addFeatureButton_shouldBeGone_whenLayersAreNotPresent() {
    setActiveProject(FakeData.PROJECT_ID_WITH_NO_LAYERS);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

      dataBindingIdlingResource.monitorActivity(scenario);
      onView(withId(R.id.add_feature_btn)).check(isGone());

      scenario.moveToState(State.DESTROYED);
    }
  }
}
