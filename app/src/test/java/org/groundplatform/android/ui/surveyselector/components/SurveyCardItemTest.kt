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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.testing.FakeDataGenerator.newSurveyListItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SurveyCardItemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `Displays restricted survey correctly`() {
    val item = newSurveyListItem(generalAccess = Survey.GeneralAccess.RESTRICTED)
    setContent(item, onCardClick = {}, menuClick = {})

    composeTestRule.onNodeWithText(getString(R.string.access_restricted)).assertIsDisplayed()
    assertAvailableOffline(false)
  }

  @Test
  fun `Displays unspecified access survey correctly`() {
    val item = newSurveyListItem(generalAccess = Survey.GeneralAccess.GENERAL_ACCESS_UNSPECIFIED)
    setContent(item, onCardClick = {}, menuClick = {})

    composeTestRule.onNodeWithText(getString(R.string.access_restricted)).assertIsDisplayed()
    assertAvailableOffline(false)
  }

  @Test
  fun `Displays unlisted survey correctly`() {
    val item = newSurveyListItem(generalAccess = Survey.GeneralAccess.UNLISTED)
    setContent(item, onCardClick = {}, menuClick = {})

    composeTestRule.onNodeWithText(getString(R.string.access_unlisted)).assertIsDisplayed()
    assertAvailableOffline(false)
  }

  @Test
  fun `Displays public survey correctly`() {
    val item = newSurveyListItem(generalAccess = Survey.GeneralAccess.PUBLIC)
    setContent(item, onCardClick = {}, menuClick = {})

    composeTestRule.onNodeWithText(getString(R.string.access_public)).assertIsDisplayed()
    assertAvailableOffline(false)
  }

  @Test
  fun `Displays offline survey correctly`() {
    val item = newSurveyListItem(availableOffline = true)
    setContent(item, onCardClick = {}, menuClick = {})

    assertAvailableOffline(true)
  }

  @Test
  fun `Card click invokes onCardClick with survey id`() {
    val item = newSurveyListItem()
    var clickedId: String? = null
    setContent(item, onCardClick = { clickedId = it })

    composeTestRule.onNodeWithText(item.title).performClick()

    assertThat(clickedId).isEqualTo(item.id)
  }

  @Test
  fun `Card has no click action when onCardClick is null`() {
    val item = newSurveyListItem()
    setContent(item, onCardClick = null)

    composeTestRule.onNodeWithText(item.title).assertHasNoClickAction()
  }

  @Test
  fun `Card has click action when onCardClick is provided`() {
    val item = newSurveyListItem()
    setContent(item, onCardClick = {})

    composeTestRule.onNodeWithText(item.title).assertHasClickAction()
  }

  @Test
  fun `Menu icon is hidden when menuClick is null even if available offline`() {
    val item = newSurveyListItem(availableOffline = true)
    setContent(item, menuClick = null)

    composeTestRule
      .onNodeWithContentDescription(getString(R.string.more_options_icon_description))
      .assertIsNotDisplayed()
  }

  @Test
  fun `Menu icon click invokes menuClick with survey id`() {
    val item = newSurveyListItem(availableOffline = true)
    var clickedId: String? = null
    setContent(item, menuClick = { clickedId = it })

    composeTestRule
      .onNodeWithContentDescription(getString(R.string.more_options_icon_description))
      .performClick()

    assertThat(clickedId).isEqualTo(item.id)
  }

  @Test
  fun `Both callbacks null renders card without click actions or menu`() {
    val item = newSurveyListItem(availableOffline = true)
    setContent(item, onCardClick = null, menuClick = null)

    composeTestRule.onNodeWithText(item.title).assertHasNoClickAction()
    composeTestRule
      .onNodeWithContentDescription(getString(R.string.more_options_icon_description))
      .assertIsNotDisplayed()
  }

  private fun setContent(
    item: SurveyListItem,
    onCardClick: ((String) -> Unit)? = null,
    menuClick: ((String) -> Unit)? = null,
  ) {
    composeTestRule.setContent {
      SurveyCardItem(item = item, onCardClick = onCardClick, menuClick = menuClick)
    }
  }

  private fun assertAvailableOffline(isAvailable: Boolean) {
    composeTestRule
      .onNodeWithContentDescription(getString(R.string.offline_icon_description))
      .apply { if (isAvailable) assertIsDisplayed() else assertIsNotDisplayed() }

    composeTestRule
      .onNodeWithContentDescription(getString(R.string.more_options_icon_description))
      .apply { if (isAvailable) assertIsDisplayed() else assertIsNotDisplayed() }
  }
}
