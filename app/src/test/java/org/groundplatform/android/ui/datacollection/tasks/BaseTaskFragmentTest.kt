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

import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.R
import org.groundplatform.android.launchFragmentWithNavController
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.TaskFragmentRunner
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.util.view.isGone
import org.mockito.kotlin.whenever

abstract class BaseTaskFragmentTest<F : AbstractTaskFragment<VM>, VM : AbstractTaskViewModel> :
  BaseHiltTest() {

  abstract val dataCollectionViewModel: DataCollectionViewModel
  abstract val viewModelFactory: ViewModelFactory

  lateinit var fragment: F
  lateinit var viewModel: VM

  protected fun runner() = TaskFragmentRunner(this)

  protected fun hasTaskViewWithHeader(task: Task) {
    onView(withId(R.id.data_collection_header))
      .check(matches(withText(task.label)))
      .check(matches(isDisplayed()))
  }

  protected fun hasTaskViewWithoutHeader(label: String) {
    onView(withId(R.id.header_label)).check(matches(withText(label))).check(matches(isDisplayed()))
    onView(withId(R.id.header_icon)).check(matches(isDisplayed()))
  }

  protected fun hasNoTaskViewHeader() {
    onView(withId(R.id.data_collection_header)).check(matches(isGone()))
  }

  protected suspend fun hasValue(taskData: TaskData?) {
    viewModel.taskTaskData.test { assertThat(expectMostRecentItem()).isEqualTo(taskData) }
  }

  /** Asserts that the task fragment has the given list of buttons in the exact same order. */
  protected fun assertFragmentHasButtons(vararg buttonActions: ButtonAction) {
    // TODO: Also verify the visibility/state of the button
    // Issue URL: https://github.com/google/ground-android/issues/2134
    assertThat(fragment.buttonDataList.map { it.button.getAction() })
      .containsExactlyElementsIn(buttonActions)
    buttonActions.withIndex().forEach { (index, expected) ->
      val actual = fragment.buttonDataList[index].button.getAction()
      assertWithMessage("Incorrect button order").that(actual).isEqualTo(expected)
    }
  }

  protected inline fun <reified T : Fragment> setupTaskFragment(job: Job, task: Task) {
    viewModel = viewModelFactory.create(DataCollectionViewModel.getViewModelClass(task.type)) as VM
    viewModel.initialize(job, task, null)
    whenever(dataCollectionViewModel.getTaskViewModel(task.id)).thenReturn(viewModel)

    launchFragmentWithNavController<T>(
      destId = R.id.data_collection_fragment,
      preTransactionAction = {
        fragment = this as F
        fragment.taskId = task.id
      },
    )
  }
}
