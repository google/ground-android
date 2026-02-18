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
package org.groundplatform.android.ui.datacollection.tasks.time

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import java.text.SimpleDateFormat
import javax.inject.Inject
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

// TODO: Add a test for selecting a time and verifying response.
// Issue URL: https://github.com/google/ground-android/issues/2134

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TimeTaskFragmentTest : BaseTaskFragmentTest<TimeTaskFragment, TimeTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(id = "task_1", index = 0, type = Task.Type.TIME, label = "Time label", isRequired = false)
  private val job = Job("job")

  @Test
  fun `displays task header correctly`() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun `response when default is empty`() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    composeTestRule
      .onNodeWithTag(TIME_TEXT_TEST_TAG)
      .assertIsDisplayed()
      .assertIsEnabled()
      .assertTextContains(getExpectedTimeHint())

    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun `response when on user input`() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    assertThat(fragment.getTimePickerDialog()).isNull()
    runner().assertButtonIsDisabled("Next")

    composeTestRule.onNodeWithTag(TIME_TEXT_TEST_TAG).performClick()

    assertThat(fragment.getTimePickerDialog()!!.isShowing).isTrue()
  }

  @Test
  fun `Initial action buttons state when task is optional`() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `Initial action buttons state when task is required`() {
    setupTaskFragment<TimeTaskFragment>(job, task.copy(isRequired = true))

    assertFragmentHasButtons(
      ButtonActionState(ButtonAction.PREVIOUS, isEnabled = true, isVisible = true),
      ButtonActionState(ButtonAction.SKIP, isEnabled = false, isVisible = false),
      ButtonActionState(ButtonAction.NEXT, isEnabled = false, isVisible = true),
    )
  }

  @Test
  fun `hint text is visible`() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    composeTestRule.onNodeWithText(getExpectedTimeHint()).isDisplayed()
  }

  private fun getExpectedTimeHint(): String {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val timeFormat = DateFormat.getTimeFormat(context)
    val hint =
      if (timeFormat is SimpleDateFormat) {
        timeFormat.toPattern().uppercase()
      } else {
        "HH:MM AM/PM"
      }
    assertThat(hint).isNotEmpty()
    return hint
  }
}
