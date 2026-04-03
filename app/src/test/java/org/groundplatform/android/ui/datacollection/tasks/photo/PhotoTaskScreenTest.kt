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
package org.groundplatform.android.ui.datacollection.tasks.photo

import android.Manifest.permission.CAMERA
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.groundplatform.android.repository.UserMediaRepository
import org.groundplatform.android.system.PermissionDeniedException
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.ButtonActionStateChecker
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenAction
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.PhotoTaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PhotoTaskScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Mock private lateinit var userMediaRepository: UserMediaRepository
  @Mock private lateinit var permissionsManager: PermissionsManager
  private lateinit var viewModel: PhotoTaskViewModel
  private var lastButtonAction: ButtonAction? = null
  private val buttonActionStateChecker = ButtonActionStateChecker(composeTestRule)

  @Before
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  private fun setupTaskScreen(
    task: Task,
    taskData: TaskData? = null,
    isFirst: Boolean = false,
    isLastWithValue: Boolean = false,
    onTakePhoto: () -> Unit = {},
  ) {
    lastButtonAction = null
    viewModel = PhotoTaskViewModel(userMediaRepository, permissionsManager)
    viewModel.initialize(
      job = JOB,
      task = task,
      taskData = taskData,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = isFirst

          override fun isLastWithValue(taskData: TaskData?) = isLastWithValue
        },
      surveyId = "survey_1",
    )

    composeTestRule.setContent {
      PhotoTaskScreen(
        viewModel = viewModel,
        onFooterPositionUpdated = {},
        onAction = {
          if (it is TaskScreenAction.OnButtonClicked) {
            lastButtonAction = it.action
          }
        },
        onAwaitingPhotoCapture = {},
      )
    }
  }

  @Test
  fun `shows capture button when photo is not present`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
    composeTestRule.onNodeWithContentDescription("Preview").assertIsNotDisplayed()
  }

  @Test
  fun `shows photo preview when photo is present`() {
    runBlocking {
      whenever(userMediaRepository.getDownloadUrl("test_file.jpg"))
        .thenReturn("content://media/external/images/media/1".toUri())
    }

    setupTaskScreen(TASK, PhotoTaskData("test_file.jpg"))

    composeTestRule.onNodeWithText("Camera").assertIsNotDisplayed()
    composeTestRule.onNodeWithContentDescription("Preview").assertIsDisplayed()
  }

  @Test
  fun `invokes onTakePhoto callback when capture button is clicked`() {
    setupTaskScreen(TASK)

    val file = File("test.jpg")
    val dummyUri = Uri.parse("content://test")
    runBlocking { whenever(userMediaRepository.createImageFile(TASK.id)).thenReturn(file) }
    whenever(userMediaRepository.getUriForFile(file)).thenReturn(dummyUri)

    composeTestRule.onNodeWithText("Camera").performClick()

    composeTestRule.waitForIdle()

    runBlocking { verify(permissionsManager).obtainPermission(CAMERA) }
  }

  @Test
  fun `displays task header correctly`() {
    setupTaskScreen(TASK)

    composeTestRule.onNodeWithText(TASK.label).assertIsDisplayed()
  }

  @Test
  fun `sets initial action buttons state when task is optional`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets initial action buttons state when task is required`() {
    setupTaskScreen(TASK.copy(isRequired = true))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when data is present`() {
    runBlocking {
      whenever(userMediaRepository.getDownloadUrl("test_file.jpg"))
        .thenReturn("content://media/external/images/media/1".toUri())
    }
    setupTaskScreen(TASK.copy(isRequired = true), PhotoTaskData("test_file.jpg"))

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.UNDO, isEnabled = true, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when it is the first task`() {
    setupTaskScreen(TASK, isFirst = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `sets action buttons state when it is the last task`() {
    setupTaskScreen(TASK, isLastWithValue = true)

    buttonActionStateChecker.assertButtonStates(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.DONE, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `invokes onButtonClicked when action button is clicked`() {
    setupTaskScreen(TASK)

    buttonActionStateChecker.getNode(ButtonAction.PREVIOUS).performClick()

    assertThat(lastButtonAction).isEqualTo(ButtonAction.PREVIOUS)
  }

  @Test
  fun `shows permission denied dialog when permission is denied`() {
    setupTaskScreen(TASK)

    runBlocking {
      whenever(permissionsManager.obtainPermission(CAMERA)).then {
        throw PermissionDeniedException("Permission denied")
      }
    }

    viewModel.onTakePhoto()

    composeTestRule.onNodeWithText("Permission denied").assertIsDisplayed()
  }

  companion object {
    private val JOB = Job("job", Style("#112233"))
    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.PHOTO,
        label = "Task for capturing a photo",
        isRequired = false,
      )
  }
}
