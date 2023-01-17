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
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.geometry.Polygon
import com.google.android.ground.ui.datacollection.PolygonDrawingViewModel.PolygonDrawingState
import com.google.android.ground.ui.map.Feature
import com.google.common.collect.ImmutableSet
import com.google.common.truth.Truth
import com.jraska.livedata.TestObserver
import com.sharedtest.FakeData
import com.sharedtest.system.auth.FakeAuthenticationManager
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class PolygonDrawingTaskFragmentModelTest : BaseHiltTest() {
  @Inject lateinit var fakeAuthenticationManager: FakeAuthenticationManager
  @Inject lateinit var viewModel: PolygonDrawingViewModel

  private lateinit var polygonCompletedTestObserver: TestObserver<Boolean>
  private lateinit var drawnGeometryTestObserver: TestObserver<ImmutableSet<Feature>>

  override fun setUp() {
    super.setUp()
    fakeAuthenticationManager.setUser(FakeData.USER)
    polygonCompletedTestObserver = TestObserver.test(viewModel.isPolygonCompleted)
    drawnGeometryTestObserver = TestObserver.test(viewModel.features)

    // Initialize polygon drawing
    viewModel.startDrawingFlow()
  }

  @Test
  fun testStateOnBegin() {
    val stateTestObserver = viewModel.drawingState.test()
    viewModel.startDrawingFlow()
    stateTestObserver.assertValue(PolygonDrawingState::isInProgress)
  }

  @Test
  fun testSelectCurrentVertex() {
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    validateMapLoiDrawn(1)
  }

  @Test
  fun testSelectMultipleVertices() {
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    viewModel.selectCurrentVertex()
    validateMapLoiDrawn(1)
    validatePolygonCompleted(false)
  }

  @Test
  fun testUpdateLastVertex_whenVertexCountLessThan3() {
    viewModel.updateLastVertex(Point(Coordinate(0.0, 0.0)), 100.0)
    viewModel.updateLastVertex(Point(Coordinate(10.0, 10.0)), 100.0)
    viewModel.updateLastVertex(Point(Coordinate(20.0, 20.0)), 100.0)
    validateMapLoiDrawn(1)
    validatePolygonCompleted(false)
  }

  @Test
  fun testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNotNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    viewModel.selectCurrentVertex()

    // Move camera such that distance from last vertex is more than threshold
    viewModel.updateLastVertex(Point(Coordinate(30.0, 30.0)), 25.0)
    validateMapLoiDrawn(1)
    validatePolygonCompleted(false)
  }

  @Test
  fun testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    viewModel.selectCurrentVertex()

    // Move camera such that distance from last vertex is equal to threshold
    viewModel.updateLastVertex(Point(Coordinate(30.0, 30.0)), 24.0)

    validateMapLoiDrawn(1)
    validatePolygonCompleted(true)
  }

  @Test
  fun testRemoveLastVertex() {
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.removeLastVertex()
    validateMapLoiDrawn(0)
    validatePolygonCompleted(false)
  }

  @Test
  fun testRemoveLastVertex_whenNothingIsSelected() {
    val testObserver = viewModel.drawingState.test()
    viewModel.removeLastVertex()
    testObserver.assertValue(PolygonDrawingState::isCanceled)
  }

  @Test
  fun testRemoveLastVertex_whenPolygonIsComplete() {
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    viewModel.selectCurrentVertex()
    viewModel.updateLastVertex(Point(Coordinate(30.0, 30.0)), 24.0)
    viewModel.removeLastVertex()
    validateMapLoiDrawn(1)
    validatePolygonCompleted(false)
  }

  @Test
  fun testPolygonDrawingCompleted_whenPolygonIsIncomplete() {
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    Assert.assertThrows("Polygon is not complete", IllegalStateException::class.java) {
      viewModel.onCompletePolygonButtonClick()
    }
  }

  @Test
  fun testPolygonDrawingCompleted() {
    val stateTestObserver = viewModel.drawingState.test()
    viewModel.onCameraMoved(Point(Coordinate(0.0, 0.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(10.0, 10.0)))
    viewModel.selectCurrentVertex()
    viewModel.onCameraMoved(Point(Coordinate(20.0, 20.0)))
    viewModel.selectCurrentVertex()
    viewModel.updateLastVertex(Point(Coordinate(30.0, 30.0)), 24.0)
    viewModel.onCompletePolygonButtonClick()
    stateTestObserver.assertValue { polygonDrawingState: PolygonDrawingState ->
      (polygonDrawingState.isCompleted && polygonDrawingState.polygon?.vertices?.size == 4)
    }
  }

  private fun validatePolygonCompleted(isVisible: Boolean) {
    polygonCompletedTestObserver.assertValue(isVisible)
  }

  private fun validateMapLoiDrawn(expectedPolygonCount: Int) {
    drawnGeometryTestObserver.assertValue { features: ImmutableSet<Feature> ->
      var actualPolygonCount = 0
      for (feature in features) {
        when (feature.geometry) {
          is Polygon -> actualPolygonCount++
          else -> {}
        }
      }

      // Check whether drawn features contain expected number of polygons.
      Truth.assertThat(actualPolygonCount).isEqualTo(expectedPolygonCount)
      true
    }
  }
}
