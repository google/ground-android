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
package org.groundplatform.android.ui.tos

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.R
import org.groundplatform.android.getString
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.testing.FakeTermsOfServiceRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TermsOfServiceScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Mock lateinit var authManager: AuthenticationManager
  private lateinit var fakeRepository: FakeTermsOfServiceRepository
  private lateinit var viewModel: TermsOfServiceViewModel

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    fakeRepository = FakeTermsOfServiceRepository()
    fakeRepository.delayMs = 0
  }

  private fun setupViewModel(result: Result<TermsOfService?> = Result.success(TEST_TOS)) {
    fakeRepository.termsOfService = result
    viewModel = TermsOfServiceViewModel(authManager, fakeRepository)
  }

  private fun setScreenContent(
    isViewOnly: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onNavigateToSurveySelector: () -> Unit = {},
    onError: () -> Unit = {},
  ) {
    composeTestRule.setContent {
      TermsOfServiceScreen(
        isViewOnly = isViewOnly,
        onNavigateUp = onNavigateUp,
        onNavigateToSurveySelector = onNavigateToSurveySelector,
        onLoadError = onError,
        termsContent = { html -> Text(html) },
        viewModel = viewModel,
      )
    }
  }

  @Test
  fun `Initial state displays loading spinner`() = runTest {
    fakeRepository.delayMs = 1000
    setupViewModel()

    setScreenContent()

    composeTestRule.onNode(hasText(TEST_TOS_TEXT, substring = true)).assertIsNotDisplayed()
  }

  @Test
  fun `Success state displays terms text and controls`() = runTest {
    setupViewModel()

    setScreenContent()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value is TosUiState.Success
    }

    composeTestRule.onNode(hasText(TEST_TOS_TEXT, substring = true)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.agree_checkbox)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.agree_terms)).assertIsDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.agree_terms)).assertIsNotEnabled()
  }

  @Test
  fun `Checkbox click enables button`() = runTest {
    setupViewModel()
    setScreenContent()

    composeTestRule.onNodeWithText(getString(R.string.agree_checkbox)).performClick()

    composeTestRule.onNodeWithText(getString(R.string.agree_terms)).assertIsEnabled()
  }

  @Test
  fun `Button click triggers navigation`() = runTest {
    setupViewModel()
    var navigated = false
    setScreenContent(onNavigateToSurveySelector = { navigated = true })

    composeTestRule.onNodeWithText(getString(R.string.agree_checkbox)).performClick()
    composeTestRule.onNodeWithText(getString(R.string.agree_terms)).performClick()

    assert(navigated)
  }

  @Test
  fun `Error triggers callback`() = runTest {
    setupViewModel(Result.failure(Exception("Failed to load")))
    var errorOccurred = false

    setScreenContent(onError = { errorOccurred = true })

    assert(errorOccurred)
  }

  @Test
  fun `Toolbar is displayed`() = runTest {
    setupViewModel()

    setScreenContent()

    composeTestRule.onNodeWithText(getString(R.string.tos_title)).assertIsDisplayed()
  }

  @Test
  fun `Toolbar Back Arrow is displayed in View-Only mode`() = runTest {
    setupViewModel()

    setScreenContent(isViewOnly = true)

    composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
  }

  @Test
  fun `View only mode hides controls`() = runTest {
    setupViewModel()

    setScreenContent(isViewOnly = true)

    composeTestRule.onNodeWithText(getString(R.string.agree_checkbox)).assertIsNotDisplayed()
    composeTestRule.onNodeWithText(getString(R.string.agree_terms)).assertIsNotDisplayed()
  }

  companion object {
    private const val TEST_TOS_TEXT = "Sample Terms"
    val TEST_TOS = TermsOfService("1", TEST_TOS_TEXT)
  }
}
