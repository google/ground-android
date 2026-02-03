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
@file:OptIn(ExperimentalCoroutinesApi::class)

package org.groundplatform.android.ui.datacollection.tasks.photo

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import java.io.File
import javax.inject.Inject
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.submission.PhotoTaskData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PhotoTaskViewModelTest : BaseHiltTest() {

  @BindValue @Mock lateinit var userMediaRepository: UserMediaRepository
  @Inject lateinit var viewModel: PhotoTaskViewModel

  @Mock private lateinit var mockFile: File

  override fun setUp() {
    super.setUp()
    setupViewModel()
  }

  @Test
  fun `createImageFileUri creates file and returns uri`() = runWithTestDispatcher {
    val mockUri = mock<Uri>()
    whenever(userMediaRepository.createImageFile(any())).thenReturn(mockFile)
    whenever(userMediaRepository.getUriForFile(mockFile)).thenReturn(mockUri)

    val uri = viewModel.createImageFileUri()

    assertThat(uri).isEqualTo(mockUri)
    verify(userMediaRepository).createImageFile(TASK.id)
    verify(userMediaRepository).getUriForFile(mockFile)
  }

  @Test
  fun `waitForPhotoCapture sets taskWaitingForPhoto`() {
    viewModel.waitForPhotoCapture(TASK.id)

    assertThat(viewModel.taskWaitingForPhoto).isEqualTo(TASK.id)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `onCaptureResult saves photo when result is true and uri is present`() =
    runWithTestDispatcher {
      val uri = mock<Uri>()
      viewModel.capturedUri = uri
      viewModel.taskWaitingForPhoto = TASK.id
      whenever(userMediaRepository.savePhotoFromUri(any(), any())).thenReturn(mockFile)
      whenever(mockFile.absolutePath).thenReturn("/path/to/file")
      whenever(mockFile.name).thenReturn("file.jpg")

      viewModel.onCaptureResult(true)
      advanceUntilIdle()

      verify(userMediaRepository).savePhotoFromUri(uri, TASK.id)
      verify(userMediaRepository).addImageToGallery("/path/to/file", "file.jpg")
      assertThat(viewModel.hasLaunchedCamera).isFalse()
    }

  @Test
  fun `onCaptureResult does nothing when result is false`() = runWithTestDispatcher {
    viewModel.onCaptureResult(false)

    verify(userMediaRepository, org.mockito.kotlin.never()).savePhotoFromUri(any(), any())
    assertThat(viewModel.hasLaunchedCamera).isFalse()
  }

  @Test
  fun `onCaptureResult does nothing when capturedUri is null`() = runWithTestDispatcher {
    viewModel.capturedUri = null

    viewModel.onCaptureResult(true)

    verify(userMediaRepository, org.mockito.kotlin.never()).savePhotoFromUri(any(), any())
    assertThat(viewModel.hasLaunchedCamera).isFalse()
  }

  @Test
  fun `Should have the correct action buttons in the proper order`() = runWithTestDispatcher {
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    assertThat(states.map { it.action })
      .containsExactly(
        ButtonAction.PREVIOUS,
        ButtonAction.UNDO,
        ButtonAction.SKIP,
        ButtonAction.NEXT,
      )
      .inOrder()
  }

  @Test
  fun `UNDO is not visible and NEXT is disabled when the photo is not taken yet`() =
    runWithTestDispatcher {
      advanceUntilIdle()

      val states = viewModel.taskActionButtonStates.first()

      with(requireNotNull(states.find { it.action == ButtonAction.UNDO })) {
        assertFalse(isVisible)
        assertFalse(isEnabled)
      }
      with(requireNotNull(states.find { it.action == ButtonAction.NEXT })) {
        assertTrue(isVisible)
        assertFalse(isEnabled)
      }
    }

  @Test
  fun `UNDO and NEXT are visible and enabled when the photo is present`() = runWithTestDispatcher {
    viewModel.setValue(PhotoTaskData("path/photo.jpg"))
    advanceUntilIdle()

    val states = viewModel.taskActionButtonStates.first()

    with(requireNotNull(states.find { it.action == ButtonAction.UNDO })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
    with(requireNotNull(states.find { it.action == ButtonAction.NEXT })) {
      assertTrue(isVisible)
      assertTrue(isEnabled)
    }
  }

  private fun setupViewModel(
    isTaskRequired: Boolean = false,
    isFirstTask: Boolean = false,
    isLastTaskWithValue: Boolean = false,
  ) {
    viewModel.initialize(
      JOB,
      TASK.copy(isRequired = isTaskRequired),
      null,
      object : TaskPositionInterface {
        override fun isFirst() = isFirstTask

        override fun isLastWithValue(taskData: TaskData?) = isLastTaskWithValue
      },
    )
    viewModel.surveyId = "survey_1"
  }

  companion object {
    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.PHOTO,
        label = "Task for capturing a photo",
        isRequired = false,
      )
    private val JOB = Job("job", Style("#112233"))
  }
}
