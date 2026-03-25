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
package org.groundplatform.android.ui.datacollection.tasks.date

import android.app.DatePickerDialog
import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionUiState
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.TaskPosition
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowDatePickerDialog
import java.text.SimpleDateFormat
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTaskFragmentTest : BaseTaskFragmentTest<DateTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(id = "task_1", index = 0, type = Task.Type.DATE, label = "Date label", isRequired = false)

  private val job = Job(id = "job1")

  private fun setupScreen() {
    whenever(dataCollectionViewModel.uiState)
      .thenReturn(
        MutableStateFlow(
          DataCollectionUiState.Ready(
            surveyId = "survey 1",
            job = job,
            loiName = "Loi 1",
            tasks = listOf(task),
            isAddLoiFlow = false,
            currentTaskId = task.id,
            position = TaskPosition(0, 0, 1),
          )
        )
      )
    whenever(dataCollectionViewModel.loiNameDialogOpen).thenReturn(mutableStateOf(false))

    val env = TaskScreenEnvironment(mock(), dataCollectionViewModel, mock(), mock(), mock(), mock())

    composeTestRule.setContent { DateTaskScreen(viewModel, env) }
  }

  @Test
  fun `displays task header correctly`() {
    setupTaskFragment(job, task)
    setupScreen()

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `Initial action buttons state when task is optional`() {
    setupTaskFragment(job, task)
    setupScreen()

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `Initial action buttons state when task is required`() {
    setupTaskFragment(job, task.copy(isRequired = true))
    setupScreen()

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `default response is empty`() {
    setupTaskFragment(job, task)
    setupScreen()

    composeTestRule
      .onNodeWithTag(DATE_TEXT_TEST_TAG)
      .assertIsDisplayed()
      .assertTextContains(getExpectedDateHint())

    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun `response when on user input`() {
    setupTaskFragment(job, task)
    setupScreen()

    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()

    val dialog = shadowOf(ShadowDatePickerDialog.getLatestDialog())
    assertThat(dialog).isNotNull()
  }

  @Test
  fun `selected date is visible on user input`() {
    setupTaskFragment(job, task)
    setupScreen()

    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()
    val dialog = ShadowDatePickerDialog.getLatestDialog() as DatePickerDialog
    assertThat(dialog.isShowing).isTrue()

    val hardcodedYear = 2024
    val hardcodedMonth = 9
    val hardcodedDay = 10

    dialog.datePicker.updateDate(hardcodedYear, hardcodedMonth, hardcodedDay)
    dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("10/10/24").assertIsDisplayed()
    runner().assertButtonIsEnabled("Next")
  }

  @Test
  fun `Clear button resets the response`() {
    setupTaskFragment(job, task)
    setupScreen()

    composeTestRule.onNodeWithTag(DATE_TEXT_TEST_TAG).performClick()
    val dialog = ShadowDatePickerDialog.getLatestDialog() as DatePickerDialog
    assertThat(dialog.isShowing).isTrue()

    val hardcodedYear = 2024
    val hardcodedMonth = 9
    val hardcodedDay = 10

    dialog.datePicker.updateDate(hardcodedYear, hardcodedMonth, hardcodedDay)
    dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("10/10/24").assertIsDisplayed()

    dialog.getButton(DatePickerDialog.BUTTON_NEUTRAL).performClick()

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText(getExpectedDateHint()).assertIsDisplayed()
    composeTestRule.onNodeWithText("10/10/24").assertIsNotDisplayed()
  }

  @Test
  fun `hint text is visible`() {
    setupTaskFragment(job, task)
    setupScreen()

    composeTestRule.onNodeWithText(getExpectedDateHint()).assertIsDisplayed()
  }

  private fun getExpectedDateHint(): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hint = (DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern().uppercase()
    assertThat(hint).isNotEmpty()
    return hint
  }
}
