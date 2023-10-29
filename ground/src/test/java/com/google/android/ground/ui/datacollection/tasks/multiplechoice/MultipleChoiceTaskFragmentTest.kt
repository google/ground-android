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
package com.google.android.ground.ui.datacollection.tasks.multiplechoice

import android.content.Context
import android.widget.RadioButton
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class MultipleChoiceTaskFragmentTest :
  BaseTaskFragmentTest<MultipleChoiceTaskFragment, MultipleChoiceTaskViewModel>() {

  @Inject @ApplicationContext lateinit var context: Context
  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.MULTIPLE_CHOICE,
      label = "Text label",
      isRequired = false,
      multipleChoice = MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE)
    )
  private val job = Job(id = "job1")

  private val options =
    persistentListOf(
      Option("option id 1", "code1", "Option 1"),
      Option("option id 2", "code2", "Option 2"),
    )

  @Test
  fun taskFails_whenMultipleChoiceIsNull() {
    assertThrows(NullPointerException::class.java) {
      setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = null))
    }
  }

  @Test
  fun testHeader() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun testMultipleChoice_whenSelectOne() {
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE))
    )

    onView(withId(R.id.select_option_list)).check(matches(allOf(isDisplayed(), hasChildCount(2))))
    onView(withText("Option 1"))
      .check(matches(allOf(isDisplayed(), instanceOf(RadioButton::class.java))))
  }

  @Test
  fun testMultipleChoice_whenSelectOne_click() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    onView(withText("Option 1")).perform(click())
    onView(withText("Option 2")).perform(click())

    hasTaskData(MultipleChoiceTaskData(multipleChoice, listOf("option id 2")))
    buttonIsEnabled("Next")
  }

  @Test
  fun testMultipleChoice_whenSelectMultiple() {
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(
        multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
      )
    )

    onView(withId(R.id.select_option_list)).check(matches(allOf(isDisplayed(), hasChildCount(2))))
    onView(withText("Option 1"))
      .check(matches(allOf(isDisplayed(), instanceOf(MaterialCheckBox::class.java))))
  }

  @Test
  fun testMultipleChoice_whenSelectMultiple_click() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    onView(withText("Option 1")).perform(click())
    onView(withText("Option 2")).perform(click())

    hasTaskData(MultipleChoiceTaskData(multipleChoice, listOf("option id 1", "option id 2")))
    buttonIsEnabled("Next")
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    hasButtons(ButtonAction.NEXT, ButtonAction.SKIP)
  }

  @Test
  fun testActionButtons_whenLastTask() {
    whenever(dataCollectionViewModel.isLastPosition(any())).thenReturn(true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    hasButtons(ButtonAction.SKIP, ButtonAction.DONE)
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(isRequired = false))

    buttonIsDisabled("Next")
    buttonIsEnabled("Skip")
  }

  @Test
  fun `Skip button gets hidden on selecting an option`() {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    onView(withText("Option 1")).perform(click())

    buttonIsHidden("Skip")
  }

  @Test
  fun testActionButtons_noDataEntered_skipButtonTapped_confirmationDialogIsNotShown() {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    onView(withText("Skip")).perform(click())
    assertThat(ShadowAlertDialog.getShownDialogs().isEmpty()).isTrue()
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(isRequired = true))

    buttonIsDisabled("Next")
    buttonIsHidden("Skip")
  }
}
