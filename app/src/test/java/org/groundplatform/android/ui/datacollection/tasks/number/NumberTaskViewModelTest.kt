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
package org.groundplatform.android.ui.datacollection.tasks.number

import com.google.common.truth.Truth.assertThat
import org.groundplatform.android.ui.datacollection.tasks.TaskPositionInterface
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.job.Style
import org.groundplatform.domain.model.submission.NumberTaskData
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Before
import org.junit.Test

class NumberTaskViewModelTest {

  private lateinit var viewModel: NumberTaskViewModel

  @Before
  fun setUp() {
    viewModel = NumberTaskViewModel()
    setupViewModel()
  }

  @Test
  fun `onInputChanged accepts a valid number`() {
    viewModel.onInputChanged("123.4")

    assertThat((viewModel.taskTaskData.value as NumberTaskData).number).isEqualTo("123.4")
  }

  @Test
  fun `onInputChanged rejects non-numeric input and keeps previous value`() {
    viewModel.onInputChanged("12")
    viewModel.onInputChanged("12a")

    assertThat((viewModel.taskTaskData.value as NumberTaskData).number).isEqualTo("12")
  }

  @Test
  fun `onInputChanged rejects a lone non-numeric character`() {
    viewModel.onInputChanged("-")

    assertThat(viewModel.taskTaskData.value).isNull()
  }

  @Test
  fun `onInputChanged clears the value on empty input`() {
    viewModel.onInputChanged("42")
    assertThat(viewModel.taskTaskData.value).isNotNull()

    viewModel.onInputChanged("")

    assertThat(viewModel.taskTaskData.value).isNull()
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
      "survey_id",
      eventReporter = {},
    )
  }

  private companion object {
    val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.NUMBER,
        label = "Number task",
        isRequired = false,
      )
    val JOB = Job("job", Style("#112233"))
  }
}