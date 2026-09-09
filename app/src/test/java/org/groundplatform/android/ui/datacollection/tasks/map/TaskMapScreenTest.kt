/*
 * Copyright 2026 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.ui.components.LOCATION_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.LOCATION_NOT_LOCKED_TEST_TAG
import org.groundplatform.android.ui.components.MapFloatingActionButtonType
import org.groundplatform.android.ui.datacollection.tasks.map.components.LocationInfo
import org.groundplatform.android.ui.datacollection.tasks.map.components.LocationInfoCardTestTags
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TaskMapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun setContent(
    locationLockButtonType: MapFloatingActionButtonType =
      MapFloatingActionButtonType.LocationNotLocked,
    shouldShowRecenter: Boolean = false,
    isCenterMarkerVisible: Boolean = true,
    locationInfo: LocationInfo? = null,
    onMapTypeClicked: () -> Unit = {},
    onLocationLockClicked: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      AppTheme {
        TaskMapScreen(
          locationLockButtonType = locationLockButtonType,
          shouldShowRecenter = shouldShowRecenter,
          isCenterMarkerVisible = isCenterMarkerVisible,
          locationInfo = locationInfo,
          onMapTypeClicked = onMapTypeClicked,
          onLocationLockClicked = onLocationLockClicked,
        )
      }
    }
  }

  @Test
  fun `Center marker is displayed when isCenterMarkerVisible is true`() {
    setContent(isCenterMarkerVisible = true)

    composeTestRule.onNodeWithTag(TaskMapScreenTestTags.CENTER_MARKER).assertIsDisplayed()
  }

  @Test
  fun `Center marker is not displayed when isCenterMarkerVisible is false`() {
    setContent(isCenterMarkerVisible = false)

    composeTestRule.onNodeWithTag(TaskMapScreenTestTags.CENTER_MARKER).assertDoesNotExist()
  }

  @Test
  fun `Map type button is displayed and clicking triggers callback`() {
    var mapTypeClicked = false
    setContent(onMapTypeClicked = { mapTypeClicked = true })

    composeTestRule
      .onNodeWithTag(MapFloatingActionButtonType.MapType.testTag)
      .assertIsDisplayed()
      .performClick()

    assertThat(mapTypeClicked).isTrue()
  }

  @Test
  fun `Recenter button is displayed when shouldShowRecenter is true and clicking triggers callback`() {
    var recenterClicked = false
    setContent(
      shouldShowRecenter = true,
      onLocationLockClicked = { recenterClicked = true },
    )

    composeTestRule.onNodeWithText(getString(R.string.recenter)).assertIsDisplayed().performClick()

    assertThat(recenterClicked).isTrue()
  }

  @Test
  fun `Recenter button is not displayed when shouldShowRecenter is false`() {
    setContent(shouldShowRecenter = false)

    composeTestRule.onNodeWithText(getString(R.string.recenter)).assertDoesNotExist()
  }

  @Test
  fun `Location lock button is displayed with correct icon when not locked and clicking triggers callback`() {
    var locationLockClicked = false
    setContent(
      locationLockButtonType = MapFloatingActionButtonType.LocationNotLocked,
      onLocationLockClicked = { locationLockClicked = true },
    )

    composeTestRule.onNodeWithTag(LOCATION_NOT_LOCKED_TEST_TAG).assertIsDisplayed().performClick()

    assertThat(locationLockClicked).isTrue()
  }

  @Test
  fun `Location lock button is displayed with correct icon when locked`() {
    setContent(locationLockButtonType = MapFloatingActionButtonType.LocationLocked())

    composeTestRule.onNodeWithTag(LOCATION_LOCKED_TEST_TAG).assertIsDisplayed()
  }

  @Test
  fun `Location info card is not displayed when locationInfo is null`() {
    setContent(locationInfo = null)

    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.LOCATION_INFO_CARD).assertDoesNotExist()
  }

  @Test
  fun `Location info card is displayed when locationInfo is not null`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.current_location,
        locationText = "29º58’15” N  114º36’17”W",
        accuracyText = "3m",
        isAccuracyGood = true,
      )
    setContent(locationInfo = locationInfo)

    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.LOCATION_INFO_CARD).assertIsDisplayed()
  }
}
