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

package org.groundplatform.android.ui.datacollection.tasks.text

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.common.Constants
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.submission.TextTaskData
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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.test.runTest
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TextTaskFragmentTest {

//  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)
//  @get:Rule(order = 1) var composeTestRule = createAndroidComposeRule<MainActivity>()
//
//  @BindValue @Mock lateinit var dataCollectionViewModel: DataCollectionViewModel
//  @Inject lateinit var viewModelFactory: ViewModelFactory
//
//  private val task =
//    Task(id = "task_1", index = 0, type = Task.Type.TEXT, label = "Text label", isRequired = false)
//
//  private lateinit var viewModel: TextTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//  }
//
//  private fun setupViewModel(task: Task) {
//    val mockViewModel = viewModelFactory.create(TextTaskViewModel::class.java)
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn mockViewModel
//    viewModel = (dataCollectionViewModel.getTaskViewModel(task) as TextTaskViewModel).apply { initialize(task) }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    setupViewModel(task)
//    composeTestRule.setContent {
//      TextTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel))
//    }
//  }
//
//  @Test
//  fun `displays task header correctly`() = runTest {
//    setupScreen()
//    composeTestRule.onNodeWithText(task.label).assertIsDisplayed()
//  }
//
//  @Test
//  fun `response when default is empty`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).assertTextEquals("")
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//
//    assertThat(viewModel.taskTaskData.value).isNull()
//  }
//
//  @Test
//  fun `inserted text is displayed`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("some text")
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).assertTextEquals("some text")
//    assertThat(viewModel.taskTaskData.value).isEqualTo(TextTaskData("some text"))
//  }
//
//  @Test
//  fun `deleting text resets the displayed text and next button`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("some text")
//    composeTestRule.waitForIdle()
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("") // Clear text
//    composeTestRule.waitForIdle()
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).assertTextEquals("")
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//
//    assertThat(viewModel.taskTaskData.value).isNull()
//  }
//
//  @Test
//  fun `text over the character limit is invalid`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    val longText = "a".repeat(Constants.TEXT_DATA_CHAR_LIMIT + 1)
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput(longText)
//    composeTestRule.waitForIdle()
//
//    assertThat(viewModel.validate()).isEqualTo(R.string.text_task_data_character_limit)
//  }
//
//  @Test
//  fun `response when on user input next button is enabled`() = runTest {
//    setupScreen()
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//
//    composeTestRule.onNodeWithTag(INPUT_TEXT_TEST_TAG).performTextInput("Hello world")
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    assertThat(viewModel.taskTaskData.value).isEqualTo(TextTaskData("Hello world"))
//  }
//
//  @Test
//  fun `Initial action buttons state when task is optional`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
//
//  @Test
//  fun `Initial action buttons state when task is required`() = runTest {
//    setupScreen(task.copy(isRequired = true))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
}
