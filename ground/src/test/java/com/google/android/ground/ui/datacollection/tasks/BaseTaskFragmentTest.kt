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

package com.google.android.ground.ui.datacollection.tasks

import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.hiltActivityScenario
import com.google.android.ground.launchFragment
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.NavControllerTestUtil
import com.google.common.truth.Truth.assertThat
import java8.util.Optional
import org.hamcrest.core.IsNot.not
import org.mockito.kotlin.whenever

abstract class BaseTaskFragmentTest<F : AbstractTaskFragment<VM>, VM : AbstractTaskViewModel> :
  BaseHiltTest() {

  abstract val dataCollectionViewModel: DataCollectionViewModel
  abstract val viewModelFactory: ViewModelFactory

  lateinit var fragment: F
  lateinit var viewModel: VM

  protected fun hasTaskViewWithHeader(task: Task) {
    onView(ViewMatchers.withId(R.id.data_collection_step_number))
      .check(matches(withText("Task ${task.index + 1}:")))
      .check(matches(isDisplayed()))
    onView(ViewMatchers.withId(R.id.data_collection_question))
      .check(matches(withText(task.label)))
      .check(matches(isDisplayed()))
  }

  protected fun hasButtonCount(expectedSize: Int) {
    assertThat(fragment.getButtons()).hasSize(expectedSize)
  }

  protected fun buttonIsHidden(buttonText: String) {
    onView(withText(buttonText)).check(matches(not(isDisplayed())))
  }

  protected fun buttonIsEnabled(buttonText: String) {
    onView(withText(buttonText)).check(matches(isDisplayed())).check(matches(isEnabled()))
  }

  protected fun buttonIsDisabled(buttonText: String) {
    onView(withText(buttonText)).check(matches(isDisplayed())).check(matches(not(isEnabled())))
  }

  protected inline fun <reified T : Fragment> setupTaskFragment(task: Task) {
    viewModel = viewModelFactory.create(DataCollectionViewModel.getViewModelClass(task.type)) as VM
    viewModel.initialize(task, Optional.empty())
    whenever(dataCollectionViewModel.getTaskViewModel(task.index)).thenReturn(viewModel)

    hiltActivityScenario()
      .launchFragment<T>(
        preTransactionAction = {
          fragment = this as F
          fragment.position = task.index

          viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
            if (viewLifecycleOwner != null) {
              // Bind the controller after the view is created but before onViewCreated is called
              Navigation.setViewNavController(
                fragment.requireView(),
                NavControllerTestUtil.createTestNavController()
              )
            }
          }
        }
      )
  }
}
