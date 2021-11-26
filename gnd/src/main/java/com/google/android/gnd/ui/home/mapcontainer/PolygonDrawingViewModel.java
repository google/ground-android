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
import com.google.android.gnd.rx.BooleanOrError;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.auto.value.AutoValue;
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

  @Hot private final Subject<PolygonDrawingState> polygonDrawingState = PublishSubject.create();

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
  @Nullable private Point cameraTarget;

  /**
   * If true, then it means that the last vertex is added automatically and should be removed before
   * adding any permanent vertex. Used for rendering a line between last added point and current
   * camera target.
   */
  boolean isLastVertexNotSelectedByUser;

  /** Avoid creating a new uuid to prevent re-drawing everything on render. Reset to */
  @Nullable private String uuid;

  @Inject
  PolygonDrawingViewModel(
      LocationManager locationManager,
      AuthenticationManager authManager,
      OfflineUuidGenerator uuidGenerator) {
    this.locationManager = locationManager;
    this.authManager = authManager;
    this.uuidGenerator = uuidGenerator;
    // TODO: Create custom ui component for location lock button and share across app.
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
  public Observable<PolygonDrawingState> getDrawingState() {
    return polygonDrawingState;
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
  }

  public void removeLastVertex() {
    if (vertices.isEmpty()) {
      polygonDrawingState.onNext(createDrawingState(State.CANCELED));
      reset();
    } else {
      vertices.remove(vertices.size() - 1);
      updateUI();
    }
  }

  public void selectCurrentVertex() {
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
    if (isLastVertexNotSelectedByUser && !vertices.isEmpty()) {
      vertices.remove(vertices.size() - 1);
    }

    // Update selected state
    isLastVertexNotSelectedByUser = isNotSelectedByUser;

    // Add the new vertex
    vertices.add(vertex);

    // Render changes to UI
    updateUI();
  }

  private String getId() {
    if (uuid == null) {
      uuid = uuidGenerator.generateUuid();
    }
    return uuid;
  }

  private void updateUI() {
    if (selectedLayer.getValue() == null || selectedProject.getValue() == null) {
      Timber.e("Project or layer is null");
      return;
    }

    // Update complete button visibility
    completeButtonVisible.postValue(isPolygonComplete() ? VISIBLE : INVISIBLE);

    // Update drawn polygon
    AuditInfo auditInfo = AuditInfo.now(authManager.getCurrentUser());
    PolygonFeature polygonFeature =
        PolygonFeature.builder()
            .setVertices(ImmutableList.copyOf(this.vertices))
            .setId(getId())
            .setProject(selectedProject.getValue())
            .setLayer(selectedLayer.getValue())
            .setCreated(auditInfo)
            .setLastModified(auditInfo)
            .build();
    drawnPolylineVertices.setValue(polygonFeature);
  }

  public void onCompletePolygonButtonClick() {
    if (!isPolygonComplete()) {
      throw new IllegalStateException("Polygon is not complete");
    }
    polygonDrawingState.onNext(
        createDrawingState(State.COMPLETED, drawnPolylineVertices.getValue()));
    reset();
  }

  private void reset() {
    isLastVertexNotSelectedByUser = false;
    vertices.clear();
    uuid = null;
    completeButtonVisible.setValue(INVISIBLE);
  }

  private boolean isPolygonComplete() {
    return vertices.size() > 3 && getFirstVertex().equals(getLastVertex());
  }

  Optional<Point> getFirstVertex() {
    return vertices.isEmpty() ? Optional.empty() : Optional.of(vertices.get(0));
  }

  Optional<Point> getLastVertex() {
    return vertices.isEmpty() ? Optional.empty() : Optional.of(vertices.get(vertices.size() - 1));
  }

  public void onLocationLockClick() {
    locationLockChangeRequests.onNext(!isLocationLockEnabled());
  }

  public LiveData<Integer> getPolygonDrawingCompletedVisibility() {
    return completeButtonVisible;
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
    polygonDrawingState.onNext(createDrawingState(State.IN_PROGRESS));
  }

  public LiveData<Integer> getIconTint() {
    return iconTint;
  }

  public LiveData<PolygonFeature> getPolygonFeature() {
    return drawnPolylineVertices;
  }

  private PolygonDrawingState createDrawingState(State state) {
    return createDrawingState(state, null);
  }

  private PolygonDrawingState createDrawingState(State state, @Nullable PolygonFeature feature) {
    return new AutoValue_PolygonDrawingViewModel_PolygonDrawingState(state, feature);
  }

  @AutoValue
  public abstract static class PolygonDrawingState {

    public abstract State getState();

    @Nullable
    public abstract PolygonFeature getPolygonFeature();
  }

  /** Represents state of PolygonDrawing action. */
  public enum State {
    IN_PROGRESS,
    COMPLETED,
    CANCELED
  }
}
