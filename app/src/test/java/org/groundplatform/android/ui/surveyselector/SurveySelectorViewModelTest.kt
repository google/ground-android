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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.system.GmsQrCodeScanner
import org.groundplatform.android.usecases.survey.ActivateSurveyUseCase
import org.groundplatform.android.usecases.survey.ListAvailableSurveysUseCase
import org.groundplatform.android.usecases.survey.RemoveOfflineSurveyUseCase
import org.groundplatform.domain.model.Survey
import org.groundplatform.domain.model.SurveyListItem
import org.groundplatform.domain.repository.UserRepositoryInterface
import org.groundplatform.domain.usecases.survey.GetSurveyListItemUseCase
import org.groundplatform.domain.util.SurveyQrCodeParser
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
  @Mock lateinit var parseSurveyQrCodeUseCase: SurveyQrCodeParser
  @Mock lateinit var qrCodeScanner: GmsQrCodeScanner
  @Mock lateinit var removeOfflineSurveyUseCase: RemoveOfflineSurveyUseCase
  @Mock lateinit var getSurveyListItemUseCase: GetSurveyListItemUseCase
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
        qrCodeScanner,
        parseSurveyQrCodeUseCase,
        removeOfflineSurveyUseCase,
        getSurveyListItemUseCase,
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
      assertThat(awaitItem())
        .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.Generic(error)))
    }
  }

  @Test
  fun `activateSurvey emits Generic error when use case returns false`() = runWithTestDispatcher {
    createViewModel()
    whenever(activateSurveyUseCase("1")).thenReturn(false)

    viewModel.events.test {
      viewModel.activateSurvey("1")
      val event = awaitItem()
      assertThat(event).isInstanceOf(SurveySelectorEvent.ShowError::class.java)
      val errorType = (event as SurveySelectorEvent.ShowError).errorType
      assertThat(errorType).isInstanceOf(SurveySelectorEvent.ErrorType.Generic::class.java)
      assertThat((errorType as SurveySelectorEvent.ErrorType.Generic).cause)
    }
  }

  @Test
  fun `activateSurvey emits Timeout when use case throws TimeoutCancellationException`() =
    runWithTestDispatcher {
      createViewModel()
      val timeout = assertFailsWith<TimeoutCancellationException> { withTimeout(1) { delay(2) } }
      whenever(activateSurveyUseCase("1")).thenThrow(timeout)

      viewModel.events.test {
        viewModel.activateSurvey("1")
        assertThat(awaitItem())
          .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.Timeout))
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
      assertThat(awaitItem())
        .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.Generic(error)))
    }
  }

  @Test
  fun `joinSurveyByQrCode requests confirmation for parsed survey`() = runWithTestDispatcher {
    val payload = "https://groundplatform.org/android/survey/xyz"
    whenever(getSurveyListItemUseCase(TEST_SURVEY.id)).thenReturn(TEST_SURVEY)
    createViewModel()
    whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Success(payload))
    whenever(parseSurveyQrCodeUseCase(payload)).thenReturn(TEST_SURVEY.id)

    viewModel.joinSurveyByQrCode()
    val state = viewModel.uiState.first { it.pendingJoinSurvey != null }
    assertThat(state.pendingJoinSurvey).isEqualTo(TEST_SURVEY)
  }

  @Test
  fun `confirmJoinSurvey activates pending survey and clears confirmation`() =
    runWithTestDispatcher {
      val payload = "https://groundplatform.org/android/survey/xyz"
      whenever(getSurveyListItemUseCase(TEST_SURVEY.id)).thenReturn(TEST_SURVEY)
      createViewModel()
      whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Success(payload))
      whenever(parseSurveyQrCodeUseCase(payload)).thenReturn(TEST_SURVEY.id)
      whenever(activateSurveyUseCase(TEST_SURVEY.id)).thenReturn(true)

      viewModel.events.test {
        viewModel.joinSurveyByQrCode()
        viewModel.confirmJoinSurvey()
        assertThat(awaitItem()).isEqualTo(SurveySelectorEvent.NavigateToHome)
      }
      assertThat(viewModel.uiState.value.pendingJoinSurvey).isNull()
    }

  @Test
  fun `dismissJoinSurveyConfirmation clears pending survey without activating`() =
    runWithTestDispatcher {
      val payload = "https://groundplatform.org/android/survey/xyz"
      whenever(getSurveyListItemUseCase(TEST_SURVEY.id)).thenReturn(TEST_SURVEY)
      createViewModel()
      whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Success(payload))
      whenever(parseSurveyQrCodeUseCase(payload)).thenReturn(TEST_SURVEY.id)

      viewModel.events.test {
        viewModel.joinSurveyByQrCode()
        viewModel.dismissJoinSurveyConfirmation()
        expectNoEvents()
      }
      assertThat(viewModel.uiState.value.pendingJoinSurvey).isNull()
    }

  @Test
  fun `joinSurveyByQrCode emits invalid event when survey cannot be loaded`() =
    runWithTestDispatcher {
      val payload = "https://groundplatform.org/android/survey/missing"
      createViewModel()
      whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Success(payload))
      whenever(parseSurveyQrCodeUseCase(payload)).thenReturn("missing")

      viewModel.events.test {
        viewModel.joinSurveyByQrCode()
        assertThat(awaitItem())
          .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.InvalidQrCode))
      }
    }

  @Test
  fun `joinSurveyByQrCode emits invalid event for bad payload`() = runWithTestDispatcher {
    createViewModel()
    whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Success("not a url"))
    whenever(parseSurveyQrCodeUseCase("not a url")).thenReturn(null)

    viewModel.events.test {
      viewModel.joinSurveyByQrCode()
      assertThat(awaitItem())
        .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.InvalidQrCode))
    }
  }

  @Test
  fun `joinSurveyByQrCode is silent on cancellation`() = runWithTestDispatcher {
    createViewModel()
    whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Cancelled)

    viewModel.events.test {
      viewModel.joinSurveyByQrCode()
      expectNoEvents()
    }
  }

  @Test
  fun `joinSurveyByQrCode emits generic error when there's a problem scanning`() =
    runWithTestDispatcher {
      createViewModel()
      val error = RuntimeException("camera unavailable")
      whenever(qrCodeScanner.scan()).thenReturn(GmsQrCodeScanner.Result.Error(error))

      viewModel.events.test {
        viewModel.joinSurveyByQrCode()
        assertThat(awaitItem())
          .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.Generic(error)))
      }
    }

  @Test
  fun `surveyList failure emits Generic error event`() = runWithTestDispatcher {
    val error = RuntimeException()
    whenever(listAvailableSurveysUseCase()).thenReturn(flow { throw error })
    createViewModel()

    viewModel.events.test {
      viewModel.uiState.test {
        awaitItem()
        cancelAndIgnoreRemainingEvents()
      }
      assertThat(awaitItem())
        .isEqualTo(SurveySelectorEvent.ShowError(SurveySelectorEvent.ErrorType.Generic(error)))
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
