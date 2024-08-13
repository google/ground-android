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
package com.google.android.ground.ui.offlineareas.selector

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreaSelectorFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreaSelectorFragment

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<OfflineAreaSelectorFragment> {
      fragment = this as OfflineAreaSelectorFragment
    }
  }

  @Test
  fun `all the buttons are visible`() {
    onView(withId(R.id.download_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isDisplayed()))
    onView(withId(R.id.cancel_button)).check(matches(isEnabled()))
  }

  @Test
  fun `default value of bottomText`() {
    onView(withId(R.id.bottom_text)).check(matches(withText("")))
  }

  @Test
  fun `toolbar text should be correct`() {
    onView(withId(R.id.offline_area_selector_toolbar))
      .check(
        matches(hasDescendant(withText(fragment.getString(R.string.offline_area_selector_title))))
      )
  }
}
