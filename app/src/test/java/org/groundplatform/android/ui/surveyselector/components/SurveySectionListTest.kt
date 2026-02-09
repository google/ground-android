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
package org.groundplatform.android.ui.surveyselector.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidTest
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.ui.surveyselector.SurveySection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySectionListTest : BaseHiltTest() {

  @get:Rule override val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun `Displays section headers correctly`() {
    val dummySurveys =
      listOf(
        SurveyListItem("1", "Tree Survey", "Track tree growth", true, Survey.GeneralAccess.PUBLIC)
      )
    val sectionData =
      listOf(
        SurveySection(R.string.section_on_device, dummySurveys),
        SurveySection(R.string.section_shared_with_me, emptyList()),
        SurveySection(R.string.section_public, emptyList()),
      )

    composeTestRule.setContent {
      SurveySectionList(sectionData = sectionData, onConfirmDelete = {}, onCardClick = {})
    }

    val onDeviceTitle = composeTestRule.activity.getString(R.string.section_on_device)
    val sharedTitle = composeTestRule.activity.getString(R.string.section_shared_with_me)
    val publicTitle = composeTestRule.activity.getString(R.string.section_public)

    composeTestRule.onNodeWithText("$onDeviceTitle (1)").assertIsDisplayed()
    composeTestRule.onNodeWithText("$sharedTitle (0)").assertIsDisplayed()
    composeTestRule.onNodeWithText("$publicTitle (0)").assertIsDisplayed()
  }

  @Test
  fun `Initial expansion state is correct`() {
    val dummySurveys =
      listOf(SurveyListItem("1", "Device Survey", "Desc", true, Survey.GeneralAccess.PUBLIC))
    val publicSurveys =
      listOf(SurveyListItem("2", "Public Survey", "Desc", false, Survey.GeneralAccess.PUBLIC))
    val sectionData =
      listOf(
        SurveySection(R.string.section_on_device, dummySurveys),
        SurveySection(R.string.section_shared_with_me, emptyList()),
        SurveySection(R.string.section_public, publicSurveys),
      )

    composeTestRule.setContent {
      SurveySectionList(sectionData = sectionData, onConfirmDelete = {}, onCardClick = {})
    }

    // "On Device" is expanded by default, so its survey card is visible.
    composeTestRule.onNodeWithText("Device Survey").assertIsDisplayed()

    // "Public" is collapsed by default, so its survey card should not be visible.
    composeTestRule.onNodeWithText("Public Survey").assertDoesNotExist()
  }

  @Test
  fun `Clicking expandable section toggles visibility`() {
    val publicSurveys =
      listOf(SurveyListItem("2", "Public Survey", "Desc", false, Survey.GeneralAccess.PUBLIC))
    val sectionData =
      listOf(
        SurveySection(R.string.section_on_device, emptyList()),
        SurveySection(R.string.section_shared_with_me, emptyList()),
        SurveySection(R.string.section_public, publicSurveys),
      )

    composeTestRule.setContent {
      SurveySectionList(sectionData = sectionData, onConfirmDelete = {}, onCardClick = {})
    }

    // "Public Survey" should not be visible initially.
    composeTestRule.onNodeWithText("Public Survey").assertDoesNotExist()

    // Click the public section header to expand it.
    val publicTitle = composeTestRule.activity.getString(R.string.section_public)
    composeTestRule.onNodeWithText("$publicTitle (1)").performClick()

    // Now it should be displayed.
    composeTestRule.onNodeWithText("Public Survey").assertIsDisplayed()

    // Click again to collapse it.
    composeTestRule.onNodeWithText("$publicTitle (1)").performClick()

    // It should disappear.
    composeTestRule.onNodeWithText("Public Survey").assertDoesNotExist()
  }

  @Test
  fun `Clicking survey card invokes onCardClick callback`() {
    val dummySurveys =
      listOf(
        SurveyListItem("1", "Tree Survey", "Track tree growth", true, Survey.GeneralAccess.PUBLIC)
      )
    val sectionData = listOf(SurveySection(R.string.section_on_device, dummySurveys))

    var clickedId: String? = null

    composeTestRule.setContent {
      SurveySectionList(
        sectionData = sectionData,
        onConfirmDelete = {},
        onCardClick = { clickedId = it },
      )
    }

    composeTestRule.onNodeWithText("Tree Survey").performClick()

    assertEquals("1", clickedId)
  }

  @Test
  fun `Clicking menu deletes survey through confirmation dialog`() {
    val dummySurveys =
      listOf(
        SurveyListItem("1", "Tree Survey", "Track tree growth", true, Survey.GeneralAccess.PUBLIC)
      )
    val sectionData = listOf(SurveySection(R.string.section_on_device, dummySurveys))

    var confirmedDeleteId: String? = null

    composeTestRule.setContent {
      SurveySectionList(
        sectionData = sectionData,
        onConfirmDelete = { confirmedDeleteId = it },
        onCardClick = {},
      )
    }

    // Verify dialog title doesn't exist yet
    val dialogTitle =
      composeTestRule.activity.getString(R.string.remove_offline_access_warning_title)
    composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

    // Click menu icon on survey card
    val menuDescription = composeTestRule.activity.getString(R.string.more_options_icon_description)
    composeTestRule.onNodeWithContentDescription(menuDescription).performClick()

    // Dialog should be visible
    composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

    // Click confirm button
    val confirmButtonText =
      composeTestRule.activity.getString(R.string.remove_offline_access_warning_confirm_button)
    composeTestRule.onNodeWithText(confirmButtonText).performClick()

    // Callback should be invoked with survey id
    assertEquals("1", confirmedDeleteId)

    // Dialog should be dismissed
    composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()
  }

  @Test
  fun `Dismissing dialog does not delete survey`() {
    val dummySurveys =
      listOf(
        SurveyListItem("1", "Tree Survey", "Track tree growth", true, Survey.GeneralAccess.PUBLIC)
      )
    val sectionData = listOf(SurveySection(R.string.section_on_device, dummySurveys))

    var confirmedDeleteId: String? = null

    composeTestRule.setContent {
      SurveySectionList(
        sectionData = sectionData,
        onConfirmDelete = { confirmedDeleteId = it },
        onCardClick = {},
      )
    }

    val dialogTitle =
      composeTestRule.activity.getString(R.string.remove_offline_access_warning_title)

    // Click menu icon to open dialog
    val menuDescription = composeTestRule.activity.getString(R.string.more_options_icon_description)
    composeTestRule.onNodeWithContentDescription(menuDescription).performClick()

    // Asserts dialog visible
    composeTestRule.onNodeWithText(dialogTitle).assertIsDisplayed()

    // Click cancel button
    val dismissButtonText = composeTestRule.activity.getString(R.string.cancel)
    composeTestRule.onNodeWithText(dismissButtonText).performClick()

    // Dialog should be dismissed
    composeTestRule.onNodeWithText(dialogTitle).assertDoesNotExist()

    // Delete should not be called
    assertNull(confirmedDeleteId)
  }
}
