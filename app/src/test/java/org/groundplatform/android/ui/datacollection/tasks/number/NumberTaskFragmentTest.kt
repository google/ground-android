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
package org.groundplatform.android.ui.datacollection.tasks.number

import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.NumberTaskData
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
class NumberTaskFragmentTest : BaseTaskFragmentTest<NumberTaskFragment, NumberTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.NUMBER,
      label = "Number label",
      isRequired = false,
    )
  private val job = Job("job1")

  @Test
  fun `displays task header correctly`() {
    setupTaskFragment<NumberTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `response when default is empty`() = runWithTestDispatcher {
    setupTaskFragment<NumberTaskFragment>(job, task)

    runner().assertInputNumberDisplayed("").assertButtonIsDisabled("Next")

    hasValue(null)
  }

  @Test
  fun `response when on user input next button is enabled`() = runWithTestDispatcher {
    setupTaskFragment<NumberTaskFragment>(job, task)

    runner().inputNumber(123.1).assertInputNumberDisplayed("123.1").assertButtonIsEnabled("Next")

    hasValue(NumberTaskData("123.1"))
  }

  @Test
  fun `deleting number resets the displayed text and next button`() = runWithTestDispatcher {
    setupTaskFragment<NumberTaskFragment>(job, task)

    runner()
      .inputNumber(129.2)
      .clearInputNumber()
      .assertInputNumberDisplayed("")
      .assertButtonIsDisabled("Next")

    hasValue(null)
  }

  @Test
  fun `action buttons`() {
    setupTaskFragment<NumberTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.NEXT)
  }

  @Test
  fun `action buttons when task is optional`() {
    setupTaskFragment<NumberTaskFragment>(job, task.copy(isRequired = false))

    runner().assertButtonIsDisabled("Next").assertButtonIsEnabled("Skip")
  }

  @Test
  fun `action buttons when task is required`() {
    setupTaskFragment<NumberTaskFragment>(job, task.copy(isRequired = true))

    runner().assertButtonIsDisabled("Next").assertButtonIsHidden("Skip")
  }
}
