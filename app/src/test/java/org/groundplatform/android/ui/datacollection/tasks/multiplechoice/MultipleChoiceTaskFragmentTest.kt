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
package org.groundplatform.android.ui.datacollection.tasks.multiplechoice

import android.content.Context
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import org.groundplatform.android.Config
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowAlertDialog

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
      multipleChoice = MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
    )
  private val job = Job(id = "job1")

  private val options =
    persistentListOf(
      Option("option id 1", "code1", "Option 1"),
      Option("option id 2", "code2", "Option 2"),
    )

  @Test
  fun `fails when multiple choice is null`() {
    assertThrows(IllegalStateException::class.java) {
      setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = null))
    }
  }

  @Test
  fun `renders header`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `renders SELECT_ONE options`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)),
    )

    runner().assertOptionsDisplayed("Option 1", "Option 2")
  }

  @Test
  fun `renders SELECT_MULTIPLE options`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(
        multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
      ),
    )

    runner().assertOptionsDisplayed("Option 1", "Option 2")
  }

  @Test
  fun `allows only one selection for SELECT_ONE cardinality`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    runner().selectOption("Option 1").selectOption("Option 2").assertButtonIsEnabled("Next")

    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 2")))
  }

  @Test
  fun `allows multiple selection for SELECT_MULTIPLE cardinality`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    runner().selectOption("Option 1").selectOption("Option 2").assertButtonIsEnabled("Next")

    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 1", "option id 2")))
  }

  @Test
  fun `saves other text`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )
    val userInput = "User inputted text"

    runner().selectOption("Other").inputOtherText(userInput).assertButtonIsEnabled("Next")

    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
  }

  @Test
  fun `text over the character limit is invalid`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )
    val userInput = "a".repeat(Config.TEXT_DATA_CHAR_LIMIT + 1)
    // TODO: We should actually validate that the error toast is displayed after Next is clicked.
    // Unfortunately, matching toasts with espresso is not straightforward, so we leave it at
    // an explicit validation check for now.
    runner().selectOption("Other").inputOtherText(userInput)
    assertThat(viewModel.validate()).isEqualTo(R.string.text_task_data_character_limit)
  }

  @Test
  fun `selects other option on text input and deselects other radio inputs`() =
    runWithTestDispatcher {
      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
      setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

      val userInput = "A"

      runner()
        .selectOption("Option 1")
        .assertOptionNotSelected("Other")
        .inputOtherText(userInput)
        .assertOptionNotSelected("Option 1")
        .assertOptionSelected("Other")

      hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
    }

  @Test
  fun `deselects other option on text clear and required`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )

    runner()
      .assertOptionNotSelected("Other")
      .inputOtherText("A")
      .assertOptionSelected("Other")
      .clearOtherText()
      .assertOptionNotSelected("Other")
  }

  @Test
  fun `no deselection of other option on text clear when not required`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    runner()
      .assertOptionNotSelected("Other")
      .inputOtherText("A")
      .assertOptionSelected("Other")
      .clearOtherText()
      .assertOptionSelected("Other")
  }

  @Test
  fun `no deselection of non-other selection when other is cleared`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )

    runner()
      .inputOtherText("A")
      .selectOption("Option 1")
      .assertOptionNotSelected("Other")
      .assertOptionSelected("Option 1")
      .clearOtherText()
      .assertOptionSelected("Option 1")
      .assertOptionNotSelected("Other")
  }

  @Test
  fun `renders action buttons`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.NEXT)
  }

  @Test
  fun `renders action buttons on last task`() {
    whenever(dataCollectionViewModel.isLastPosition(any())).thenReturn(true)
    whenever(dataCollectionViewModel.isLastPositionWithValue(any(), eq(null))).thenReturn(true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.DONE)
  }

  @Test
  fun `renders action buttons when first task and optional`() {
    whenever(dataCollectionViewModel.isFirstPosition(task.id)).thenReturn(true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(isRequired = false))

    runner()
      .assertButtonIsDisabled("Previous")
      .assertButtonIsDisabled("Next")
      .assertButtonIsEnabled("Skip")
  }

  @Test
  fun `hides skip button when option is selected`() {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    runner().selectOption("Option 1").assertButtonIsHidden("Skip")
  }

  @Test
  fun `no confirmation dialog shown when no data is entered and skipped`() {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    runner().clickButton("Skip")

    assertThat(ShadowAlertDialog.getShownDialogs().isEmpty()).isTrue()
  }

  @Test
  fun `renders action buttons when task is first and required`() {
    whenever(dataCollectionViewModel.isFirstPosition(task.id)).thenReturn(true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsDisabled("Previous")
      .assertButtonIsDisabled("Next")
      .assertButtonIsHidden("Skip")
  }

  @Test
  fun `renders action buttons when task is not first`() {
    whenever(dataCollectionViewModel.isFirstPosition(task.id)).thenReturn(false)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    runner()
      .assertButtonIsEnabled("Previous")
      .assertButtonIsDisabled("Next")
      .assertButtonIsEnabled("Skip")
  }

  @Test
  fun `doesn't save response when other text is missing and task is required`() =
    runWithTestDispatcher {
      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
      setupTaskFragment<MultipleChoiceTaskFragment>(
        job,
        task.copy(multipleChoice = multipleChoice, isRequired = true),
      )

      runner().selectOption("Other").inputOtherText("").assertButtonIsDisabled("Next")

      hasValue(null)
    }

  @Test
  fun `doesn't save response when multiple options selected but other text is missing and task is required`() =
    runWithTestDispatcher {
      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
      setupTaskFragment<MultipleChoiceTaskFragment>(
        job,
        task.copy(multipleChoice = multipleChoice, isRequired = true),
      )

      runner()
        .selectOption("Option 1")
        .selectOption("Option 2")
        .selectOption("Other")
        .assertButtonIsDisabled("Next")

      hasValue(null)
    }
}
