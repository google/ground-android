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

package org.groundplatform.android.ui.datacollection.tasks.text

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TextTaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
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
  fun `text over the character limit is invalid`() = runWithTestDispatcher {
    setupTaskFragment<TextTaskFragment>(job, task)

    runner().inputText("a".repeat(Constants.TEXT_DATA_CHAR_LIMIT + 1))
    // TODO: We should actually validate that the error toast is displayed after Next is clicked.
    // Unfortunately, matching toasts with espresso is not straightforward, so we leave it at
    // an explicit validation check for now.
    assertThat(viewModel.validate()).isEqualTo(R.string.text_task_data_character_limit)
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
