/*
 * Copyright 2025 Google LLC
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
package org.groundplatform.android.ui.datacollection.tasks.instruction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.R
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.doReturn
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class InstructionTaskFragmentTest {

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
//      type = Task.Type.INSTRUCTIONS,
//      label = "Instruction label",
//      isRequired = true,
//    )
//
//  private lateinit var viewModel: InstructionTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//  }
//
//  private fun setupViewModel(task: Task) {
//    val mockViewModel = viewModelFactory.create(InstructionTaskViewModel::class.java)
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn mockViewModel
//    viewModel = (dataCollectionViewModel.getTaskViewModel(task) as InstructionTaskViewModel).apply { initialize(task) }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    setupViewModel(task)
//    composeTestRule.setContent {
//      InstructionTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel))
//    }
//  }
//
//  @Test
//  fun `action buttons`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//  }
//
//  @Test
//  fun `instructions text is displayed`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithText("Instruction label").assertIsDisplayed()
//  }
}
