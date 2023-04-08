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
package com.google.android.ground.ui.datacollection

import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.geometry.Coordinate
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.datacollection.tasks.polygon.PolygonDrawingViewModel
import com.google.android.ground.ui.datacollection.tasks.polygon.PolygonDrawingViewModel.Companion.DISTANCE_THRESHOLD_DP
import com.google.android.ground.ui.map.Feature
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jraska.livedata.TestObserver
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PolygonDrawingTaskFragmentModelTest : BaseHiltTest() {
  @Inject lateinit var viewModel: PolygonDrawingViewModel

  private lateinit var polygonTestObserver: TestObserver<Polygon>
  private lateinit var drawnGeometryTestObserver: TestObserver<Set<Feature>>

  override fun setUp() {
    super.setUp()
    polygonTestObserver = TestObserver.test(viewModel.polygonLiveData)
    drawnGeometryTestObserver = TestObserver.test(viewModel.features)
  }

  @Test
  fun testAddVertex() {
    updateLastVertexAndAdd(COORDINATE_1)

    // One vertex is selected and another is temporary vertex for rendering.
    assertPolygon(2, false)
  }

  @Test
  fun testAddVertex_multiplePoints() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    assertPolygon(4, false)
  }

  @Test
  fun testUpdateLastVertex_closeToFirstVertex_vertexCountIs2() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    updateLastVertex(COORDINATE_3, true)

    assertPolygon(3, false)
  }

  @Test
  fun testUpdateLastVertex_closeToFirstVertex_vertexCountIs3() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    updateLastVertex(COORDINATE_4, true)

    assertPolygon(4, true)
  }

  @Test
  fun testRemoveLastVertex_twoVertices() {
    updateLastVertexAndAdd(COORDINATE_1)

    viewModel.removeLastVertex()

    assertPolygon(1, false)
  }

  @Test
  fun testRemoveLastVertex_oneVertex() {
    updateLastVertex(COORDINATE_1)

    viewModel.removeLastVertex()

    assertPolygon(0, false)
  }

  @Test
  fun testRemoveLastVertex_whenEmpty_doesNothing() {
    updateLastVertex(COORDINATE_1)
    viewModel.removeLastVertex()

    viewModel.removeLastVertex()

    assertPolygon(0, false)
  }

  @Test
  fun testRemoveLastVertex_whenPolygonIsComplete() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.removeLastVertex()

    assertPolygon(3, false)
  }

  @Test
  fun testOnCompletePolygonButtonClick_whenPolygonIsIncomplete() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertex(COORDINATE_3, false)

    assertThrows("Polygon is not complete", IllegalStateException::class.java) {
      viewModel.onCompletePolygonButtonClick()
    }
  }

  @Test
  fun testOnCompletePolygonButtonClick_whenPolygonIsComplete() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.onCompletePolygonButtonClick()

    assertPolygon(4, true)
  }

  private fun assertPolygonDrawn(result: Boolean) {
    drawnGeometryTestObserver.assertValue { features: Set<Feature> ->
      var actualPolygonCount = 0
      for (feature in features) {
        if (feature.geometry is Polygon) {
          actualPolygonCount++
        }
      }

      assertThat(actualPolygonCount).isEqualTo(if (result) 1 else 0)
      true
    }
  }

  private fun assertPolygon(expectedVerticesCount: Int, isPolygonComplete: Boolean) {
    val polygon = polygonTestObserver.value()
    assertWithMessage(polygon.vertices.toString())
      .that(polygon.size)
      .isEqualTo(expectedVerticesCount)
    assertWithMessage("isPolygonComplete doesn't match")
      .that(polygon.isComplete)
      .isEqualTo(isPolygonComplete)

    assertPolygonDrawn(expectedVerticesCount > 0)
  }

  /** Overwrites the last vertex and also adds a new one. */
  private fun updateLastVertexAndAdd(coordinate: Coordinate) {
    updateLastVertex(coordinate, false)
    viewModel.addLastVertex()
  }

  /** Updates the last vertex of the polygon with the given vertex. */
  private fun updateLastVertex(coordinate: Coordinate, isNearFirstVertex: Boolean = false) {
    val threshold = DISTANCE_THRESHOLD_DP.toDouble()
    val distanceInPixels = if (isNearFirstVertex) threshold else threshold + 1
    viewModel.updateLastVertexAndMaybeCompletePolygon(coordinate) { _, _ -> distanceInPixels }
  }

  companion object {
    private val COORDINATE_1 = Coordinate(0.0, 0.0)
    private val COORDINATE_2 = Coordinate(10.0, 10.0)
    private val COORDINATE_3 = Coordinate(20.0, 20.0)
    private val COORDINATE_4 = Coordinate(30.0, 30.0)
  }
}
