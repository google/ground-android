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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.Matchers.not
import org.junit.Test

@HiltAndroidTest
class AddLocationOfInterestTest : BaseMainActivityTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(FakeData.USER)
  }

  // Given: a logged in user - with an active survey with no map markers.
  // When: they tap on the centre of the map.
  // Then: nothing happens - the feature fragment is not displayed.
  @Test
  fun tappingCrosshairOnEmptyMapDoesNothing() {
    dataBindingIdlingResource.monitorActivity(scenarioRule.scenario)

    // Tap on the checkbox
    onView(withId(R.id.agreeCheckBox)).perform(click())

    // Tap on Submit on Terms Fragment
    onView(withId(R.id.agreeButton)).perform(click())

    // Tap on the cross-hair at the centre of the map.
    onView(withId(R.id.map_crosshairs_img)).perform(click())

    // Verify that the title is not displayed.
    onView(withId(R.id.location_of_interest_title)).check(matches(not(isCompletelyDisplayed())))
  }
}
