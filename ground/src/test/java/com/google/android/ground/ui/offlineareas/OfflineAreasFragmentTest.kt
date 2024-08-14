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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentInHiltContainer
import com.google.android.ground.persistence.local.stores.LocalOfflineAreaStore
import com.google.android.ground.util.recyclerview.atPositionOnView
import com.sharedtest.FakeData.OFFLINE_AREA
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class OfflineAreasFragmentTest : BaseHiltTest() {

  lateinit var fragment: OfflineAreasFragment
  @Inject lateinit var localOfflineAreaStore: LocalOfflineAreaStore

  @Before
  override fun setUp() {
    super.setUp()
    launchFragmentInHiltContainer<OfflineAreasFragment> { fragment = this as OfflineAreasFragment }
  }

  @Test
  fun `toolbar text should be correct`() {
    onView(withId(R.id.offline_areas_toolbar))
      .check(matches(hasDescendant(withText(fragment.getString(R.string.offline_map_imagery)))))
  }

  @Test
  fun `heading should be correct`() {
    onView(withId(R.id.offline_areas_list_title))
      .check(matches(withText(fragment.getString(R.string.offline_downloaded_areas))))
    onView(withId(R.id.offline_areas_list_tip))
      .check(matches(withText(fragment.getString(R.string.offline_area_list_tip))))
    onView(withId(R.id.no_areas_downloaded_message))
      .check(matches(withText(fragment.getString(R.string.no_basemaps_downloaded))))
  }

  @Test
  fun `list is visible`() = runWithTestDispatcher {
    localOfflineAreaStore.insertOrUpdate(OFFLINE_AREA)

    advanceUntilIdle()
    onView(withId(R.id.offline_areas_list)).check(matches(isDisplayed()))

    verifyTextOnOfflineAreasListItemAtPosition(
      itemPosition = 0,
      targetView = R.id.offline_area_list_item_name,
      stringToMatch = "Test Area",
    )
    verifyTextOnOfflineAreasListItemAtPosition(
      itemPosition = 0,
      targetView = R.id.offline_area_list_item_size,
      stringToMatch = "<1\u00A0MB",
    )
  }

  private fun verifyTextOnOfflineAreasListItemAtPosition(
    itemPosition: Int,
    targetView: Int,
    stringToMatch: String,
  ) {
    onView(
        atPositionOnView(
          recyclerViewId = R.id.offline_areas_list,
          position = itemPosition,
          targetViewId = targetView,
        )
      )
      .check(matches(withText(stringToMatch)))
  }
}
