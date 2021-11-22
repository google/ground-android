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
import java8.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class PolygonDrawingViewModel extends AbstractViewModel {

  /** Min. distance in dp between two points for them be considered as overlapping. */
  public static final int DISTANCE_THRESHOLD_DP = 24;

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

  /**
   * If true, then it means that the last vertex is added automatically and should be removed before
   * adding any permanent vertex. Used for rendering a line between last added point and current
   * camera target.
   */
  boolean isLastVertexNotSelectedByUser;

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
    if (locationLockState.getValue() != null && isLocationLockEnabled()) {
      Timber.d("User dragged map. Disabling location lock");
      locationLockChangeRequests.onNext(false);
    }
  }

  public void updateLastVertex(Point newTarget, double distanceInPixels) {
    boolean isPolygonComplete = vertices.size() > 2 && distanceInPixels <= DISTANCE_THRESHOLD_DP;
    addVertex(isPolygonComplete ? vertices.get(0) : newTarget, true);
    updateDrawingState(isPolygonComplete ? PolygonDrawing.COMPLETED : PolygonDrawing.STARTED);
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
      addVertex(cameraTarget, false);
    }
  }

  public void setLocationLockEnabled(boolean enabled) {
    locationLockEnabled.postValue(enabled);
  }

  /**
   * Adds a new vertex.
   *
   * @param vertex new position
   * @param isNotSelectedByUser whether the vertex is not selected by the user
   */
  private void addVertex(Point vertex, boolean isNotSelectedByUser) {
    // Clear last vertex if it is unselected
    if (isLastVertexNotSelectedByUser && vertices.size() > 1) {
      vertices.remove(vertices.size() - 1);
    }

    // Update selected state
    isLastVertexNotSelectedByUser = isNotSelectedByUser;

    // Add the new vertex
    vertices.add(vertex);

    // Render changes to UI
    updateDrawnPolygonFeature(ImmutableList.copyOf(vertices));
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

  public void onCompletePolygonButtonClick() {
    defaultMapMode.onNext(Nil.NIL);
    drawingCompleted.onNext(Nil.NIL);
    isLastVertexNotSelectedByUser = false;
    vertices.clear();
  }

  public Optional<Point> getFirstVertex() {
    return vertices.isEmpty() ? Optional.empty() : Optional.of(vertices.get(0));
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
