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
package org.groundplatform.android.ui.datacollection.tasks.point

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.R
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.map.CameraPosition
import org.groundplatform.android.model.submission.DropPinTaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.Point
import org.hamcrest.Matchers.allOf
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
class DropPinTaskFragmentTest {

//  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
//  @get:Rule(order = 1) var composeTestRule = createAndroidComposeRule<MainActivity>()
//
//  @BindValue @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel
//  @Inject lateinit var viewModelFactory: ViewModelFactory
//  @Inject lateinit var localValueStore: LocalValueStore
//
//  private val task =
//    Task(
//      id = "task_1",
//      index = 0,
//      type = Task.Type.DROP_PIN,
//      label = "Task for dropping a pin",
//      isRequired = false,
//    )
//  private val job = Job("job", Style("#112233"))
//
//  private lateinit var viewModel: DropPinTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//    // Disable the instructions dialog to prevent click jacking.
//    localValueStore.dropPinInstructionsShown = true
//  }
//
//  private fun setupViewModel(task: Task) {
//    val mockViewModel = viewModelFactory.create(DropPinTaskViewModel::class.java)
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn mockViewModel
//    viewModel =
//      (dataCollectionViewModel.getTaskViewModel(task) as DropPinTaskViewModel).apply {
//        initialize(task)
//      }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    setupViewModel(task)
//    composeTestRule.setContent {
//      DropPinTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel))
//    }
//  }
//
//  @Test
//  fun `header renders correctly`() = runTest {
//    setupScreen()
//    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
//  }
//
//  @Test
//  fun `drop pin button works`() = runTest {
//    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    viewModel.updateCameraPosition(testPosition)
//    composeTestRule.waitForIdle()
//
//    onView(withText("Drop pin")).perform(click())
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.drop_pin_button)).check(matches(isNotDisplayed()))
//
//    assertThat(viewModel.taskTaskData.value)
//      .isEqualTo(DropPinTaskData(Point(Coordinates(10.0, 20.0))))
//  }
//
//  @Test
//  fun `undo works`() = runTest {
//    val testPosition = CameraPosition(Coordinates(10.0, 20.0))
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    viewModel.updateCameraPosition(testPosition)
//    composeTestRule.waitForIdle()
//
//    onView(withText("Drop pin")).perform(click())
//    composeTestRule.waitForIdle()
//    onView(withText("Undo")).perform(click())
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.drop_pin_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    assertThat(viewModel.taskTaskData.value).isNull()
//  }
//
//  @Test
//  fun `Initial action buttons state when task is optional`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.drop_pin_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//  }
//
//  @Test
//  fun `Initial action buttons state when task is required`() = runTest {
//    setupScreen(task.copy(isRequired = true))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.undo_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.drop_pin_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//  }
}
