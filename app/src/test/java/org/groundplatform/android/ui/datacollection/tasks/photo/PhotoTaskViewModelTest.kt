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
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.di.UserMediaRepositoryModule
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.PhotoTaskData
import org.groundplatform.domain.model.task.Task
import org.groundplatform.domain.repository.UserMediaRepositoryInterface
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@UninstallModules(UserMediaRepositoryModule::class)
@RunWith(RobolectricTestRunner::class)
class PhotoTaskViewModelTest : BaseHiltTest() {

  @BindValue @Mock lateinit var userMediaRepository: UserMediaRepositoryInterface
  @BindValue @Mock lateinit var permissionsManager: PermissionsManager
  @Inject lateinit var viewModel: PhotoTaskViewModel

  override fun setUp() {
    super.setUp()
    setupViewModel()
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `onCaptureResult saves photo when result is true`() = runWithTestDispatcher {
    whenever(permissionsManager.obtainPermission(any())).thenReturn(Unit)
    val mockFile = mock<File>()
    doReturn(TEST_FILE_PATH).whenever(userMediaRepository).createImageFile(any())
    doReturn(TEST_URI).whenever(userMediaRepository).getUriForFile(TEST_FILE_PATH)
    whenever(mockFile.absolutePath).thenReturn(TEST_FILE_PATH.value)
    whenever(mockFile.name).thenReturn(TEST_FILE_NAME)

    viewModel.onTakePhoto()
    advanceUntilIdle()

    viewModel.taskTaskData.test {
      assertThat(awaitItem()).isNull()

      viewModel.onCaptureResult(true)
      advanceUntilIdle()

      val item = awaitItem()
      assertThat(item).isInstanceOf(PhotoTaskData::class.java)
      assertThat((item as PhotoTaskData).remoteFilename)
        .isEqualTo("user-media/surveys/survey_1/submissions/file.jpg")
    }

    verify(userMediaRepository).addImageToGallery("/path/to/file.jpg", "file.jpg")
    assertThat(viewModel.isAwaitingPhotoCapture.value).isFalse()
  }

  @Test
  fun `onCaptureResult emits error when finalizePhotoCapture throws Exception`() =
    runWithTestDispatcher {
      whenever(permissionsManager.obtainPermission(any())).thenReturn(Unit)
      val mockFile = mock<File>()
      doReturn(TEST_FILE_PATH).whenever(userMediaRepository).createImageFile(any())
      doReturn(TEST_URI).whenever(userMediaRepository).getUriForFile(TEST_FILE_PATH)
      whenever(mockFile.absolutePath).thenReturn(TEST_FILE_PATH.value)
      whenever(mockFile.name).thenReturn(TEST_FILE_NAME)

      viewModel.events.test {
        viewModel.onTakePhoto()
        assertThat(awaitItem()).isInstanceOf(PhotoTaskEvent.LaunchCamera::class.java)

        whenever(userMediaRepository.addImageToGallery(any(), any())).thenThrow(RuntimeException())

        viewModel.onCaptureResult(true)
        val event = awaitItem()
        assertThat(event).isInstanceOf(PhotoTaskEvent.ShowError::class.java)
        assertThat((event as PhotoTaskEvent.ShowError).errorType)
          .isEqualTo(PhotoTaskError.PHOTO_SAVE_FAILED)
      }
    }

  @Test
  fun `onTakePhoto emits LaunchCamera event`() = runWithTestDispatcher {
    whenever(permissionsManager.obtainPermission(any())).thenReturn(Unit)
    doReturn(TEST_FILE_PATH).whenever(userMediaRepository).createImageFile(any())
    val mockUri = mock<Uri>()
    whenever(mockUri.toString()).thenReturn(TEST_URI.value)
    doReturn(TEST_URI).whenever(userMediaRepository).getUriForFile(TEST_FILE_PATH)

    viewModel.events.test {
      viewModel.onTakePhoto()
      val event = awaitItem()
      assertThat(event).isInstanceOf(PhotoTaskEvent.LaunchCamera::class.java)
      assertThat((event as PhotoTaskEvent.LaunchCamera).uri).isEqualTo(mockUri)
    }
  }

  @Test
  fun clearResponse_clearsUriFlow() = runWithTestDispatcher {
    doReturn(TEST_URI).whenever(userMediaRepository).getDownloadUrl(any())

    viewModel.setValue(PhotoTaskData("path/photo.jpg"))
    advanceUntilIdle()
    assertThat(viewModel.uri.first()).isNotNull()

    viewModel.clearResponse()
    advanceUntilIdle()

    assertThat(viewModel.uri.first()).isEqualTo(Uri.EMPTY)
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
      "survey_1",
    )
  }

  private companion object {
    val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.PHOTO,
        label = "Task for capturing a photo",
        isRequired = false,
      )
    val JOB = Job("job", Style("#112233"))

    val TEST_FILE_PATH = UserMediaRepositoryInterface.MediaFilePath("/path/to/file.jpg")
    const val TEST_FILE_NAME = "file.jpg"
    val TEST_URI = UserMediaRepositoryInterface.MediaUri("content://test")
  }
}
