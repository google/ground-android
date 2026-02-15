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
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import java.text.SimpleDateFormat
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.components.ButtonActionState
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DateTaskFragmentTest : BaseTaskFragmentTest<DateTaskFragment, DateTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(id = "task_1", index = 0, type = Task.Type.DATE, label = "Date label", isRequired = false)

  private val job = Job(id = "job1")

  @Test
  fun `displays task header correctly`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `Initial action buttons state when task is optional`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `Initial action buttons state when task is required`() {
    setupTaskFragment<DateTaskFragment>(job, task.copy(isRequired = true))

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `default response is empty`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    composeTestRule
      .onNodeWithTag("dateInputText")
      .assertIsDisplayed()
      .assertTextContains(getExpectedDateHint())

    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun `response when on user input`() {
    setupTaskFragment<DateTaskFragment>(job, task)
    // NOTE: The task container layout is given 0dp height to allow Android's constraint system to
    // determine the appropriate height. Unfortunately, Espresso does not perform actions on views
    // with height zero, and it doesn't seem to repro constraint calculations. Force the view to
    // have a height of 1 to ensure the action performed below actually takes place.
    val view: View? = fragment.view?.findViewById(R.id.task_container)
    view?.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)

    assertThat(fragment.getDatePickerDialog()).isNull()
    composeTestRule.onNodeWithTag("dateInputText").performClick()
    assertThat(fragment.getDatePickerDialog()).isNotNull()
    assertThat(fragment.getDatePickerDialog()?.isShowing).isTrue()
  }

  @Test
  fun `selected date is visible on user input`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    val view: View? = fragment.view?.findViewById(R.id.task_container)
    view?.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    composeTestRule.onNodeWithTag("dateInputText").performClick()
    assertThat(fragment.getDatePickerDialog()?.isShowing).isTrue()

    val hardcodedYear = 2024
    val hardcodedMonth = 9
    val hardcodedDay = 10

    val datePickerDialog = fragment.getDatePickerDialog()
    datePickerDialog?.datePicker?.updateDate(hardcodedYear, hardcodedMonth, hardcodedDay)

    datePickerDialog?.getButton(DatePickerDialog.BUTTON_POSITIVE)?.performClick()

    composeTestRule.onNodeWithText("10/10/24").isDisplayed()
    runner().assertButtonIsEnabled("Next")
  }

  @Test
  fun `Clear button resets the response`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    val view: View? = fragment.view?.findViewById(R.id.task_container)
    view?.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
    composeTestRule.onNodeWithTag("dateInputText").performClick()
    assertThat(fragment.getDatePickerDialog()?.isShowing).isTrue()

    val hardcodedYear = 2024
    val hardcodedMonth = 9
    val hardcodedDay = 10

    val datePickerDialog = fragment.getDatePickerDialog()
    datePickerDialog?.datePicker?.updateDate(hardcodedYear, hardcodedMonth, hardcodedDay)

    datePickerDialog?.getButton(DatePickerDialog.BUTTON_POSITIVE)?.performClick()
    composeTestRule.onNodeWithText("10/10/24").isDisplayed()

    datePickerDialog?.getButton(DatePickerDialog.BUTTON_NEUTRAL)?.performClick()
    composeTestRule.onNodeWithText("10/10/24").isNotDisplayed()
  }

  @Test
  fun `hint text is visible`() {
    setupTaskFragment<DateTaskFragment>(job, task)

    composeTestRule.onNodeWithText(getExpectedDateHint()).isDisplayed()
  }

  private fun getExpectedDateHint(): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hint = (DateFormat.getDateFormat(context) as SimpleDateFormat).toPattern().uppercase()
    assertThat(hint).isNotEmpty()
    return hint
  }
}
