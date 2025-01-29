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
package com.google.android.ground.ui.offlineareas

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.FakeData.OFFLINE_AREA
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.launchFragmentWithNavController
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreasFragmentTest : BaseHiltTest() {

  private lateinit var fragment: OfflineAreasFragment

  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore
  private lateinit var navController: NavController

  @Test
  fun `Toolbar text is displayed`() {
    setupFragment()
    onView(withId(R.id.offline_areas_toolbar))
      .check(matches(hasDescendant(withText(fragment.getString(R.string.offline_map_imagery)))))
  }

  @Test
  fun `Heading text is displayed`() {
    setupFragment()
    onView(withId(R.id.offline_areas_list_title))
      .check(matches(withText(fragment.getString(R.string.offline_downloaded_areas))))
    onView(withId(R.id.offline_areas_list_tip))
      .check(matches(withText(fragment.getString(R.string.offline_area_list_tip))))
    onView(withId(R.id.no_areas_downloaded_message))
      .check(matches(withText(fragment.getString(R.string.no_basemaps_downloaded))))
  }

  @Test
  fun `showOfflineAreaSelector should navigate correctly`() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    advanceUntilIdle()

    launchFragmentWithNavController<OfflineAreasFragment>(
      destId = R.id.offline_areas_fragment,
      navControllerCallback = { navController = it },
    )

    onView(withId(R.id.floatingActionButton)).perform(click())
    assertThat(navController.currentDestination?.id).isEqualTo(R.id.offline_area_selector_fragment)
  }

  @Test
  fun `List is Displayed`() = runWithTestDispatcher {
    setupFragment()

    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)
    advanceUntilIdle()

    // List is displayed
    composeTestRule.onNodeWithTag("offline area list").assertIsDisplayed()

    // Has exactly one item
    composeTestRule.onNodeWithTag("offline area list").onChildren().assertCountEquals(1)
    composeTestRule.onNodeWithTag("offline area list").onChildAt(0).isDisplayed()

    // Matches item's text
    composeTestRule.onNodeWithText("Test Area").isDisplayed()
    composeTestRule.onNodeWithText("<1\u00A0MB").isDisplayed()
  }

  private fun setupFragment() {
    launchFragmentInHiltContainer<OfflineAreasFragment> { fragment = this as OfflineAreasFragment }
  }
}
