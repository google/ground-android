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

import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.common.ViewModelFactory
import org.groundplatform.android.ui.datacollection.DataCollectionViewModel
import org.groundplatform.android.ui.datacollection.components.ButtonAction
import org.groundplatform.android.ui.datacollection.tasks.BaseTaskFragmentTest
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

    runner().assertInfoCardHidden()
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

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsEnabled("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Add point")
      .assertButtonIsHidden("Complete")
  }

  @Test
  fun testActionButtons_whenTaskIsRequired() {
    setupTaskFragment<DrawAreaTaskFragment>(job, task.copy(isRequired = true))

    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsHidden("Undo", true)
      .assertButtonIsEnabled("Add point")
      .assertButtonIsHidden("Complete")
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
      DrawAreaTaskIncompleteData(
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
    runner()
      .assertButtonIsHidden("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsEnabled("Undo", true)
      .assertButtonIsEnabled("Add point")
      .assertButtonIsHidden("Complete")
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

    runner()
      .clickButton("Complete")
      .assertButtonIsHidden("Next")
      .assertButtonIsHidden("Skip")
      .assertButtonIsEnabled("Undo", true)
      .assertButtonIsHidden("Add point")
      .assertButtonIsEnabled("Complete")

    hasValue(
      DrawAreaTaskData(
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

    runner().clickButton("Add point")
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
