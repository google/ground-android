/*
 * Copyright 2018 Google LLC
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

package com.google.android.gnd.ui.home;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.gnd.rx.RxCompletable.toBooleanSingle;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.Action;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapPin;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  @Hot(replays = true)
  public final MutableLiveData<Boolean> isObservationButtonVisible = new MutableLiveData<>(false);

  private final ProjectRepository projectRepository;
  private final Navigator navigator;
  private final FeatureRepository featureRepository;

  /** The state and value of the currently active project (loading, loaded, etc.). */
  private final LiveData<Loadable<Project>> projectLoadingState;

  // TODO(#719): Move into MapContainersViewModel
  @Hot(replays = true)
  private final MutableLiveData<Event<Point>> addFeatureDialogRequests = new MutableLiveData<>();
  // TODO(#719): Move into FeatureDetailsViewModel.
  @Hot(replays = true)
  private final MutableLiveData<Action> openDrawerRequests = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<BottomSheetState> bottomSheetState = new MutableLiveData<>();

  @Hot private final FlowableProcessor<Feature> addFeatureClicks = PublishProcessor.create();
  @Hot private final FlowableProcessor<Feature> updateFeatureRequests = PublishProcessor.create();
  @Hot private final FlowableProcessor<Feature> deleteFeatureRequests = PublishProcessor.create();

  private final LiveData<Feature> addFeatureResults;
  private final LiveData<Boolean> updateFeatureResults;
  private final LiveData<Boolean> deleteFeatureResults;

  @Hot(replays = true)
  private final MutableLiveData<Throwable> errors = new MutableLiveData<>();

  @Hot(replays = true)
  private final MutableLiveData<Integer> addFeatureButtonVisibility = new MutableLiveData<>(GONE);

  @Inject
  HomeScreenViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      Navigator navigator) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.navigator = navigator;

    projectLoadingState =
        LiveDataReactiveStreams.fromPublisher(
            projectRepository
                .getProjectLoadingState()
                .doAfterNext(this::onProjectLoadingStateChange));
    addFeatureResults =
        LiveDataReactiveStreams.fromPublisher(
            addFeatureClicks.switchMapSingle(
                feature ->
                    featureRepository
                        .createFeature(feature)
                        .toSingleDefault(feature)
                        .doOnError(this::handleError)
                        .onErrorResumeNext(Single.never()))); // Prevent from breaking upstream.
    deleteFeatureResults =
        LiveDataReactiveStreams.fromPublisher(
            deleteFeatureRequests.switchMapSingle(
                feature ->
                    toBooleanSingle(featureRepository.deleteFeature(feature), this::handleError)));
    updateFeatureResults =
        LiveDataReactiveStreams.fromPublisher(
            updateFeatureRequests.switchMapSingle(
                feature ->
                    toBooleanSingle(featureRepository.updateFeature(feature), this::handleError)));
  }

  private void handleError(Throwable throwable) {
    errors.postValue(throwable);
  }

  /** Handle state of the UI elements depending upon the active project. */
  private void onProjectLoadingStateChange(Loadable<Project> project) {
    addFeatureButtonVisibility.postValue(shouldShowAddFeatureButton(project) ? VISIBLE : GONE);
  }

  private boolean shouldShowAddFeatureButton(Loadable<Project> project) {
    if (!project.isLoaded()) {
      return false;
    }

    // TODO: Also check if the project has user-editable layers.
    //  Pending feature, https://github.com/google/ground-platform/issues/228

    // Project must contain at least 1 layer.
    return project.value().map(p -> !p.getLayers().isEmpty()).orElse(false);
  }

  public LiveData<Integer> getAddFeatureButtonVisibility() {
    return addFeatureButtonVisibility;
  }

  public LiveData<Feature> getAddFeatureResults() {
    return addFeatureResults;
  }

  public LiveData<Boolean> getUpdateFeatureResults() {
    return updateFeatureResults;
  }

  public LiveData<Boolean> getDeleteFeatureResults() {
    return deleteFeatureResults;
  }

  public LiveData<Throwable> getErrors() {
    return errors;
  }

  public void addFeature(Project project, Layer layer, Point point) {
    addFeatureClicks.onNext(featureRepository.newFeature(project, layer, point));
  }

  public void updateFeature(Feature feature) {
    updateFeatureRequests.onNext(feature);
  }

  public void deleteFeature(Feature feature) {
    deleteFeatureRequests.onNext(feature);
  }

  public boolean shouldShowProjectSelectorOnStart() {
    return projectRepository.getLastActiveProjectId().isEmpty();
  }

  public LiveData<Action> getOpenDrawerRequests() {
    return openDrawerRequests;
  }

  public void openNavDrawer() {
    openDrawerRequests.setValue(Action.create());
  }

  public LiveData<Loadable<Project>> getProjectLoadingState() {
    return projectLoadingState;
  }

  public LiveData<Event<Point>> getShowAddFeatureDialogRequests() {
    return addFeatureDialogRequests;
  }

  public LiveData<BottomSheetState> getBottomSheetState() {
    return bottomSheetState;
  }

  // TODO: Remove extra indirection here?
  public void onMarkerClick(MapPin marker) {
    showBottomSheet(marker.getFeature());
  }

  private void showBottomSheet(Feature feature) {
    Timber.d("showing bottom sheet");
    isObservationButtonVisible.setValue(true);
    bottomSheetState.setValue(BottomSheetState.visible(feature));
  }

  public void onAddFeatureBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addFeatureDialogRequests.setValue(Event.create(location));
  }

  public void onBottomSheetHidden() {
    bottomSheetState.setValue(BottomSheetState.hidden());
    isObservationButtonVisible.setValue(false);
  }

  public void addObservation() {
    BottomSheetState state = bottomSheetState.getValue();
    if (state == null) {
      Timber.e("Missing bottomSheetState");
      return;
    }

    Optional<Feature> optionalFeature = state.getFeature();
    if (optionalFeature.isEmpty()) {
      Timber.e("Missing feature");
      return;
    }
    Feature feature = optionalFeature.get();
    Optional<Form> form = feature.getLayer().getForm();
    if (form.isEmpty()) {
      // .TODO: Hide Add Observation button if no forms defined.
      Timber.e("No forms in layer");
      return;
    }
    Project project = feature.getProject();
    if (project == null) {
      Timber.e("Missing project");
      return;
    }
    navigator.navigate(
        HomeScreenFragmentDirections.addObservation(
            project.getId(), feature.getId(), form.get().getId()));
  }

  public void init() {
    // Last active project will be loaded once view subscribes to activeProject.
    projectRepository.getLastActiveProjectId().ifPresent(projectRepository::activateProject);
  }

  public void showOfflineAreas() {
    navigator.navigate(HomeScreenFragmentDirections.showOfflineAreas());
  }

  public void showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity());
  }
}
