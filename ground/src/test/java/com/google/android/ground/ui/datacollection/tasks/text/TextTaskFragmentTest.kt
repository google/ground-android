/*
 * Copyright 2023 Google LLC
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

package com.google.android.ground.ui.datacollection.tasks.text

import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.TextTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TextTaskFragmentTest : BaseTaskFragmentTest<TextTaskFragment, TextTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(id = "task_1", index = 0, type = Task.Type.TEXT, label = "Text label", isRequired = false)
  private val job = Job("job")

  @Test
  fun testHeader() {
    setupTaskFragment<TextTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun testResponse_defaultIsEmpty() = runWithTestDispatcher {
    setupTaskFragment<TextTaskFragment>(job, task)

    runner().assertInputTextDisplayed("").assertButtonIsDisabled("Next")

    hasValue(null)
  }

  @Test
  fun `inserted text is displayed`() = runWithTestDispatcher {
    setupTaskFragment<TextTaskFragment>(job, task)

    runner().inputText("some text").assertInputTextDisplayed("some text")

    hasValue(TextTaskData("some text"))
  }

  @Test
  fun `deleting text resets the displayed text and next button`() = runWithTestDispatcher {
    setupTaskFragment<TextTaskFragment>(job, task)

    runner()
      .inputText("some text")
      .clearInputText()
      .assertInputTextDisplayed("")
      .assertButtonIsDisabled("Next")

    hasValue(null)
  }

  @Test
  fun testResponse_onUserInput_nextButtonIsEnabled() = runWithTestDispatcher {
    setupTaskFragment<TextTaskFragment>(job, task)

    runner().inputText("Hello world").clickNextButton()

    hasValue(TextTaskData("Hello world"))
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<TextTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.NEXT)
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<TextTaskFragment>(job, task.copy(isRequired = false))

    runner().assertButtonIsDisabled("Next").assertButtonIsEnabled("Skip")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<TextTaskFragment>(job, task.copy(isRequired = true))

    runner().assertButtonIsDisabled("Next").assertButtonIsHidden("Skip")
  }
}
