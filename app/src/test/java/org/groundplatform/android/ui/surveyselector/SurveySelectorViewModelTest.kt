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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class SurveySelectorViewModelTest : BaseHiltTest() {

  @Mock lateinit var activateSurveyUseCase: ActivateSurveyUseCase
  @Mock lateinit var listAvailableSurveysUseCase: ListAvailableSurveysUseCase
  @Mock lateinit var removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase
  @Mock lateinit var userRepository: UserRepositoryInterface

  private lateinit var externalScope: CoroutineScope
  private lateinit var ioDispatcher: CoroutineDispatcher

  private lateinit var viewModel: SurveySelectorViewModel

  @Before
  override fun setUp() {
    super.setUp()
    externalScope = TestScope(testDispatcher)
    ioDispatcher = testDispatcher
    whenever(listAvailableSurveysUseCase()).thenReturn(flowOf(listOf(TEST_SURVEY)))
  }

  private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) {
    viewModel =
      SurveySelectorViewModel(
        activateSurveyUseCase,
        externalScope,
        ioDispatcher,
        listAvailableSurveysUseCase,
        removeOfflineSurveyUseCase,
        userRepository,
        savedStateHandle,
      )
  }

  @Test
  fun `uiState loads surveys`() = runWithTestDispatcher {
    createViewModel()

    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.isLoading).isFalse()
      assertThat(state.hasSurveys).isTrue()
    }
  }

  @Test
  fun `activateSurvey navigates on success`() = runWithTestDispatcher {
    createViewModel()
    whenever(activateSurveyUseCase("1")).thenReturn(true)

    viewModel.events.test {
      viewModel.activateSurvey("1")
      assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.NavigateToHome)
    }
  }

  @Test
  fun `activateSurvey shows error on failure`() = runWithTestDispatcher {
    createViewModel()
    val error = RuntimeException("Oops")
    whenever(activateSurveyUseCase("1")).thenThrow(error)

    viewModel.events.test {
      viewModel.activateSurvey("1")
      assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.ShowError(error))
    }
  }

  @Test
  fun `activateSurvey from deeplink works correctly`() = runWithTestDispatcher {
    val savedState = SavedStateHandle(mapOf("surveyId" to "deeplink-id"))
    whenever(activateSurveyUseCase("deeplink-id")).thenReturn(true)
    createViewModel(savedStateHandle = savedState)

    viewModel.events.test { assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.NavigateToHome) }
  }

  @Test
  fun `activateSurvey from deeplink shows error on failure`() = runWithTestDispatcher {
    val savedState = SavedStateHandle(mapOf("surveyId" to "bad-id"))
    val error = RuntimeException("activation failed")
    whenever(activateSurveyUseCase("bad-id")).thenThrow(error)
    createViewModel(savedStateHandle = savedState)

    viewModel.events.test {
      assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.ShowError(error))
    }
  }

  companion object {
    private val TEST_SURVEY =
      SurveyListItem(
        id = "1",
        title = "survey 1",
        description = "description 1",
        availableOffline = true,
        generalAccess = Survey.GeneralAccess.RESTRICTED,
      )
  }
}
