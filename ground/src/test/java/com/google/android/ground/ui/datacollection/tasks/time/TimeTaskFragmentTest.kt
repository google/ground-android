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
package com.google.android.ground.ui.datacollection.tasks.time

import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

// TODO: Add a test for selecting a time and verifying response.

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class TimeTaskFragmentTest : BaseTaskFragmentTest<TimeTaskFragment, TimeTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(id = "task_1", index = 0, type = Task.Type.TIME, label = "Time label", isRequired = false)
  private val job = Job("job")

  @Test
  fun testHeader() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun testResponse_defaultIsEmpty() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    onView(withId(R.id.user_response_text))
      .check(matches(withText("")))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))

    assertThat(viewModel.responseText.value).isEqualTo(null)

    runner().assertButtonIsDisabled("Next")
  }

  @Test
  fun testResponse_onUserInput() {
    setupTaskFragment<TimeTaskFragment>(job, task)
    // NOTE: The task container layout is given 0dp height to allow Android's constraint system to
    // determine the appropriate height. Unfortunately, Espresso does not perform actions on views
    // with height zero, and it doesn't seem to repro constraint calculations. Force the view to
    // have a height of 1 to ensure the action performed below actually takes place.
    val view: View? = fragment.view?.findViewById(R.id.task_container)
    view?.layoutParams = ViewGroup.LayoutParams(0, 1)

    assertThat(fragment.getTimePickerDialog()).isNull()
    onView(withId(R.id.user_response_text)).perform(click())
    assertThat(fragment.getTimePickerDialog()!!.isShowing).isTrue()
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<TimeTaskFragment>(job, task)

    assertFragmentHasButtons(ButtonAction.PREVIOUS, ButtonAction.SKIP, ButtonAction.NEXT)
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<TimeTaskFragment>(job, task.copy(isRequired = false))

    runner().assertButtonIsDisabled("Next").assertButtonIsEnabled("Skip")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<TimeTaskFragment>(job, task.copy(isRequired = true))

    runner().assertButtonIsDisabled("Next").assertButtonIsHidden("Skip")
  }
}
