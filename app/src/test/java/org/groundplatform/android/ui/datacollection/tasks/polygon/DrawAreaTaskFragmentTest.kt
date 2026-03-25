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
package org.groundplatform.android.ui.datacollection.tasks.polygon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
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
import org.groundplatform.android.getString
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.tasks.TaskScreenEnvironment
import org.groundplatform.domain.model.geometry.Coordinates
import org.groundplatform.domain.model.geometry.LineString
import org.groundplatform.domain.model.geometry.LinearRing
import org.groundplatform.domain.model.geometry.Polygon
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
class DrawAreaTaskFragmentTest {

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
//      type = Task.Type.DRAW_AREA,
//      label = "Task for drawing a polygon",
//      isRequired = false,
//    )
//  private val job = Job("job", Style("#112233"))
//
//  private lateinit var viewModel: DrawAreaTaskViewModel
//
//  @Before
//  fun setup() {
//    hiltRule.inject()
//    localValueStore.drawAreaInstructionsShown = true
//  }
//
//  private fun setupViewModel(task: Task) {
//    val mockViewModel = viewModelFactory.create(DrawAreaTaskViewModel::class.java)
//    whenever(dataCollectionViewModel.getTaskViewModel(task)) doReturn mockViewModel
//    viewModel =
//      (dataCollectionViewModel.getTaskViewModel(task) as DrawAreaTaskViewModel).apply {
//        initialize(task)
//      }
//  }
//
//  private fun setupScreen(task: Task = this.task) {
//    setupViewModel(task)
//    composeTestRule.setContent {
//      DrawAreaTaskScreen(viewModel, TaskScreenEnvironment(dataCollectionViewModel))
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
//  fun `Initial action buttons state when task is optional`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.add_point_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.complete_button)).check(matches(isNotDisplayed()))
//  }
//
//  @Test
//  fun `Initial action buttons state when task is required`() = runTest {
//    setupScreen(task.copy(isRequired = true))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.prev_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.skip_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.undo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.add_point_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.complete_button)).check(matches(isNotDisplayed()))
//  }
//
//  @Test
//  fun `draw area when incomplete when task is optional`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    updateLastVertexAndAddPoint(COORDINATE_1)
//    updateLastVertexAndAddPoint(COORDINATE_2)
//    updateLastVertexAndAddPoint(COORDINATE_3)
//
//    assertThat(viewModel.taskTaskData.value)
//      .isEqualTo(
//        DrawAreaTaskIncompleteData(
//          LineString(
//            listOf(
//              Coordinates(0.0, 0.0),
//              Coordinates(10.0, 10.0),
//              Coordinates(20.0, 20.0),
//              Coordinates(20.0, 20.0), // Last vertex is duplicated
//            )
//          )
//        )
//      )
//
//    onView(withId(R.id.next_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.skip_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.undo_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.add_point_button))
//      .check(matches(enabled())) // Should be enabled to add more points
//    onView(withId(R.id.complete_button)).check(matches(isNotDisplayed()))
//  }
//
//  @Test
//  fun `draw area`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    updateLastVertexAndAddPoint(COORDINATE_1)
//    updateLastVertexAndAddPoint(COORDINATE_2)
//    updateLastVertexAndAddPoint(COORDINATE_3)
//    updateLastVertex(COORDINATE_1, true) // Close to the first vertex
//    composeTestRule.waitForIdle()
//
//    onView(withText(COMPLETE_POINT_BUTTON_TEXT)).perform(click())
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.skip_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.undo_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//    onView(withId(R.id.add_point_button)).check(matches(isNotDisplayed()))
//    onView(withId(R.id.next_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//
//    assertThat(viewModel.taskTaskData.value)
//      .isEqualTo(
//        DrawAreaTaskData(
//          Polygon(
//            LinearRing(
//              listOf(
//                Coordinates(0.0, 0.0),
//                Coordinates(10.0, 10.0),
//                Coordinates(20.0, 20.0),
//                Coordinates(0.0, 0.0),
//              )
//            )
//          )
//        )
//      )
//  }
//
//  @Test
//  fun `draw area when add point button disabled when too close`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.add_point_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//
//    updateLastVertexAndAddPoint(COORDINATE_1)
//    updateCloseVertex(COORDINATE_5) // Close to COORDINATE_1
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.add_point_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
//
//  @Test
//  fun `redo button when is visible`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//
//    updateLastVertexAndAddPoint(COORDINATE_1)
//    updateLastVertexAndAddPoint(COORDINATE_2)
//
//    viewModel.removeLastVertex() // This enables Redo
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//  }
//
//  @Test
//  fun `redo button when is disabled empty redo vertex stack`() = runTest {
//    setupScreen(task.copy(isRequired = false))
//    composeTestRule.waitForIdle()
//
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//
//    updateLastVertexAndAddPoint(COORDINATE_1)
//    updateLastVertexAndAddPoint(COORDINATE_2)
//
//    viewModel.removeLastVertex()
//    composeTestRule.waitForIdle()
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isEnabled())))
//
//    viewModel.removeLastVertex()
//    viewModel
//      .removeLastVertex() // Should be viewModel.undo() potentially multiple times to clear stack
//    composeTestRule.waitForIdle()
//    // Assuming state is now where redo is not possible
//    assertThat(viewModel.redoVertexStack).isEmpty()
//    onView(withId(R.id.redo_button)).check(matches(allOf(isDisplayed(), isNotEnabled())))
//  }
//
//  @Test
//  fun `Instructions dialog is not shown if shown previously`() = runTest {
//    // Instructions are shown by default
//    setupScreen(task)
//    composeTestRule.waitForIdle()
//    composeTestRule
//      .onNodeWithText(getString(R.string.draw_area_task_instruction))
//      .assertIsDisplayed()
//    composeTestRule.onNodeWithText("Close").performClick()
//    composeTestRule.waitForIdle()
//    assertThat(localValueStore.drawAreaInstructionsShown).isTrue()
//
//    // Re-setup screen
//    setupScreen(task)
//    composeTestRule.waitForIdle()
//
//    composeTestRule
//      .onNodeWithText(getString(R.string.draw_area_task_instruction))
//      .assertIsNotDisplayed()
//  }
//
//  /** Overwrites the last vertex and also adds a new one. */
//  private fun updateLastVertexAndAddPoint(coordinate: Coordinates) {
//    updateLastVertex(coordinate, false)
//    composeTestRule.waitForIdle()
//    onView(withText(ADD_POINT_BUTTON_TEXT)).perform(click())
//    composeTestRule.waitForIdle()
//  }
//
//  /** Updates the last vertex of the polygon with the given vertex. */
//  private fun updateLastVertex(coordinate: Coordinates, isNearFirstVertex: Boolean = false) {
//    val threshold = DrawAreaTaskViewModel.DISTANCE_THRESHOLD_DP.toDouble()
//    val distanceInPixels = if (isNearFirstVertex) threshold else threshold + 1
//    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> distanceInPixels }
//  }
//
//  /** Updates the last vertex of the polygon with the given vertex. */
//  private fun updateCloseVertex(coordinate: Coordinates) {
//    val threshold = DrawAreaTaskViewModel.DISTANCE_THRESHOLD_DP.toDouble()
//    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> threshold }
//  }

  companion object {
    private val COORDINATE_1 = Coordinates(0.0, 0.0)
    private val COORDINATE_2 = Coordinates(10.0, 10.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)
    private val COORDINATE_4 = Coordinates(30.0, 30.0)
    private val COORDINATE_5 = Coordinates(5.0, 5.0)

    private const val ADD_POINT_BUTTON_TEXT = "Add point"
    private const val NEXT_POINT_BUTTON_TEXT = "Next"
    private const val SKIP_POINT_BUTTON_TEXT = "Skip"
    private const val UNDO_POINT_BUTTON_TEXT = "Undo"
    private const val REDO_POINT_BUTTON_TEXT = "Redo"
    private const val COMPLETE_POINT_BUTTON_TEXT = "Complete"
  }
}
