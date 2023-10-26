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
package com.google.android.ground.ui.datacollection.tasks.number

import android.text.InputType
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withInputType
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.CustomViewActions.forceTypeText
import com.google.android.ground.R
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.submission.NumberTaskData
import com.google.android.ground.model.task.Task
import com.google.android.ground.ui.common.ViewModelFactory
import com.google.android.ground.ui.datacollection.DataCollectionViewModel
import com.google.android.ground.ui.datacollection.components.ButtonAction
import com.google.android.ground.ui.datacollection.tasks.BaseTaskFragmentTest
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class NumberTaskFragmentTest : BaseTaskFragmentTest<NumberTaskFragment, NumberTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.NUMBER,
      label = "Number label",
      isRequired = false
    )
  private val job = Job("job1")

  @Test
  fun testHeader() {
    setupTaskFragment<NumberTaskFragment>(job, task)

    hasTaskViewWithHeader(task)
  }

  @Test
  fun testResponse_defaultIsEmpty() = runWithTestDispatcher {
    setupTaskFragment<NumberTaskFragment>(job, task)

    onView(withId(R.id.user_response_text))
      .check(matches(withText("")))
      .check(matches(isDisplayed()))
      .check(matches(isEnabled()))

    hasTaskData(null)
    buttonIsDisabled("Next")
  }

  @Test
  fun testResponse_onUserInput_nextButtonIsEnabled() = runWithTestDispatcher {
    setupTaskFragment<NumberTaskFragment>(job, task)

    onView(withId(R.id.user_response_text))
      .check(matches(withInputType(InputType.TYPE_CLASS_NUMBER)))
      .perform(forceTypeText("123"))

    hasTaskData(NumberTaskData("123"))
    buttonIsEnabled("Next")
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<NumberTaskFragment>(job, task)

    hasButtons(ButtonAction.NEXT, ButtonAction.SKIP)
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<NumberTaskFragment>(job, task.copy(isRequired = false))

    buttonIsDisabled("Next")
    buttonIsEnabled("Skip")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<NumberTaskFragment>(job, task.copy(isRequired = true))

    buttonIsDisabled("Next")
    buttonIsHidden("Skip")
  }
}
