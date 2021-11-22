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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import dagger.hilt.android.testing.HiltAndroidRule;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

/**
 * Configures all necessary rules and {@link MainActivity} needed for running instrumentation tests.
 */
public abstract class BaseMainActivityTest {

  // Create an idling resource which can be used to wait for data-bindings to complete.
  public final DataBindingIdlingResource dataBindingIdlingResource =
      new DataBindingIdlingResource();

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

  @Before
  @OverridingMethodsMustInvokeSuper
  public void setUp() {
    hiltRule.inject();
  }

  /**
   * Idling resources tell Espresso that the app is idle or busy. This is needed when operations are
   * not scheduled in the main Looper (for example when executed on a different thread).
   */
  @Before
  public void registerIdlingResource() {
    // Register the data-binding idling resource. If a test is dependent on a data-binding then it
    // MUST monitor the activity, otherwise it will timeout waiting for the data-binding to
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
}
