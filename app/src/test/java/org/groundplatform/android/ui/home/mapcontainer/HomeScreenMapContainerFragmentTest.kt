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
package org.groundplatform.android.ui.home.mapcontainer

import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentWithNavController
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerFragmentTest : BaseHiltTest() {

  private lateinit var navController: NavController

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentWithNavController<HomeScreenMapContainerFragment>(
      destId = R.id.home_screen_fragment,
      navControllerCallback = { navController = it },
    )
  }

  @Test
  fun clickMapType_launchesMapTypeDialogFragment() = runWithTestDispatcher {
    onView(withId(R.id.map_type_btn)).perform(click()).check(matches(isEnabled()))
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.mapTypeDialogFragment)
  }
}
