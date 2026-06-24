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
package org.groundplatform.android.ui.datacollection

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.FakeData
import org.groundplatform.domain.model.task.Task
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DataCollectionViewModelTest : BaseHiltTest() {

  @Test
  fun `onBackClicked shows exit warning while loading`() = runWithTestDispatcher {
    val viewModel = setupViewModel(DataCollectionUiState.Loading)
    assertThat(viewModel.uiState.value).isEqualTo(DataCollectionUiState.Loading)

    viewModel.onBackClicked()

    assertThat(viewModel.showExitWarning.value).isTrue()
  }

  @Test
  fun `onBackClicked shows exit warning in error state`() = runWithTestDispatcher {
    val errorState = DataCollectionUiState.Error(DataCollectionErrorCode.SURVEY_LOAD_FAILED)
    val viewModel = setupViewModel(errorState)
    advanceUntilIdle()
    assertThat(viewModel.uiState.value).isEqualTo(errorState)

    viewModel.onBackClicked()

    assertThat(viewModel.showExitWarning.value).isTrue()
  }

  @Test
  fun `onBackClicked shows exit warning on the first task`() = runWithTestDispatcher {
    val viewModel = setupViewModel(READY_STATE)
    advanceUntilIdle()

    viewModel.onBackClicked()

    assertThat(viewModel.showExitWarning.value).isTrue()
  }

  @Test
  fun `onBackClicked emits exit effect when task is submitted`() = runWithTestDispatcher {
    val viewModel = setupViewModel(DataCollectionUiState.TaskSubmitted(null))
    advanceUntilIdle()

    viewModel.uiEffects.test {
      viewModel.onBackClicked()
      assertThat(awaitItem()).isEqualTo(DataCollectionUiEffect.Exit)
    }
  }

  @Test
  fun `onCloseClicked shows exit warning when not submitted`() = runWithTestDispatcher {
    val viewModel = setupViewModel(READY_STATE)
    advanceUntilIdle()

    viewModel.onCloseClicked()

    assertThat(viewModel.showExitWarning.value).isTrue()
  }

  @Test
  fun `onCloseClicked emits exit effect when task is submitted`() = runWithTestDispatcher {
    val viewModel = setupViewModel(DataCollectionUiState.TaskSubmitted(null))
    advanceUntilIdle()

    viewModel.uiEffects.test {
      viewModel.onCloseClicked()
      assertThat(awaitItem()).isEqualTo(DataCollectionUiEffect.Exit)
    }
    assertThat(viewModel.showExitWarning.value).isFalse()
  }

  private suspend fun setupViewModel(result: DataCollectionUiState): DataCollectionViewModel {
    val initializer =
      mock<DataCollectionInitializer>().apply {
        whenever(initialize(any(), any(), anyOrNull(), anyOrNull())) doReturn result
      }

    return DataCollectionViewModel(
      savedStateHandle = SavedStateHandle(mapOf("jobId" to "job1")),
      externalScope = CoroutineScope(testDispatcher),
      ioDispatcher = testDispatcher,
      submissionRepository = mock(),
      submitDataUseCase = mock(),
      offlineUuidGenerator = mock(),
      viewModelFactory = mock(),
      dataCollectionInitializer = initializer,
      getLoiReportUseCase = mock(),
      loiReportExporter = mock()
    )
  }

  private companion object {
    val READY_STATE =
      DataCollectionUiState.Ready(
        surveyId = "survey1",
        job = FakeData.JOB,
        loiName = "loi",
        tasks =
          listOf(
            Task(
              id = "taskId",
              index = 0,
              type = Task.Type.TEXT,
              label = "Task",
              isRequired = false,
            )
          ),
        isAddLoiFlow = false,
        currentTaskId = "taskId",
        position = TaskPosition(absoluteIndex = 0, relativeIndex = 0, sequenceSize = 1),
      )
  }
}
