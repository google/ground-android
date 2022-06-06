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

package com.google.android.gnd.ui.home.mapcontainer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel.PolygonDrawingState;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapPin;
import com.google.android.gnd.ui.map.MapPolygon;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
public class PolygonDrawingViewModelTest extends BaseHiltTest {

  @Inject PolygonDrawingViewModel viewModel;

  private com.jraska.livedata.TestObserver<Boolean> polygonCompletedTestObserver;
  private com.jraska.livedata.TestObserver<ImmutableSet<MapFeature>> drawnMapFeaturesTestObserver;

  @Override
  public void setUp() {
    super.setUp();

    polygonCompletedTestObserver =
        com.jraska.livedata.TestObserver.test(viewModel.isPolygonCompleted());
    drawnMapFeaturesTestObserver =
        com.jraska.livedata.TestObserver.test(viewModel.getUnsavedMapFeatures());

    // Initialize polygon drawing
    viewModel.startDrawingFlow(FakeData.SURVEY, FakeData.JOB);
  }

  @Test
  public void testStateOnBegin() {
    TestObserver<PolygonDrawingState> stateTestObserver = viewModel.getDrawingState().test();

    viewModel.startDrawingFlow(FakeData.SURVEY, FakeData.JOB);

    stateTestObserver.assertValue(PolygonDrawingState::isInProgress);
  }

  @Test
  public void testSelectCurrentVertex() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    validateMapFeaturesDrawn(1, 1);
  }

  @Test
  public void testSelectMultipleVertices() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    validateMapFeaturesDrawn(1, 3);
    validatePolygonCompleted(false);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountLessThan3() {
    viewModel.updateLastVertex(newPoint(0.0, 0.0), 100);
    viewModel.updateLastVertex(newPoint(10.0, 10.0), 100);
    viewModel.updateLastVertex(newPoint(20.0, 20.0), 100);

    validateMapFeaturesDrawn(1, 1);
    validatePolygonCompleted(false);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNotNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    // Move camera such that distance from last vertex is more than threshold
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 25);

    validateMapFeaturesDrawn(1, 4);
    validatePolygonCompleted(false);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountEqualTo3AndLastVertexIsNearFirstPoint() {
    // Select 3 vertices
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    // Move camera such that distance from last vertex is equal to threshold
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 24);

    // Only 3 pins should be drawn. First and last points are exactly same.
    validateMapFeaturesDrawn(1, 3);
    validatePolygonCompleted(true);
  }

  @Test
  public void testRemoveLastVertex() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    viewModel.removeLastVertex();

    validateMapFeaturesDrawn(0, 0);
    validatePolygonCompleted(false);
  }

  @Test
  public void testRemoveLastVertex_whenNothingIsSelected() {
    TestObserver<PolygonDrawingState> testObserver = viewModel.getDrawingState().test();

    viewModel.removeLastVertex();

    testObserver.assertValue(PolygonDrawingState::isCanceled);
  }

  @Test
  public void testRemoveLastVertex_whenPolygonIsComplete() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 24);

    viewModel.removeLastVertex();

    validateMapFeaturesDrawn(1, 3);
    validatePolygonCompleted(false);
  }

  @Test
  public void testPolygonDrawingCompleted_whenPolygonIsIncomplete() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));

    assertThrows(
        "Polygon is not complete",
        IllegalStateException.class,
        () -> viewModel.onCompletePolygonButtonClick());
  }

  @Test
  public void testPolygonDrawingCompleted() {
    TestObserver<PolygonDrawingState> stateTestObserver = viewModel.getDrawingState().test();

    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();
    viewModel.updateLastVertex(newPoint(30.0, 30.0), 24);

    viewModel.onCompletePolygonButtonClick();

    stateTestObserver.assertValue(
        polygonDrawingState ->
            polygonDrawingState.isCompleted()
                && polygonDrawingState.getUnsavedPolygonFeature() != null
                && polygonDrawingState.getUnsavedPolygonFeature().getVertices().size() == 4);
  }

  private void validatePolygonCompleted(boolean isVisible) {
    polygonCompletedTestObserver.assertValue(isVisible);
  }

  private void validateMapFeaturesDrawn(int expectedMapPolygonCount, int expectedMapPinCount) {
    drawnMapFeaturesTestObserver.assertValue(
        mapFeatures -> {
          int actualMapPolygonCount = 0;
          int actualMapPinCount = 0;

          for (MapFeature mapFeature : mapFeatures) {
            if (mapFeature instanceof MapPolygon) {
              actualMapPolygonCount++;
            } else if (mapFeature instanceof MapPin) {
              actualMapPinCount++;
            }
          }

          // Check whether drawn features contain expected number of polygons and pins.
          assertThat(actualMapPinCount).isEqualTo(expectedMapPinCount);
          assertThat(actualMapPolygonCount).isEqualTo(expectedMapPolygonCount);

          return true;
        });
  }

  private Point newPoint(double latitude, double longitude) {
    return Point.newBuilder().setLatitude(latitude).setLongitude(longitude).build();
  }
}
