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

import static com.google.android.gnd.TestObservers.observeUntilFirstChange;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.view.View;
import com.google.android.gnd.BaseHiltTest;
import com.google.android.gnd.FakeData;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel.PolygonDrawingState;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel.State;
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

  @Override
  public void setUp() {
    super.setUp();

    // Initialize polygon drawing
    viewModel.startDrawingFlow(FakeData.PROJECT, FakeData.LAYER);
  }

  @Test
  public void testStateOnBegin() {
    TestObserver<PolygonDrawingState> stateTestObserver = viewModel.getDrawingState().test();

    viewModel.startDrawingFlow(FakeData.PROJECT, FakeData.LAYER);

    stateTestObserver.assertValue(
        state -> state.getState() == State.IN_PROGRESS && state.getPolygonFeature() == null);
  }

  @Test
  public void testSelectCurrentVertex() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    assertPolygonFeatureMutated(1);
  }

  @Test
  public void testSelectMultipleVertices() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(10.0, 10.0));
    viewModel.selectCurrentVertex();
    viewModel.onCameraMoved(newPoint(20.0, 20.0));
    viewModel.selectCurrentVertex();

    assertPolygonFeatureMutated(3);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  @Test
  public void testUpdateLastVertex_whenVertexCountLessThan3() {
    viewModel.updateLastVertex(newPoint(0.0, 0.0), 100);
    viewModel.updateLastVertex(newPoint(10.0, 10.0), 100);
    viewModel.updateLastVertex(newPoint(20.0, 20.0), 100);

    assertPolygonFeatureMutated(1);
    assertCompleteButtonVisible(View.INVISIBLE);
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

    assertPolygonFeatureMutated(4);
    assertCompleteButtonVisible(View.INVISIBLE);
    assertThat(viewModel.getFirstVertex()).isNotEqualTo(viewModel.getLastVertex());
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

    assertPolygonFeatureMutated(4);
    assertCompleteButtonVisible(View.VISIBLE);
    assertThat(viewModel.getFirstVertex()).isEqualTo(viewModel.getLastVertex());
  }

  @Test
  public void testRemoveLastVertex() {
    viewModel.onCameraMoved(newPoint(0.0, 0.0));
    viewModel.selectCurrentVertex();

    viewModel.removeLastVertex();

    assertPolygonFeatureMutated(0);
    assertCompleteButtonVisible(View.INVISIBLE);
  }

  @Test
  public void testRemoveLastVertex_whenNothingIsSelected() {
    TestObserver<PolygonDrawingState> testObserver = viewModel.getDrawingState().test();

    viewModel.removeLastVertex();

    testObserver.assertValue(state -> state.getState() == State.CANCELED);
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

    assertPolygonFeatureMutated(3);
    assertCompleteButtonVisible(View.INVISIBLE);
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
            polygonDrawingState.getState() == State.COMPLETED
                && polygonDrawingState.getPolygonFeature() != null
                && polygonDrawingState.getPolygonFeature().getVertices().size() == 4);
  }

  private void assertCompleteButtonVisible(int visibility) {
    observeUntilFirstChange(viewModel.getPolygonDrawingCompletedVisibility());
    assertThat(viewModel.getPolygonDrawingCompletedVisibility().getValue()).isEqualTo(visibility);
  }

  private void assertPolygonFeatureMutated(int vertexCount) {
    observeUntilFirstChange(viewModel.getMapFeatures());
    // TODO: Update test
    // assertThat(viewModel.getMapFeatures().getValue().getVertices()).hasSize(vertexCount);
  }

  private Point newPoint(double latitude, double longitude) {
    return Point.newBuilder().setLatitude(latitude).setLongitude(longitude).build();
  }
}
