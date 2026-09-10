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
import com.google.common.truth.Truth.assertThat
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

    composeTestRule.onNodeWithTag(LOCATION_INFO_CARD_TEST_TAG).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_TITLE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.current_location))
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_VALUE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals("29º58’15” N  114º36’17”W")
    composeTestRule
      .onNodeWithTag(ACCURACY_TITLE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.accuracy))
    composeTestRule
      .onNodeWithTag(ACCURACY_VALUE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals("3m")
  }

  @Test
  fun `Location info card is displayed with poor accuracy when isAccuracyGood is false`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.current_location,
        locationText = "29º58’15” N  114º36’17”W",
        accuracyText = "25m",
        isAccuracyGood = false,
      )
    composeTestRule.setContent { AppTheme { LocationInfoCard(locationInfo = locationInfo) } }

    composeTestRule.onNodeWithTag(LOCATION_INFO_CARD_TEST_TAG).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_TITLE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.current_location))
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_VALUE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals("29º58’15” N  114º36’17”W")
    composeTestRule
      .onNodeWithTag(ACCURACY_TITLE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.accuracy))
    composeTestRule
      .onNodeWithTag(ACCURACY_VALUE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals("25m")
  }

  @Test
  fun `Location info card displays question mark when accuracy is unknown`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.current_location,
        locationText = "29º58’15” N  114º36’17”W",
        accuracyText = "?",
        isAccuracyGood = false,
      )
    composeTestRule.setContent { AppTheme { LocationInfoCard(locationInfo = locationInfo) } }

    composeTestRule.onNodeWithTag(ACCURACY_VALUE_TEST_TAG).assertIsDisplayed().assertTextEquals("?")
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

    composeTestRule.onNodeWithTag(LOCATION_INFO_CARD_TEST_TAG).assertIsDisplayed()
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_TITLE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals(getString(R.string.map_location))
    composeTestRule
      .onNodeWithTag(CURRENT_LOCATION_VALUE_TEST_TAG)
      .assertIsDisplayed()
      .assertTextEquals("29º58’15” N  114º36’17”W")
    composeTestRule.onNodeWithTag(ACCURACY_TITLE_TEST_TAG).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ACCURACY_VALUE_TEST_TAG).assertDoesNotExist()
  }

  @Test
  fun `LocationInfo constructor defaults accuracyText to null and isAccuracyGood to false`() {
    val locationInfo =
      LocationInfo(
        titleRes = R.string.map_location,
        locationText = "10º00’00” N  20º00’00”W",
      )

    assertThat(locationInfo.accuracyText).isNull()
    assertThat(locationInfo.isAccuracyGood).isFalse()
  }
}
