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
package com.google.android.ground.ui.datacollection.tasks.polygon

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.job.Style
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
import org.robolectric.shadows.ShadowDialog

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DrawAreaTaskFragmentTest :
  BaseTaskFragmentTest<DrawAreaTaskFragment, DrawAreaTaskViewModel>() {

  @BindValue @Mock override lateinit var dataCollectionViewModel: DataCollectionViewModel
  @Inject override lateinit var viewModelFactory: ViewModelFactory

  private val task =
    Task(
      id = "task_1",
      index = 0,
      type = Task.Type.DRAW_AREA,
      label = "Task for drawing a polygon",
      isRequired = false,
    )
  private val job = Job("job", Style("#112233"))

  @Test
  fun testHeader() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task)

    hasTaskViewWithoutHeader(task.label)
  }

  @Test
  fun testInfoCard_noValue() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task)

    infoCardHidden()
  }

  @Test
  fun testActionButtons() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task)

    assertFragmentHasButtons(
      ButtonAction.PREVIOUS,
      ButtonAction.SKIP,
      ButtonAction.UNDO,
      ButtonAction.NEXT,
      ButtonAction.ADD_POINT,
      ButtonAction.COMPLETE,
    )
  }

  @Test
  fun testActionButtons_whenTaskIsOptional() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task.copy(isRequired = false))

    buttonIsHidden("Next")
    buttonIsEnabled("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Add point")
    buttonIsHidden("Complete")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task.copy(isRequired = true))

    buttonIsHidden("Next")
    buttonIsHidden("Skip")
    buttonIsHidden(ButtonAction.UNDO)
    buttonIsEnabled("Add point")
    buttonIsHidden("Complete")
  }

  @Test
  fun testDrawArea_incompleteWhenTaskIsOptional() = runWithTestDispatcher {
    setupTaskFragment<DrawAreaTaskFragment>(job, task.copy(isRequired = false))
    // Dismiss the instructions dialog
    ShadowDialog.getLatestDialog().dismiss()

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)
    updateLastVertexAndAddPoint(COORDINATE_3)

    hasValue(
      DrawAreaTaskIncompleteResult(
        LineString(
          listOf(
            Coordinates(0.0, 0.0),
            Coordinates(10.0, 10.0),
            Coordinates(20.0, 20.0),
            Coordinates(20.0, 20.0),
          )
        )
      )
    )

    // Only "Undo" and "Add point" buttons should be visible.
    buttonIsHidden("Next")
    buttonIsHidden("Skip")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsEnabled("Add point")
    buttonIsHidden("Complete")
  }

  @Test
  fun testDrawArea() = runWithTestDispatcher {
    setupTaskFragment<DrawAreaTaskFragment>(job, task.copy(isRequired = false))
    // Dismiss the instructions dialog
    ShadowDialog.getLatestDialog().dismiss()

    updateLastVertexAndAddPoint(COORDINATE_1)
    updateLastVertexAndAddPoint(COORDINATE_2)
    updateLastVertexAndAddPoint(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)
    onView(withText("Complete")).perform(click())

    hasValue(
      DrawAreaTaskResult(
        Polygon(
          LinearRing(
            listOf(
              Coordinates(0.0, 0.0),
              Coordinates(10.0, 10.0),
              Coordinates(20.0, 20.0),
              Coordinates(0.0, 0.0),
            )
          )
        )
      )
    )

    // Only "Undo" and "Complete" buttons should be visible.
    buttonIsHidden("Next")
    buttonIsHidden("Skip")
    buttonIsEnabled(ButtonAction.UNDO)
    buttonIsHidden("Add point")
    buttonIsEnabled("Complete")
  }

  @Test
  fun `Instructions dialog is shown`() = runWithTestDispatcher {
    setupTaskFragment<DrawAreaTaskFragment>(job, task)
    assertThat(ShadowDialog.getLatestDialog()).isNotNull()
  }

  @Test
  fun `Instructions dialog is not shown if shown previously`() = runWithTestDispatcher {
    setupTaskFragment<DrawAreaTaskFragment>(job, task)

    viewModel.instructionsDialogShown = true
    ShadowDialog.reset()

    setupTaskFragment<DrawAreaTaskFragment>(job, task)

    assertThat(ShadowDialog.getLatestDialog()).isNull()
  }

  /** Overwrites the last vertex and also adds a new one. */
  private fun updateLastVertexAndAddPoint(coordinate: Coordinates) {
    updateLastVertex(coordinate, false)
    onView(withText("Add point")).perform(click())
  }

  /** Updates the last vertex of the polygon with the given vertex. */
  private fun updateLastVertex(coordinate: Coordinates, isNearFirstVertex: Boolean = false) {
    val threshold = DrawAreaTaskViewModel.DISTANCE_THRESHOLD_DP.toDouble()
    val distanceInPixels = if (isNearFirstVertex) threshold else threshold + 1
    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> distanceInPixels }
  }

  companion object {
    private val COORDINATE_1 = Coordinates(0.0, 0.0)
    private val COORDINATE_2 = Coordinates(10.0, 10.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)
    private val COORDINATE_4 = Coordinates(30.0, 30.0)
  }
}
