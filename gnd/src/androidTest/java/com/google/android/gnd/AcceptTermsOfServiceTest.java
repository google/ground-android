/*
 * Copyright 2021 Google LLC
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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import com.google.android.gnd.persistence.remote.RemoteStorageModule;
import com.google.android.gnd.persistence.sync.WorkManagerModule;
import com.google.android.gnd.system.auth.AuthenticationModule;
import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.UninstallModules;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@UninstallModules({
    AuthenticationModule.class,
    RemoteStorageModule.class,
    WorkManagerModule.class,
})
@HiltAndroidTest
public class AcceptTermsOfServiceTest {

  // Ensures that the Hilt component is initialized before running the ActivityScenarioRule.
  @Rule(order = 0)
  public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  // Swaps the background executor in Architecture Components with one which executes synchronously.
  @Rule(order = 1)
  public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

  // Sets the preferences so no login is required and an active project is selected.
  @Rule(order = 2)
  public SetPreferencesRule preferencesRule = new SetPreferencesRule();

  // Load the MainActivity for each test.
  @Rule(order = 3)
  public ActivityScenarioRule<MainActivity> scenarioRule =
      new ActivityScenarioRule<>(MainActivity.class);

  // Create an idling resource which can be used to wait for databindings to complete.
  private final DataBindingIdlingResource dataBindingIdlingResource =
      new DataBindingIdlingResource();

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
  }

  /**
   * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
   */
  @After
  public void unregisterIdlingResource() {
    IdlingRegistry.getInstance().unregister(dataBindingIdlingResource);
  }

  // Given: a logged in user - with terms not accepted.
  // When: they tap on the checkbox of the TermsOfService Screen.
  // Then: Agree button should be enabled and upon click of that next screen
  //       should appear.
  @Test
  public void acceptTerms() {

    dataBindingIdlingResource.monitorActivity(scenarioRule.getScenario());

    // Verify that the agree button is not enabled by default.
    onView(withId(R.id.agreeButton)).check(matches(not(isEnabled())));

    // Tap on the checkbox.
    onView(withId(R.id.agreeCheckBox)).perform(click());

    // Verify that the agree button is enabled when checkbox is checked.
    onView(withId(R.id.agreeButton)).check(matches(isEnabled()));

    // Verify that the terms text matched with fake data.
    onView(withId(R.id.termsText)).check(matches(withText(FakeData.TERMS_OF_SERVICE)));

    // Tap on the button
    onView(withId(R.id.agreeButton)).perform(click());
  }
}
