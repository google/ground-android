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
import app.cash.turbine.test
import com.google.android.ground.*
import com.google.android.ground.NavGraphDirections.ShowMapTypeDialogFragment
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.map.MapType
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class HomeScreenMapContainerFragmentTest : BaseHiltTest() {

  @Inject lateinit var navigator: Navigator

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<HomeScreenMapContainerFragment>()
  }

  @Test
  fun clickMapType_launchesMapTypeDialogFragment() = runWithTestDispatcher {
    onView(withId(R.id.map_type_btn)).perform(click()).check(matches(isEnabled()))

    navigator.getNavigateRequests().test {
      val result = expectMostRecentItem()
      assertThat(result).isInstanceOf(ShowMapTypeDialogFragment::class.java)
      assertThat((result as ShowMapTypeDialogFragment).mapTypes).isEqualTo(MapType.values())
    }
  }
}
