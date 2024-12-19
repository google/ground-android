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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.MultipleChoiceTaskData
import com.google.android.ground.model.task.MultipleChoice
import com.google.android.ground.model.task.Option
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
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

    composeTestRule
      .onNodeWithTag(MultipleChoiceTestTags.MULTIPLE_CHOICE_LIST)
      .performScrollToNode(hasText("Option 1"))
      .performScrollToNode(hasText("Option 2"))
      .assertIsDisplayed()
  }

  @Test
  fun `allows only one selection for SELECT_ONE cardinality`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithText("Option 2").performClick()

    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 2")))
    runner().assertButtonIsEnabled("Next")
  }

  @Test
  fun `renders SELECT_MULTIPLE options`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(
        multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
      ),
    )

    composeTestRule
      .onNodeWithTag(MultipleChoiceTestTags.MULTIPLE_CHOICE_LIST)
      .performScrollToNode(hasText("Option 1"))
      .performScrollToNode(hasText("Option 2"))
      .assertIsDisplayed()
  }

  @Test
  fun `allows multiple selection for SELECT_MULTIPLE cardinality`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    composeTestRule.onNodeWithText("Option 1").performClick()
    composeTestRule.onNodeWithText("Option 2").performClick()

    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 1", "option id 2")))
    runner().assertButtonIsEnabled("Next")
  }

  @Test
  fun `saves other text`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )
    val userInput = "User inputted text"

    composeTestRule.onNodeWithText("Other").performClick()
    getOtherInputNode().assertIsDisplayed().performTextInput(userInput)

    composeTestRule.onNode(hasText("Other")).assertIsDisplayed()
    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
    runner().assertButtonIsEnabled("Next")
  }

  @Test
  fun `selects other option on text input and deselects other radio inputs`() =
    runWithTestDispatcher {
      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
      setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

      composeTestRule.onNodeWithText("Option 1").performClick()
      getRadioButtonNode("Other").assertIsNotSelected()

      val userInput = "A"
      getOtherInputNode().assertIsDisplayed().performTextInput(userInput)

      getRadioButtonNode("Option 1").assertIsNotSelected()
      getRadioButtonNode("Other").assertIsSelected()
      hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
    }

  @Test
  fun `deselects other option on text clear and required`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )

    getRadioButtonNode("Other").assertIsNotSelected()
    getOtherInputNode().performTextInput("A")
    getRadioButtonNode("Other").assertIsSelected()

    getOtherInputNode().performTextClearance()

    getRadioButtonNode("Other").assertIsNotSelected()
  }

  @Test
  fun `no deselection of other option on text clear when not required`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))

    getRadioButtonNode("Other").assertIsNotSelected()
    getOtherInputNode().performTextInput("A")
    getRadioButtonNode("Other").assertIsSelected()

    getOtherInputNode().performTextClearance()

    getRadioButtonNode("Other").assertIsSelected()
  }

  @Test
  fun `no deselection of non-other selection when other is cleared`() = runWithTestDispatcher {
    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
    setupTaskFragment<MultipleChoiceTaskFragment>(
      job,
      task.copy(multipleChoice = multipleChoice, isRequired = true),
    )

    getOtherInputNode().performTextInput("A")
    composeTestRule.onNodeWithText("Option 1").performClick()

    getRadioButtonNode("Other").assertIsNotSelected()
    getRadioButtonNode("Option 1").assertIsSelected()

    getOtherInputNode().performTextClearance()

    getRadioButtonNode("Option 1").assertIsSelected()
    getRadioButtonNode("Other").assertIsNotSelected()
  }

  @Test
  fun `renders action buttons`() {
    setupTaskFragment<MultipleChoiceTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.NEXT)
  }

  @Test
  fun `renders action buttons on last task`() {
    whenever(dataCollectionViewModel.isLastPosition(any())).thenReturn(true)
    whenever(dataCollectionViewModel.checkLastPositionWithTaskData(any(), eq(null)))
      .thenReturn(true)
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

    composeTestRule.onNodeWithText("Option 1").performClick()

    runner().assertButtonIsHidden("Skip")
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

      composeTestRule.onNodeWithText("Other").performClick()

      composeTestRule
        .onNodeWithTag(MultipleChoiceTestTags.OTHER_INPUT_TEXT)
        .assertIsDisplayed()
        .performTextInput("")

      hasValue(null)
      runner().assertButtonIsDisabled("Next")
    }

  @Test
  fun `doesn't save response when multiple options selected but other text is missing and task is required`() =
    runWithTestDispatcher {
      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
      setupTaskFragment<MultipleChoiceTaskFragment>(
        job,
        task.copy(multipleChoice = multipleChoice, isRequired = true),
      )

      composeTestRule.onNodeWithText("Option 1").performClick()
      composeTestRule.onNodeWithText("Option 2").performClick()
      composeTestRule.onNodeWithText("Other").performClick()
      getOtherInputNode().performTextClearance()

      hasValue(null)
      runner().assertButtonIsDisabled("Next")
    }

  private fun getRadioButtonNode(text: String) =
    composeTestRule.onNode(
      hasTestTag(MultipleChoiceTestTags.SELECT_MULTIPLE_RADIO) and hasAnySibling(hasText(text))
    )

  private fun getOtherInputNode() =
    composeTestRule.onNodeWithTag(MultipleChoiceTestTags.OTHER_INPUT_TEXT)
}
