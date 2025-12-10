/*
 * Copyright 2021 Google LLC
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jraska.livedata.TestObserver
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.test.advanceUntilIdle
import org.groundplatform.android.BaseHiltTest
import org.groundplatform.android.data.local.LocalValueStore
import org.groundplatform.android.model.geometry.Coordinates
import org.groundplatform.android.model.geometry.LineString
import org.groundplatform.android.model.geometry.LinearRing
import org.groundplatform.android.model.geometry.Polygon
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.Style
import org.groundplatform.android.model.settings.MeasurementUnits
import org.groundplatform.android.model.submission.DrawAreaTaskData
import org.groundplatform.android.model.submission.DrawAreaTaskIncompleteData
import org.groundplatform.android.model.submission.TaskData
import org.groundplatform.android.model.task.Task
import org.groundplatform.android.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel.Companion.DISTANCE_THRESHOLD_DP
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.gms.GmsExt.getShellCoordinates
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DrawAreaTaskViewModelTest : BaseHiltTest() {

  @Inject lateinit var viewModel: DrawAreaTaskViewModel
  @Inject lateinit var localValueStore: LocalValueStore

  private lateinit var featureTestObserver: TestObserver<Feature>
  private lateinit var draftAreaObserver: TestObserver<Feature?>
  private lateinit var mergedFeatureFlow: Flow<Feature>
  private lateinit var mergedFeatureLiveData: LiveData<Feature>

  override fun setUp() {
    super.setUp()
    mergedFeatureFlow = merge(viewModel.draftArea.filterNotNull(), viewModel.draftUpdates)

    mergedFeatureLiveData = mergedFeatureFlow.asLiveData()
    featureTestObserver = TestObserver.test(mergedFeatureLiveData)
    draftAreaObserver = TestObserver.test(viewModel.draftArea.asLiveData())
  }

  @Test
  fun `Initializes with null task data`() {
    setupViewModel()

    assertGeometry(0)
    assertThat(viewModel.isMarkedComplete()).isFalse()
  }

  @Test
  fun `Initializes with complete polygon`() {
    val polygon =
      Polygon(LinearRing(listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3, COORDINATE_1)))
    setupViewModel(taskData = DrawAreaTaskData(area = polygon))

    assertGeometry(4, isLineString = true)
    assertThat(viewModel.isMarkedComplete()).isTrue()
  }

  @Test
  fun `Initializes with incomplete polygon`() {
    val lineString = LineString(coordinates = listOf(COORDINATE_1, COORDINATE_2, COORDINATE_3))
    setupViewModel(taskData = DrawAreaTaskIncompleteData(lineString))

    assertGeometry(3, isLineString = true)
    assertThat(viewModel.isMarkedComplete()).isFalse()
  }

  @Test
  fun `Can add vertex`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)

    // One vertex is selected and another is temporary vertex for rendering.
    assertGeometry(2, isLineString = true)
  }

  @Test
  fun `Can add multiple vertices`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    assertGeometry(4, isLineString = true)
  }

  @Test
  fun `Detects when last coordinate is close to starting one for three vertices`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    updateLastVertex(COORDINATE_3, true)

    assertGeometry(3, isLineString = true)
  }

  @Test
  fun `Detects when last coordinate is close to starting one for four vertices`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    updateLastVertex(COORDINATE_4, true)

    assertGeometry(4, isLineString = true)
  }

  @Test
  fun `Can remove last vertex on two vertices`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)

    viewModel.removeLastVertex()

    assertGeometry(1, isLineString = true)
  }

  @Test
  fun `Can remove last vertex on one vertex`() {
    setupViewModel()
    updateLastVertex(COORDINATE_1)

    viewModel.removeLastVertex()

    assertGeometry(0, isLineString = true)
  }

  @Test
  fun `Removing last vertex on no vertices does nothing`() {
    setupViewModel()
    updateLastVertex(COORDINATE_1)
    viewModel.removeLastVertex()

    viewModel.removeLastVertex()

    assertGeometry(0, isLineString = true)
  }

  @Test
  fun `Removing last vertex on complete polygon`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.removeLastVertex()

    assertGeometry(3, isLineString = true)
  }

  @Test
  fun `Cannot complete polygon when polygon is not complete`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertex(COORDINATE_3, false)

    assertThrows("Polygon is not complete", IllegalStateException::class.java) {
      viewModel.completePolygon()
    }
  }

  @Test
  fun `Check distance between Vertices`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    assertThat(featureTestObserver.value()?.tooltipText).isEqualTo("3,106,126 m")
  }

  @Test
  fun `Check distance between Vertices keeping last vertex far from starting point`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, false)

    assertThat(featureTestObserver.value()?.tooltipText).isEqualTo("1,497,148 m")
  }

  @Test
  fun `Tooltip is not rendered`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)

    viewModel.removeLastVertex()
    assertThat(featureTestObserver.value()?.tooltipText).isEqualTo(null)
  }

  @Test
  fun `Tooltip is rendered and removed`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    viewModel.removeLastVertex()

    assertThat(featureTestObserver.value()?.tooltipText).isEqualTo("1,565,109 m")

    viewModel.removeLastVertex()

    assertThat(featureTestObserver.value()?.tooltipText).isEqualTo(null)
  }

  @Test
  fun `Completes a polygon`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.completePolygon()
    assertGeometry(4, isLineString = true)
  }

  @Test
  fun `Completing a polygon populates polygonArea with the correct value in hectares`() {
    setupViewModel()
    localValueStore.selectedLengthUnit = MeasurementUnits.METRIC.name
    viewModel.initialize(JOB, TASK, taskData = null)

    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.completePolygon()

    with(viewModel.polygonArea.value) {
      assert((this?.split(" ")?.first()?.toDouble() ?: 0.0) > 0.0)
      assertEquals(this?.endsWith("ha"), true)
    }
  }

  @Test
  fun `Completing a polygon populates polygonArea with the correct value in acres`() {
    localValueStore.selectedLengthUnit = MeasurementUnits.IMPERIAL.name
    setupViewModel()

    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.completePolygon()

    with(viewModel.polygonArea.value) {
      assert((this?.split(" ")?.first()?.toDouble() ?: 0.0) > 0.0)
      assertEquals(this?.endsWith("ac"), true)
    }
  }

  @Test
  fun `redoLastVertex re-adds last vertex`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    viewModel.removeLastVertex()
    assertGeometry(2, isLineString = true)

    viewModel.redoLastVertex()
    assertGeometry(3, isLineString = true)
  }

  @Test
  fun `redoLastVertex when redo stack is empty`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)

    viewModel.redoLastVertex()
    assertThat(viewModel.redoVertexStack).isEqualTo(emptyList<Coordinates>())
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `First feature is emitted on draftArea and not on draftUpdates`() = runWithTestDispatcher {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    viewModel.draftArea.test {
      val first = awaitItem()
      assertThat(first).isNotNull()
      assertThat(first!!.geometry).isInstanceOf(LineString::class.java)
      cancelAndIgnoreRemainingEvents()
    }

    viewModel.draftUpdates.test {
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Subsequent vertex move emits in-place update on draftUpdates with stable tag`() =
    runWithTestDispatcher {
      setupViewModel()
      updateLastVertexAndAdd(COORDINATE_1)
      updateLastVertexAndAdd(COORDINATE_2)
      advanceUntilIdle()

      val initialTag = viewModel.draftArea.first()!!.tag

      viewModel.draftUpdates.test {
        updateLastVertex(Coordinates(15.0, 15.0), isNearFirstVertex = false)
        advanceUntilIdle()

        val upd = awaitItem()
        assertThat(upd.tag).isEqualTo(initialTag)
        val ls = upd.geometry as LineString
        assertThat(ls.coordinates.last()).isEqualTo(Coordinates(15.0, 15.0))
        assertThat(upd.tooltipText).isNotNull()
        cancelAndIgnoreRemainingEvents()
      }
    }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `Tooltip updates along with in-place geometry updates`() = runWithTestDispatcher {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    advanceUntilIdle()

    val firstFeature = viewModel.draftArea.first()
    val firstLine = firstFeature!!.geometry as LineString
    assertThat(firstLine.coordinates.size).isEqualTo(1)
    assertThat(firstFeature.tooltipText).isNull()

    updateLastVertexAndAdd(COORDINATE_2)
    advanceUntilIdle()
    val secondFeature = viewModel.draftArea.first()
    val secondLine = secondFeature!!.geometry as LineString
    assertThat(secondLine.coordinates.size).isEqualTo(1)
    assertThat(secondFeature.tooltipText).isNull()

    viewModel.removeLastVertex()
  }

  @Test
  fun `isTooClose is true when last vertex is close to previous vertex`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    // Distance between COORDINATE_2 (10, 10) and COORDINATE_3 (20, 20) is ~14.14
    // Threshold is 24. So this should be too close.
    updateLastVertex(COORDINATE_3, isNearFirstVertex = false)
    viewModel.updateLastVertexAndMaybeCompletePolygon(COORDINATE_3) { _, _ ->
      DISTANCE_THRESHOLD_DP.toDouble()
    }

    assertThat(viewModel.isTooClose.value).isTrue()
  }

  @Test
  fun `isTooClose is false when last vertex is far from previous vertex`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    viewModel.updateLastVertexAndMaybeCompletePolygon(COORDINATE_3) { _, _ ->
      DISTANCE_THRESHOLD_DP.toDouble() + 1
    }

    assertThat(viewModel.isTooClose.value).isFalse()
  }

  @Test
  fun `isTooClose is true after adding a vertex if size is greater than 1`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    // Add a 3rd vertex.
    // The logic `_isTooClose.value = vertices.size > 1` in `addLastVertex` should set it to true.
    viewModel.addLastVertex()

    assertThat(viewModel.isTooClose.value).isTrue()
  }

  @Test
  fun `isTooClose is false if only one vertex`() {
    setupViewModel()
    updateLastVertexAndAdd(COORDINATE_1)

    // Only 1 vertex.
    assertThat(viewModel.isTooClose.value).isFalse()
  }

  private fun assertGeometry(
    expectedVerticesCount: Int,
    isLineString: Boolean = false,
    isLinearRing: Boolean = false,
    isPolygon: Boolean = false,
  ) {
    if (expectedVerticesCount == 0) {
      assertThat(draftAreaObserver.value()).isNull()
      return
    }

    val geometry = featureTestObserver.value()?.geometry
    assertNotNull(geometry)
    assertWithMessage(geometry.getShellCoordinates().toString())
      .that(geometry.getShellCoordinates().size)
      .isEqualTo(expectedVerticesCount)
    assertThat(geometry)
      .isInstanceOf(
        when {
          isLineString -> LineString::class.java
          isLinearRing -> LinearRing::class.java
          isPolygon -> Polygon::class.java
          else -> error("Must be one of LineString, LinearRing, or Polygon")
        }
      )
  }

  /** Overwrites the last vertex and also adds a new one. */
  private fun updateLastVertexAndAdd(coordinate: Coordinates) {
    updateLastVertex(coordinate, false)
    viewModel.addLastVertex()
  }

  /** Updates the last vertex of the polygon with the given vertex. */
  private fun updateLastVertex(coordinate: Coordinates, isNearFirstVertex: Boolean = false) {
    val threshold = DISTANCE_THRESHOLD_DP.toDouble()
    val distanceInPixels = if (isNearFirstVertex) threshold else threshold + 1
    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> distanceInPixels }
  }

  private fun setupViewModel(job: Job = JOB, task: Task = TASK, taskData: TaskData? = null) {
    viewModel.initialize(job = job, task = task, taskData = taskData)
  }

  companion object {
    private val COORDINATE_1 = Coordinates(0.0, 0.0)
    private val COORDINATE_2 = Coordinates(10.0, 10.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)
    private val COORDINATE_4 = Coordinates(30.0, 30.0)

    private val TASK =
      Task(
        id = "task_1",
        index = 0,
        type = Task.Type.DRAW_AREA,
        label = "Task for drawing a polygon",
        isRequired = false,
      )
    private val JOB = Job("job", Style("#112233"))
  }
}
