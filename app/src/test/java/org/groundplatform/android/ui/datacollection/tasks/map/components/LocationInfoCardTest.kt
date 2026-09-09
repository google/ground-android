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
package org.groundplatform.android.ui.datacollection.tasks.map.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationInfoCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Location info card is displayed with current location and accuracy when accuracy is provided`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.current_location,
        locationText = "29º58’15” N  114º36’17”W",
        accuracyText = "3m",
        isAccuracyGood = true,
      )
    composeTestRule.setContent { AppTheme { LocationInfoCard(locationInfo = locationInfo) } }

    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.LOCATION_INFO_CARD).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.CURRENT_LOCATION_TITLE)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.current_location))
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.CURRENT_LOCATION_VALUE)
      .assertIsDisplayed()
      .assertTextEquals("29º58’15” N  114º36’17”W")
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.ACCURACY_TITLE)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.accuracy))
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.ACCURACY_VALUE)
      .assertIsDisplayed()
      .assertTextEquals("3m")
  }

  @Test
  fun `Location info card is displayed with map location and no accuracy when accuracy is null`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.map_location,
        locationText = "29º58’15” N  114º36’17”W",
        accuracyText = null,
      )
    composeTestRule.setContent { AppTheme { LocationInfoCard(locationInfo = locationInfo) } }

    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.LOCATION_INFO_CARD).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.CURRENT_LOCATION_TITLE)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.map_location))
    composeTestRule
      .onNodeWithTag(LocationInfoCardTestTags.CURRENT_LOCATION_VALUE)
      .assertIsDisplayed()
      .assertTextEquals("29º58’15” N  114º36’17”W")
    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.ACCURACY_TITLE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(LocationInfoCardTestTags.ACCURACY_VALUE).assertDoesNotExist()
  }
}
