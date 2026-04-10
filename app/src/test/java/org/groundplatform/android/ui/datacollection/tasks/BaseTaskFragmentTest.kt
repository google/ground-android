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

package org.groundplatform.android.ui.datacollection.tasks

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.fragment.app.Fragment
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.testrules.FragmentScenarioRule
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.TaskFragmentRunner
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.domain.model.job.Job
import org.groundplatform.domain.model.submission.TaskData
import org.groundplatform.domain.model.task.Task
import org.junit.Rule
import org.mockito.kotlin.whenever

abstract class BaseTaskFragmentTest<F : AbstractTaskFragment<VM>, VM : AbstractTaskViewModel> :
  BaseHiltTest() {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  @get:Rule val fragmentScenario = FragmentScenarioRule()

  abstract val dataCollectionViewModel: DataCollectionViewModel
  abstract val viewModelFactory: ViewModelFactory

  lateinit var fragment: F
  lateinit var viewModel: VM

  protected fun runner() = TaskFragmentRunner(this, composeTestRule)

  protected fun hasTaskViewWithHeader(task: Task) {
    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
  }

  protected fun hasTaskViewWithoutHeader(label: String) {
    composeTestRule.onNodeWithText(label).assertIsDisplayed()
  }

  protected suspend fun hasValue(taskData: TaskData?) {
    viewModel.taskTaskData.test { assertThat(expectMostRecentItem()).isEqualTo(taskData) }
  }

  protected fun assertFragmentHasButtons(vararg buttonStates: ButtonActionState) {
    ButtonActionStateChecker(composeTestRule).assertButtonStates(*buttonStates)
  }

  protected inline fun <reified T : Fragment> setupTaskFragment(
    job: Job,
    task: Task,
    isFistPosition: Boolean = false,
    isLastPosition: Boolean = false,
    isTaskActive: Boolean = true,
  ) {
    viewModel = viewModelFactory.create(DataCollectionViewModel.getViewModelClass(task.type)) as VM
    viewModel.initialize(
      job = job,
      task = task,
      taskData = null,
      taskPositionInterface =
        object : TaskPositionInterface {
          override fun isFirst() = isFistPosition

          override fun isLastWithValue(taskData: TaskData?) = isLastPosition
        },
      surveyId = "survey_id",
    )
    whenever(dataCollectionViewModel.getTaskViewModel(task.id)).thenReturn(viewModel)
    whenever(dataCollectionViewModel.isCurrentActiveTaskFlow(task.id))
      .thenReturn(flowOf(isTaskActive))

    fragmentScenario.launchFragmentWithNavController<T>(
      destId = R.id.data_collection_fragment,
      preTransactionAction = {
        fragment = this as F
        fragment.taskId = task.id
      },
    )
  }
}
