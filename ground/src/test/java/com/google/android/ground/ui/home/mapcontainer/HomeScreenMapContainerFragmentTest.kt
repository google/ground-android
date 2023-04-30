/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.home.mapcontainer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.android.ground.*
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.map.gms.GoogleMapsFragment
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerFragmentTest : BaseHiltTest() {

  @Inject lateinit var navigator: Navigator
  private lateinit var fragment: HomeScreenMapContainerFragment

  @Before
  override fun setUp() {
    super.setUp()
    setupFragment()
  }

  @Test
  fun test_clickMapType_launchesMapTypeDialogFragment() {
    val navDirectionsTestObserver = navigator.getNavigateRequests().test()

    onView(withId(R.id.map_type_btn)).perform(click()).check(matches(isEnabled()))

    navDirectionsTestObserver.assertValue(
      MapTypeDialogFragmentDirections.showMapTypeDialogFragment(GoogleMapsFragment.MAP_TYPES)
    )
  }

  private fun setupFragment() {
    launchFragmentWithNavController<HomeScreenMapContainerFragment>(
      destId = R.id.home_screen_fragment
    ) {
      fragment = this as HomeScreenMapContainerFragment
    }
  }
}
