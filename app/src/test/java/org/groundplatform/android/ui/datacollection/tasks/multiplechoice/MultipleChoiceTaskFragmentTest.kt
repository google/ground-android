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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.model.submission.MultipleChoiceTaskData
import org.groundplatform.android.model.task.MultipleChoice
import org.groundplatform.android.model.task.Option
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MultipleChoiceTaskFragmentTest {

//  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
//  @get:Rule(order = 1) var composeTestRule = createAndroidComposeRule<MainActivity>()
//
//  @BindValue @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel
//  @Inject lateinit var viewModelFactory: ViewModelFactory
//
//  private val task =
//    Task(
//      id = "task_1",
//      index = 0,
//      type = Task.Type.MULTIPLE_CHOICE,
//      label = "Text label",
//      isRequired = false,
//      multipleChoice = MultipleChoice(persistentListOf(), MultipleChoice.Cardinality.SELECT_ONE),
//    )
//  private val options =
//    persistentListOf(
//      Option("option id 1", "code1", "Option 1"),
//      Option("option id 2", "code2", "Option 2"),
//    )
//
//  private lateinit var viewModel: MultipleChoiceTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//  }
//
//  private fun setupViewModel(task: Task) {
//    val mockViewModel = viewModelFactory.create(MultipleChoiceTaskViewModel::class.java)
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn mockViewModel
//    viewModel = (dataCollectionViewModel.getTaskViewModel(task) as MultipleChoiceTaskViewModel).apply { initialize(task) }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    setupViewModel(task)
//    composeTestRule.setContent {
//      MultipleChoiceTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel))
//    }
//  }
//
//  @Test
//  fun `fails when multiple choice is null`() = runTest {
//    assertThrows(IllegalStateException::class.java) { setupScreen(task.copy(multipleChoice = null)) }
//  }
//
//  @Test
//  fun `renders header`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
//  }
//
//  @Test
//  fun `renders SELECT_ONE options`() = runTest {
//    setupScreen(
//      task.copy(multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE))
//    )
//
//    runner().assertOptionsDisplayed("Option 1", "Option 2")
//  }
//
//  @Test
//  fun `renders SELECT_MULTIPLE options`() = runTest {
//    setupScreen(
//      task.copy(
//        multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
//      )
//    )
//
//    runner().assertOptionsDisplayed("Option 1", "Option 2")
//  }
//
//  @Test
//  fun `allows only one selection for SELECT_ONE cardinality`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
//    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))
//
//    runner()
//      .assertButtonIsDisabled("Next")
//      .selectOption("Option 1")
//      .selectOption("Option 2")
//      .assertButtonIsEnabled("Next")
//
//    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 2")))
//  }
//
//  @Test
//  fun `allows multiple selection for SELECT_MULTIPLE cardinality`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE)
//    setupScreen(task.copy(multipleChoice = multipleChoice))
//
//    runner().selectOption("Option 1").selectOption("Option 2").assertButtonIsEnabled("Next")
//
//    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("option id 1", "option id 2")))
//  }
//
//  @Test
//  fun `saves other text`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
//    setupTaskFragment<MultipleChoiceTaskFragment>(
//      job,
//      task.copy(multipleChoice = multipleChoice, isRequired = true),
//    )
//    val userInput = "User inputted text"
//
//    runner().selectOption("Other").inputOtherText(userInput).assertButtonIsEnabled("Next")
//
//    hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
//  }
//
//  @Test
//  fun `text over the character limit is invalid`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
//    setupTaskFragment<MultipleChoiceTaskFragment>(
//      job,
//      task.copy(multipleChoice = multipleChoice, isRequired = true),
//    )
//    val userInput = "a".repeat(Constants.TEXT_DATA_CHAR_LIMIT + 1)
//    // TODO: We should actually validate that the error toast is displayed after Next is clicked.
//    // Unfortunately, matching toasts with espresso is not straightforward, so we leave it at
//    // an explicit validation check for now.
//    runner().selectOption("Other").inputOtherText(userInput)
//    assertThat(viewModel.validate()).isEqualTo(R.string.text_task_data_character_limit)
//  }
//
//  @Test
//  fun `selects other option on text input and deselects other radio inputs`() =
//    runWithTestDispatcher {
//      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
//      setupScreen(task.copy(multipleChoice = multipleChoice))
//
//      val userInput = "A"
//
//      runner()
//        .selectOption("Option 1")
//        .assertOptionNotSelected("Other")
//        .inputOtherText(userInput)
//        .assertOptionNotSelected("Option 1")
//        .assertOptionSelected("Other")
//
//      hasValue(MultipleChoiceTaskData(multipleChoice, listOf("[ $userInput ]")))
//    }
//
//  @Test
//  fun `deselects other option on text clear and required`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
//    setupTaskFragment<MultipleChoiceTaskFragment>(
//      job,
//      task.copy(multipleChoice = multipleChoice, isRequired = true),
//    )
//
//    runner()
//      .assertOptionNotSelected("Other")
//      .inputOtherText("A")
//      .assertOptionSelected("Other")
//      .clearOtherText()
//      .assertOptionNotSelected("Other")
//  }
//
//  @Test
//  fun `no deselection of other option on text clear when not required`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
//    setupScreen(task.copy(multipleChoice = multipleChoice))
//
//    runner()
//      .assertOptionNotSelected("Other")
//      .inputOtherText("A")
//      .assertOptionSelected("Other")
//      .clearOtherText()
//      .assertOptionSelected("Other")
//  }
//
//  @Test
//  fun `no deselection of non-other selection when other is cleared`() = runWithTestDispatcher {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
//    setupTaskFragment<MultipleChoiceTaskFragment>(
//      job,
//      task.copy(multipleChoice = multipleChoice, isRequired = true),
//    )
//
//    runner()
//      .inputOtherText("A")
//      .selectOption("Option 1")
//      .assertOptionNotSelected("Other")
//      .assertOptionSelected("Option 1")
//      .clearOtherText()
//      .assertOptionSelected("Option 1")
//      .assertOptionNotSelected("Other")
//  }
//
//  @Test
//  fun `Initial action buttons state when task is optional`() = runTest {
//    setupScreen(task.copy(multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)))
//
//    assertFragmentHasButtons(
//      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
//      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
//      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
//    )
//  }
//
//  @Test
//  fun `Initial action buttons state when task is the first and optional`() = runTest {
//    setupTaskFragment<MultipleChoiceTaskFragment>(job, task, isFistPosition = true)
//
//    assertFragmentHasButtons(
//      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true),
//      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
//      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
//    )
//  }
//
//  @Test
//  fun `Initial action buttons state when it's the last task and optional`() = runTest {
//    setupTaskFragment<MultipleChoiceTaskFragment>(job, task, isLastPosition = true)
//
//    assertFragmentHasButtons(
//      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
//      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
//      ButtonActionState(ButtonAction.DONE, isEnabled = false, isVisible = true),
//    )
//  }
//
//  @Test
//  fun `Initial action buttons state when it's the first task and required`() = runTest {
//    setupTaskFragment<MultipleChoiceTaskFragment>(
//      job,
//      task.copy(isRequired = true),
//      isFistPosition = true,
//    )
//
//    assertFragmentHasButtons(
//      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = false, isVisible = true),
//      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
//      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
//    )
//  }
//
//  @Test
//  fun `hides skip button when option is selected`() = runTest {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
//    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))
//
//    runner().selectOption("Option 1").assertButtonIsHidden("Skip")
//  }
//
//  @Test
//  fun `no confirmation dialog shown when no data is entered and skipped`() = runTest {
//    val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE)
//    setupTaskFragment<MultipleChoiceTaskFragment>(job, task.copy(multipleChoice = multipleChoice))
//
//    runner().clickButton("Skip")
//
//    assertThat(ShadowAlertDialog.getShownDialogs().isEmpty()).isTrue()
//  }
//
//  @Test
//  fun `doesn't save response when other text is missing and task is required`() =
//    runWithTestDispatcher {
//      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_ONE, true)
//      setupTaskFragment<MultipleChoiceTaskFragment>(
//        job,
//        task.copy(multipleChoice = multipleChoice, isRequired = true),
//      )
//
//      runner().selectOption("Other").inputOtherText("").assertButtonIsDisabled("Next")
//
//      hasValue(null)
//    }
//
//  @Test
//  fun `doesn't save response when multiple options selected but other text is missing and task is required`() =
//    runWithTestDispatcher {
//      val multipleChoice = MultipleChoice(options, MultipleChoice.Cardinality.SELECT_MULTIPLE, true)
//      setupTaskFragment<MultipleChoiceTaskFragment>(
//        job,
//        task.copy(multipleChoice = multipleChoice, isRequired = true),
//      )
//
//      runner()
//        .selectOption("Option 1")
//        .selectOption("Option 2")
//        .selectOption("Other")
//        .assertButtonIsDisabled("Next")
//
//      hasValue(null)
//    }
}
