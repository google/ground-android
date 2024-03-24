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

import android.view.View
import androidx.fragment.app.Fragment
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import app.cash.turbine.test
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.R
import com.google.android.ground.launchFragmentWithNavController
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.Value
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.hamcrest.core.IsNot.not
import org.mockito.kotlin.whenever

abstract class BaseTaskFragmentTest<F : AbstractTaskFragment<VM>, VM : AbstractTaskViewModel> :
  BaseHiltTest() {

  abstract val dataCollectionViewModel: DataCollectionViewModel
  abstract val viewModelFactory: ViewModelFactory

  lateinit var fragment: F
  lateinit var viewModel: VM

  protected fun hasTaskViewWithHeader(task: Task) {
    onView(withId(R.id.data_collection_header))
      .check(matches(withText(task.label)))
      .check(matches(isDisplayed()))
  }

  protected fun hasTaskViewWithoutHeader(label: String) {
    onView(withId(R.id.header_label)).check(matches(withText(label))).check(matches(isDisplayed()))
    onView(withId(R.id.header_icon)).check(matches(isDisplayed()))
  }

  protected fun infoCardHidden() {
    onView(withId(R.id.infoCard)).check(matches(not(isDisplayed())))
  }

  protected fun infoCardShown(title: String, value: String) {
    onView(withId(R.id.infoCard)).check(matches(isDisplayed()))
    onView(withId(R.id.card_title)).check(matches(withText(title)))
    onView(withId(R.id.card_value)).check(matches(withText(value)))
  }

  protected suspend fun hasValue(value: Value?) {
    viewModel.taskValue.test { assertThat(expectMostRecentItem()).isEqualTo(value) }
  }

  /** Asserts that the task fragment has the given list of buttons in the exact same order. */
  protected fun assertFragmentHasButtons(vararg buttonActions: ButtonAction) {
    // TODO: Also verify the visibility/state of the button
    assertThat(fragment.getButtons().keys).containsExactlyElementsIn(buttonActions)
    buttonActions.withIndex().forEach { (index, expected) ->
      val actual = fragment.getButtonsIndex()[index]
      assertWithMessage("Incorrect button order").that(actual).isEqualTo(expected)
    }
  }

  protected fun getButton(buttonAction: ButtonAction): View =
    fragment.getButtons()[buttonAction]!!.getView()

  protected fun buttonIsHidden(buttonAction: ButtonAction) {
    val button = getButton(buttonAction)
    assertThat(button.visibility).isEqualTo(View.GONE)
  }

  protected fun buttonIsHidden(buttonText: String) {
    onView(withText(buttonText)).check(matches(not(isDisplayed())))
  }

  protected fun buttonIsEnabled(buttonAction: ButtonAction) {
    val button = getButton(buttonAction)
    assertThat(buttonAction.type).isEqualTo(ButtonAction.Type.ICON)
    assertThat(button.visibility).isEqualTo(View.VISIBLE)
    assertThat(button.isEnabled).isEqualTo(true)
  }

  protected fun buttonIsEnabled(buttonText: String) {
    onView(withText(buttonText)).check(matches(isDisplayed())).check(matches(isEnabled()))
  }

  protected fun buttonIsDisabled(buttonText: String) {
    onView(withText(buttonText)).check(matches(isDisplayed())).check(matches(not(isEnabled())))
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
      }
    )
  }
}
