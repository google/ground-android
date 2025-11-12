/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.surveyselector.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveyCardItemTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Displays restricted survey correctly`() {
    val item =
      SurveyListItem(
        id = "1",
        title = "Test Survey",
        description = "Description",
        availableOffline = false,
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
    composeTestRule.setContent { SurveyCardItem(item = item, onCardClick = {}, menuClick = {}) }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.access_restricted))
      .assertIsDisplayed()

    assertAvailableOffline(false)
  }

  @Test
  fun `Displays unspecified access survey correctly`() {
    val item =
      SurveyListItem(
        id = "2",
        title = "Test Survey",
        description = "Description",
        availableOffline = false,
        generalAccess = Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED,
      )
    composeTestRule.setContent { SurveyCardItem(item = item, onCardClick = {}, menuClick = {}) }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.access_restricted))
      .assertIsDisplayed()

    assertAvailableOffline(false)
  }

  @Test
  fun `Displays unlisted survey correctly`() {
    val item =
      SurveyListItem(
        id = "3",
        title = "Test Survey",
        description = "Description",
        availableOffline = false,
        generalAccess = Survey.GeneralAccess.UNLISTED,
      )
    composeTestRule.setContent { SurveyCardItem(item = item, onCardClick = {}, menuClick = {}) }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.access_unlisted))
      .assertIsDisplayed()

    assertAvailableOffline(false)
  }

  @Test
  fun `Displays public survey correctly`() {
    val item =
      SurveyListItem(
        id = "4",
        title = "Test Survey",
        description = "Description",
        availableOffline = false,
        generalAccess = Survey.GeneralAccess.PUBLIC,
      )
    composeTestRule.setContent { SurveyCardItem(item = item, onCardClick = {}, menuClick = {}) }

    composeTestRule
      .onNodeWithText(composeTestRule.activity.getString(R.string.access_public))
      .assertIsDisplayed()

    assertAvailableOffline(false)
  }

  @Test
  fun `Displays offline survey correctly`() {
    val item =
      SurveyListItem(
        id = "1",
        title = "Test Survey",
        description = "Description",
        availableOffline = true,
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
    composeTestRule.setContent { SurveyCardItem(item = item, onCardClick = {}, menuClick = {}) }

    assertAvailableOffline(true)
  }

  private fun assertAvailableOffline(isAvailable: Boolean) {
    composeTestRule
      .onNodeWithContentDescription(
        composeTestRule.activity.getString(R.string.offline_icon_description)
      )
      .apply { if (isAvailable) assertIsDisplayed() else assertIsNotDisplayed() }

    composeTestRule
      .onNodeWithContentDescription(
        composeTestRule.activity.getString(R.string.more_options_icon_description)
      )
      .apply { if (isAvailable) assertIsDisplayed() else assertIsNotDisplayed() }
  }
}
