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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.testing.FakeDataGenerator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class JoinSurveyDialogTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `displays all dialog elements correctly`() {
    setContent()

    composeTestRule
      .onNodeWithText(getString(R.string.join_survey_confirm_title))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY.description).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.cancel)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.ok)).assertIsDisplayed()
  }

  @Test
  fun `cancel button triggers onDismiss`() {
    var dismissed = false
    setContent(onDismiss = { dismissed = true })

    composeTestRule.onNodeWithText(getString(R.string.cancel)).performClick()

    assertThat(dismissed).isTrue()
  }

  @Test
  fun `ok button triggers onConfirm`() {
    var confirmed = false
    setContent(onConfirm = { confirmed = true })

    composeTestRule.onNodeWithText(getString(R.string.ok)).performClick()

    assertThat(confirmed).isTrue()
  }

  @Test
  fun `ok button does not trigger dismiss`() {
    var dismissed = false
    setContent(onDismiss = { dismissed = true })

    composeTestRule.onNodeWithText(getString(R.string.ok)).performClick()

    assertThat(dismissed).isFalse()
  }

  @Test
  fun `cancel button does not trigger confirm`() {
    var confirmed = false
    setContent(onConfirm = { confirmed = true })

    composeTestRule.onNodeWithText(getString(R.string.cancel)).performClick()

    assertThat(confirmed).isFalse()
  }

  private fun setContent(
    item: SurveyListItem = TEST_SURVEY,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      JoinSurveyDialog(surveyListItem = item, onDismiss = onDismiss, onConfirm = onConfirm)
    }
  }

  private companion object {
    private val TEST_SURVEY = FakeDataGenerator.newSurveyListItem()
  }
}
