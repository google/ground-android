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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.system.auth.AuthenticationManager
import org.groundplatform.domain.model.TermsOfService
import org.groundplatform.testing.FakeTermsOfServiceRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class TermsOfServiceViewModelTest {

  @Mock lateinit var authManager: AuthenticationManager
  private lateinit var fakeRepository: FakeTermsOfServiceRepository
  private lateinit var viewModel: TermsOfServiceViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    MockitoAnnotations.openMocks(this)
    fakeRepository = FakeTermsOfServiceRepository()
    fakeRepository.delayMs = 0
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun setupViewModel(isViewOnly: Boolean = false) = runTest {
    val savedStateHandle = SavedStateHandle(mapOf("isViewOnly" to isViewOnly))
    viewModel = TermsOfServiceViewModel(authManager, fakeRepository, savedStateHandle)
    advanceUntilIdle()
  }

  private fun assertSuccessState(isViewOnly: Boolean, termsText: String = "") {
    val actual = viewModel.uiState.value
    val termsHtml = if (termsText.isEmpty()) "<body></body>" else "<body><p>$termsText</p></body>"
    val expected = TosUiState.Success(termsHtml, agreeChecked = false, isViewOnly = isViewOnly)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `Initial state is Loading with correct isViewOnly`() = runTest {
    setupViewModel(isViewOnly = true)
    assertSuccessState(isViewOnly = true)
  }

  @Test
  fun `Success state contains terms and isViewOnly`() = runTest {
    fakeRepository.termsOfService = Result.success(TEST_TOS)
    setupViewModel(isViewOnly = false)
    assertSuccessState(isViewOnly = false, TERMS_TEXT)
  }

  @Test
  fun `Success state with isViewOnly true`() = runTest {
    fakeRepository.termsOfService = Result.success(TEST_TOS)
    setupViewModel(isViewOnly = true)
    assertSuccessState(isViewOnly = true, TERMS_TEXT)
  }

  @Test
  fun `onAgreeButtonClicked triggers NavigateToSurveySelector`() = runTest {
    setupViewModel()

    viewModel.events.test {
      viewModel.onAgreeButtonClicked()
      val event = awaitItem()
      assertThat(event).isInstanceOf(TosEvent.NavigateToSurveySelector::class.java)
    }
  }

  @Test
  fun `Load failure triggers LoadError event and signs out`() = runTest {
    fakeRepository.termsOfService = Result.failure(Exception("Failed to load"))
    setupViewModel()

    viewModel.events.test {
      advanceUntilIdle()
      val event = awaitItem()
      assertThat(event).isInstanceOf(TosEvent.LoadError::class.java)
    }

    verify(authManager).signOut()
  }

  companion object {
    private const val TERMS_TEXT = "Terms content"
    private val TEST_TOS = TermsOfService("1", TERMS_TEXT)
  }
}
