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

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.location.Location;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.R;
import com.google.android.gnd.model.AuditInfo;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.feature.PolygonFeature;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.persistence.uuid.OfflineUuidGenerator;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.common.collect.ImmutableList;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class PolygonDrawingViewModel extends AbstractViewModel {

  /** Minimum distance (in metres) between points to be considered as non-overlapping. */
  public static final int DISTANCE_THRESHOLD = 10;

  @Hot private final Subject<Nil> defaultMapMode = PublishSubject.create();

  @Hot private final Subject<Nil> drawingCompleted = PublishSubject.create();

  private final MutableLiveData<Integer> completeButtonVisible = new MutableLiveData<>(INVISIBLE);
  /** Polyline drawn by the user but not yet saved as polygon. */
  @Hot
  private final MutableLiveData<PolygonFeature> drawnPolylineVertices = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Boolean> locationLockEnabled = new MutableLiveData<>();

  private final LiveData<Integer> iconTint;
  @Hot private final Subject<Boolean> locationLockChangeRequests = PublishSubject.create();
  private final LocationManager locationManager;
  private final LiveData<BooleanOrError> locationLockState;
  private final List<Point> vertices = new ArrayList<>();
  /** The currently selected layer and project for the polygon drawing. */
  private final BehaviorProcessor<Layer> selectedLayer = BehaviorProcessor.create();

  private final BehaviorProcessor<Project> selectedProject = BehaviorProcessor.create();

  private final OfflineUuidGenerator uuidGenerator;
  private final AuthenticationManager authManager;
  private final FeatureRepository featureRepository;
  @Nullable private Point cameraTarget;

  @Inject
  PolygonDrawingViewModel(
      LocationManager locationManager,
      FeatureRepository featureRepository,
      AuthenticationManager authManager,
      OfflineUuidGenerator uuidGenerator) {
    this.locationManager = locationManager;
    this.authManager = authManager;
    this.featureRepository = featureRepository;
    this.uuidGenerator = uuidGenerator;
    Flowable<BooleanOrError> locationLockStateFlowable = createLocationLockStateFlowable().share();
    this.locationLockState =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable.startWith(BooleanOrError.falseValue()));
    this.iconTint =
        LiveDataReactiveStreams.fromPublisher(
            locationLockStateFlowable
                .map(locked -> locked.isTrue() ? R.color.colorMapBlue : R.color.colorGrey800)
                .startWith(R.color.colorGrey800));
  }

  private Flowable<BooleanOrError> createLocationLockStateFlowable() {
    return locationLockChangeRequests
        .switchMapSingle(
            enabled ->
                enabled
                    ? this.locationManager.enableLocationUpdates()
                    : this.locationManager.disableLocationUpdates())
        .toFlowable(BackpressureStrategy.LATEST);
  }

  @Hot
  public Observable<Nil> getDefaultMapMode() {
    return defaultMapMode;
  }

  @Hot
  public Observable<Nil> getDrawingCompleted() {
    return drawingCompleted;
  }

  public void onCameraMoved(Point newTarget) {
    cameraTarget = newTarget;
    if (vertices.size() >= 3) {
      checkPointNearVertex(cameraTarget, false);
    }
    if (locationLockState.getValue() != null && isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock");
      locationLockChangeRequests.onNext(false);
    }
  }

  public void removeLastVertex() {
    if (vertices.isEmpty()) {
      defaultMapMode.onNext(Nil.NIL);
      return;
    }
    vertices.remove(vertices.size() - 1);
    updateDrawnPolygonFeature(ImmutableList.copyOf(vertices));
    updateDrawingState(PolygonDrawing.STARTED);
  }

  public void onAddPolygonBtnClick() {
    if (cameraTarget != null) {
      if (vertices.size() >= 3) {
        checkPointNearVertex(cameraTarget, true);
      } else {
        vertices.add(cameraTarget);
      }
      updateDrawnPolygonFeature(ImmutableList.copyOf(vertices));
    }
  }

  public void setLocationLockEnabled(boolean enabled) {
    locationLockEnabled.postValue(enabled);
  }

  private void checkPointNearVertex(Point position, Boolean addVertex) {
    if (isPointNearFirstVertex(position)) {
      updateDrawingState(PolygonDrawing.COMPLETED);
      vertices.add(vertices.get(0));
      updateDrawnPolygonFeature(ImmutableList.copyOf(vertices));
    } else {
      if (vertices.get(0) != vertices.get(vertices.size() - 1)) {
        if (addVertex) {
          vertices.add(position);
        }
        updateDrawingState(PolygonDrawing.STARTED);
      }
    }
  }

  private void updateDrawnPolygonFeature(ImmutableList<Point> vertices) {
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    PolygonFeature polygonFeature =
        PolygonFeature.builder()
            .setVertices(vertices)
            .setId(uuidGenerator.generateUuid())
            .setProject(selectedProject.getValue())
            .setLayer(selectedLayer.getValue())
            .setCreated(auditInfo)
            .setLastModified(auditInfo)
            .build();
    drawnPolylineVertices.setValue(polygonFeature);
  }

  private boolean isPointNearFirstVertex(Point point) {
    if (vertices.get(0) == vertices.get(vertices.size() - 1)) {
      return false;
    }
    float[] distance = new float[1];
    Location.distanceBetween(
        point.getLatitude(),
        point.getLongitude(),
        vertices.get(0).getLatitude(),
        vertices.get(0).getLongitude(),
        distance);
    return distance[0] < DISTANCE_THRESHOLD;
  }

  public void onCompletePolygonButtonClick() {
    defaultMapMode.onNext(Nil.NIL);
    updateDrawnPolygonFeature(ImmutableList.copyOf(vertices));
    drawingCompleted.onNext(Nil.NIL);
    vertices.clear();
  }

  public void onLocationLockClick() {
    locationLockChangeRequests.onNext(!isLocationLockEnabled());
  }

  public LiveData<Integer> getPolygonDrawingCompletedVisibility() {
    return completeButtonVisible;
  }

  private void updateDrawingState(PolygonDrawing polygonDrawing) {
    completeButtonVisible.postValue(
        polygonDrawing == PolygonDrawing.COMPLETED ? VISIBLE : INVISIBLE);
  }

  private boolean isLocationLockEnabled() {
    return locationLockState.getValue().isTrue();
  }

  public LiveData<Boolean> getLocationLockEnabled() {
    // TODO : current location is not working value is always false.
    return locationLockEnabled;
  }

  public void startDrawingFlow(Project selectedProject, Layer selectedLayer) {
    this.selectedLayer.onNext(selectedLayer);
    this.selectedProject.onNext(selectedProject);
    updateDrawingState(PolygonDrawing.STARTED);
  }

  public LiveData<Integer> getIconTint() {
    return iconTint;
  }

  public LiveData<PolygonFeature> getPolygonFeature() {
    return drawnPolylineVertices;
  }

  public enum PolygonDrawing {
    STARTED,
    COMPLETED
  }

  public boolean isPolygonInfoDialogShown() {
    return featureRepository.isPolygonDialogInfoShown();
  }

  public void updatePolygonInfoDialogShown() {
    featureRepository.setPolygonDialogInfoShown(true);
  }
}
