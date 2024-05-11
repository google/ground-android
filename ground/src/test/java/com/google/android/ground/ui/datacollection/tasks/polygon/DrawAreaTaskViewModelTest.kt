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
package com.google.android.ground.ui.datacollection.tasks.polygon

import androidx.lifecycle.asLiveData
import com.google.android.ground.BaseHiltTest
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.geometry.LineString
import com.google.android.ground.model.geometry.LinearRing
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.datacollection.tasks.polygon.DrawAreaTaskViewModel.Companion.DISTANCE_THRESHOLD_DP
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.gms.GmsExt.getShellCoordinates
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.jraska.livedata.TestObserver
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// TODO: Convert to fragment test for better coverage

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class DrawAreaTaskViewModelTest : BaseHiltTest() {
  @Inject lateinit var viewModel: DrawAreaTaskViewModel

  private lateinit var featureTestObserver: TestObserver<Feature>

  override fun setUp() {
    super.setUp()
    featureTestObserver = TestObserver.test(viewModel.draftArea.asLiveData())
  }

  @Test
  fun testAddVertex() {
    updateLastVertexAndAdd(COORDINATE_1)

    // One vertex is selected and another is temporary vertex for rendering.
    assertGeometry(2, isLineString = true)
  }

  @Test
  fun testAddVertex_multiplePoints() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    assertGeometry(4, isLineString = true)
  }

  @Test
  fun testUpdateLastVertex_closeToFirstVertex_vertexCountIs2() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)

    updateLastVertex(COORDINATE_3, true)

    assertGeometry(3, isLineString = true)
  }

  @Test
  fun testUpdateLastVertex_closeToFirstVertex_vertexCountIs3() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)

    updateLastVertex(COORDINATE_4, true)

    assertGeometry(4, isLineString = true)
  }

  @Test
  fun testRemoveLastVertex_twoVertices() {
    updateLastVertexAndAdd(COORDINATE_1)

    viewModel.removeLastVertex()

    assertGeometry(1, isLineString = true)
  }

  @Test
  fun testRemoveLastVertex_oneVertex() {
    updateLastVertex(COORDINATE_1)

    viewModel.removeLastVertex()

    assertGeometry(0, isLineString = true)
  }

  @Test
  fun testRemoveLastVertex_whenEmpty_doesNothing() {
    updateLastVertex(COORDINATE_1)
    viewModel.removeLastVertex()

    viewModel.removeLastVertex()

    assertGeometry(0, isLineString = true)
  }

  @Test
  fun testRemoveLastVertex_whenPolygonIsComplete() {
    updateLastVertexAndAdd(COORDINATE_1)
    updateLastVertexAndAdd(COORDINATE_2)
    updateLastVertexAndAdd(COORDINATE_3)
    updateLastVertex(COORDINATE_4, true)

    viewModel.removeLastVertex()

    assertGeometry(3, isLineString = true)
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

    assertGeometry(4, isLineString = true)
  }

  private fun assertGeometry(
    expectedVerticesCount: Int,
    isLineString: Boolean = false,
    isLinearRing: Boolean = false,
    isPolygon: Boolean = false,
  ) {
    val geometry = featureTestObserver.value()?.geometry
    if (expectedVerticesCount == 0) {
      assertThat(geometry).isNull()
    } else {
      assertNotNull(geometry)
      assertWithMessage(geometry.getShellCoordinates().toString())
        .that(geometry.getShellCoordinates().size)
        .isEqualTo(expectedVerticesCount)
      assertThat(geometry)
        .isInstanceOf(
          if (isLineString) LineString::class.java
          else if (isLinearRing) LinearRing::class.java
          else if (isPolygon) Polygon::class.java
          else error("Must be one of LineString, LinearRing, or Polygon")
        )
    }
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

  companion object {
    private val COORDINATE_1 = Coordinates(0.0, 0.0)
    private val COORDINATE_2 = Coordinates(10.0, 10.0)
    private val COORDINATE_3 = Coordinates(20.0, 20.0)
    private val COORDINATE_4 = Coordinates(30.0, 30.0)
  }
}
