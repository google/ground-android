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
package org.groundplatform.android.ui.surveyselector

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.testing.FakeDataGenerator.newSurveyListItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SurveySelectorScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val recordedQrJoinActions = mutableListOf<QrJoinAction>()

  @Test
  fun `toolbar is displayed`() {
    setScreenContent(SurveySelectorUiState())

    composeTestRule.onNodeWithText(getString(R.string.surveys)).assertIsDisplayed()
  }

  @Test
  fun `empty state is shown when no surveys and not loading`() {
    setScreenContent(SurveySelectorUiState())

    composeTestRule.onNodeWithText(getString(R.string.no_surveys_available)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.sign_out)).assertIsDisplayed()
  }

  @Test
  fun `surveys are listed when present`() {
    setScreenContent(SurveySelectorUiState(publicSurveys = listOf(TEST_SURVEY)))

    composeTestRule.onNodeWithText(TEST_SURVEY.title).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.no_surveys_available)).assertIsNotDisplayed()
  }

  @Test
  fun `loading dialog is shown when isLoading is true`() {
    setScreenContent(SurveySelectorUiState(isLoading = true))

    composeTestRule.onNodeWithText(getString(R.string.loading)).assertIsDisplayed()
  }

  @Test
  fun `floating action button is hidden while loading`() {
    setScreenContent(SurveySelectorUiState(isLoading = true))

    composeTestRule.onNodeWithText(getString(R.string.join_survey)).assertIsNotDisplayed()
  }

  @Test
  fun `floating action button emits ScanQrCode action when clicked`() {
    setScreenContent(SurveySelectorUiState())

    composeTestRule
      .onNodeWithText(getString(R.string.join_survey), useUnmergedTree = true)
      .performClick()

    assertThat(recordedQrJoinActions).containsExactly(QrJoinAction.ScanQrCode)
  }

  @Test
  fun `show confirmation dialog when there is a pendingJoinSurvey`() {
    setScreenContent(SurveySelectorUiState(pendingJoinSurvey = TEST_SURVEY))

    composeTestRule
      .onNodeWithText(getString(R.string.join_survey_confirm_title))
      .assertIsDisplayed()
    composeTestRule.onNodeWithText(TEST_SURVEY.title).assertIsDisplayed()
  }

  @Test
  fun `clicking the ok button in confirmation dialog emits Confirm action`() {
    setScreenContent(SurveySelectorUiState(pendingJoinSurvey = TEST_SURVEY))

    composeTestRule.onNodeWithText(getString(R.string.ok)).performClick()

    assertThat(recordedQrJoinActions).containsExactly(QrJoinAction.Confirm)
  }

  @Test
  fun `clicking cancel button in confirmation dialog emits Dismiss action`() {
    setScreenContent(SurveySelectorUiState(pendingJoinSurvey = TEST_SURVEY))

    composeTestRule.onNodeWithText(getString(R.string.cancel)).performClick()

    assertThat(recordedQrJoinActions).containsExactly(QrJoinAction.Dismiss)
  }

  @Test
  fun `clicking on survey card triggers onCardClick`() {
    var clickedId: String? = null
    setScreenContent(
      uiState = SurveySelectorUiState(publicSurveys = listOf(TEST_SURVEY)),
      onCardClick = { clickedId = it },
    )

    composeTestRule.onNodeWithText(TEST_SURVEY.title).performClick()

    assertThat(clickedId).isEqualTo(TEST_SURVEY.id)
  }

  @Test
  fun `sign out triggers callback correctly`() {
    var signedOut = false
    setScreenContent(uiState = SurveySelectorUiState(), onSignOut = { signedOut = true })

    composeTestRule.onNodeWithText(getString(R.string.sign_out)).performClick()

    assertThat(signedOut).isTrue()
  }

  private fun setScreenContent(
    uiState: SurveySelectorUiState,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onConfirmDelete: (String) -> Unit = {},
    onCardClick: (String) -> Unit = {},
    onQrJoinAction: (QrJoinAction) -> Unit = { recordedQrJoinActions.add(it) },
  ) {
    composeTestRule.setContent {
      SurveySelectorScreen(
        uiState = uiState,
        onBack = onBack,
        onSignOut = onSignOut,
        onConfirmDelete = onConfirmDelete,
        onCardClick = onCardClick,
        onQrJoinAction = onQrJoinAction,
      )
    }
  }

  private companion object {
    val TEST_SURVEY = newSurveyListItem()
  }
}
