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
package com.google.android.ground

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.sharedtest.FakeData
import com.sharedtest.persistence.remote.FakeRemoteDataStore
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.Matchers.not
import org.junit.Test

@HiltAndroidTest
class AcceptTermsOfServiceTest : BaseMainActivityTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var fakeRemoteDataStore: FakeRemoteDataStore

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(FakeData.USER)
    fakeRemoteDataStore.setTestSurvey(FakeData.SURVEY)
  }

  // Given: a logged in user - with terms not accepted.
  // When: they tap on the checkbox of the TermsOfService Screen.
  // Then: Agree button should be enabled and upon click of that next screen should appear.
  @Test
  fun acceptTerms() {
    dataBindingIdlingResource.monitorActivity(scenarioRule.scenario)

    // Verify that the agree button is not enabled by default.
    onView(withId(R.id.agreeButton)).check(matches(not(isEnabled())))

    // Tap on the checkbox.
    onView(withId(R.id.agreeCheckBox)).perform(click())

    // Verify that the agree button is enabled when checkbox is checked.
    onView(withId(R.id.agreeButton)).check(matches(isEnabled()))

    // Verify that the terms text matched with fake data.
    onView(withId(R.id.termsText)).check(matches(withText(FakeData.TERMS_OF_SERVICE.text)))

    // Tap on the button
    onView(withId(R.id.agreeButton)).perform(click())
  }
}
