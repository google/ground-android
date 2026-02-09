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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.di.coroutines.ApplicationScope
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.android.model.SurveyListItem
import org.groundplatform.android.proto.Survey
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
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

  @BindValue @Mock lateinit var activateSurveyUseCase: ActivateSurveyUseCase
  @BindValue @Mock lateinit var listAvailableSurveysUseCase: ListAvailableSurveysUseCase
  @BindValue @Mock lateinit var removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase
  @BindValue @Mock lateinit var userRepository: UserRepository

  @Mock lateinit var savedStateHandle: SavedStateHandle

  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher

  private lateinit var viewModel: SurveySelectorViewModel

  @Before
  override fun setUp() {
    super.setUp()
    whenever(listAvailableSurveysUseCase()).thenReturn(flowOf(listOf(TEST_SURVEY)))
    viewModel =
      SurveySelectorViewModel(
        activateSurveyUseCase,
        externalScope,
        ioDispatcher,
        listAvailableSurveysUseCase,
        removeOfflineSurveyUseCase,
        userRepository,
        SavedStateHandle(),
      )
  }

  @Test
  fun `uiState loads surveys`() = runWithTestDispatcher {
    viewModel.uiState.test {
      val state = awaitItem()
      assertThat(state.isLoading).isFalse()
      assertThat(state.hasSurveys).isTrue()
    }
  }

  @Test
  fun `activateSurvey navigates on success`() = runWithTestDispatcher {
    whenever(activateSurveyUseCase("1")).thenReturn(true)

    viewModel.events.test {
      viewModel.activateSurvey("1")
      assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.NavigateToHome)
    }
  }

  @Test
  fun `activateSurvey shows error on failure`() = runWithTestDispatcher {
    val error = RuntimeException("Oops")
    whenever(activateSurveyUseCase("1")).thenThrow(error)

    viewModel.events.test {
      viewModel.activateSurvey("1")
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
