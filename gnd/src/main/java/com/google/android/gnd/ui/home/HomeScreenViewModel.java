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

import static com.google.android.gnd.rx.Nil.NIL;
import static com.google.android.gnd.rx.RxCompletable.toBooleanSingle;
import static com.google.android.gnd.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Mutation.Type;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureMutation;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.repository.UserRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.annotations.Hot;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapFeature;
import com.google.android.gnd.ui.map.MapPin;
import com.google.common.collect.ImmutableList;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import java.util.List;
import java8.util.Objects;
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
  private final UserRepository userRepository;

  /** The state and value of the currently active project (loading, loaded, etc.). */
  private final LiveData<Loadable<Project>> projectLoadingState;

  // TODO(#719): Move into FeatureDetailsViewModel.
  @Hot private final FlowableProcessor<Nil> openDrawerRequests = PublishProcessor.create();

  @Hot(replays = true)
  private final MutableLiveData<BottomSheetState> bottomSheetState = new MutableLiveData<>();

  @Hot
  private final FlowableProcessor<FeatureMutation> addFeatureRequests = PublishProcessor.create();

  @Hot(replays = true)
  private final FlowableProcessor<FeatureMutation> addPolygonFeatureRequests =
      PublishProcessor.create();

  @Hot
  private final FlowableProcessor<FeatureMutation> updateFeatureRequests =
      PublishProcessor.create();

  @Hot
  private final FlowableProcessor<FeatureMutation> deleteFeatureRequests =
      PublishProcessor.create();

  @Hot private final Flowable<Feature> addFeatureResults;
  @Hot private final Flowable<Boolean> updateFeatureResults;
  @Hot private final Flowable<Boolean> deleteFeatureResults;

  @Hot private final FlowableProcessor<Throwable> errors = PublishProcessor.create();

  @Hot(replays = true)
  private final MutableLiveData<Boolean> addFeatureButtonVisible = new MutableLiveData<>(false);

  @Hot
  private final Subject<ImmutableList<Feature>> showFeatureSelectorRequests =
      PublishSubject.create();

  private Subject<Pair<ImmutableList<Layer>, Point>> showAddFeatureDialogRequests =
      PublishSubject.create();

  @Inject
  HomeScreenViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      Navigator navigator,
      UserRepository userRepository) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.navigator = navigator;
    this.userRepository = userRepository;

    projectLoadingState =
        LiveDataReactiveStreams.fromPublisher(
            projectRepository
                .getProjectLoadingState()
                .doAfterNext(this::onProjectLoadingStateChange));
    addFeatureResults =
        addFeatureRequests.switchMapSingle(
            mutation ->
                featureRepository
                    .applyAndEnqueue(mutation)
                    .andThen(featureRepository.getFeature(mutation))
                    .doOnError(errors::onNext)
                    .onErrorResumeNext(Single.never())); // Prevent from breaking upstream.
    deleteFeatureResults =
        deleteFeatureRequests.switchMapSingle(
            mutation ->
                toBooleanSingle(featureRepository.applyAndEnqueue(mutation), errors::onNext));
    updateFeatureResults =
        updateFeatureRequests.switchMapSingle(
            mutation ->
                toBooleanSingle(featureRepository.applyAndEnqueue(mutation), errors::onNext));
  }

  /** Handle state of the UI elements depending upon the active project. */
  private void onProjectLoadingStateChange(Loadable<Project> project) {
    addFeatureButtonVisible.postValue(shouldShowAddFeatureButton(project));
  }

  private boolean shouldShowAddFeatureButton(Loadable<Project> project) {
    // Project must contain at least one layer that the user can modify for add feature button to be
    // shown.
    ImmutableList<Layer> modifiableLayers =
        projectRepository.getModifiableLayers(project.value(), FeatureType.POINT);
    return !modifiableLayers.isEmpty();
  }

  public LiveData<Boolean> isAddFeatureButtonVisible() {
    return addFeatureButtonVisible;
  }

  @Hot
  public Observable<ImmutableList<Feature>> getShowFeatureSelectorRequests() {
    return showFeatureSelectorRequests;
  }

  public void onAddFeatureButtonClick(Point point) {
    ImmutableList<Layer> layers =
        projectRepository.getModifiableLayers(getActiveProject(), FeatureType.POINT);
    // TODO: Pause location updates while dialog is open.
    if (layers.size() == 1) {
      addFeature(layers.get(0), point);
    } else {
      showAddFeatureDialogRequests.onNext(Pair.create(layers, point));
    }
  }

  public Observable<Pair<ImmutableList<Layer>, Point>> getShowAddFeatureDialogRequests() {
    return showAddFeatureDialogRequests;
  }

  public Flowable<Feature> getAddFeatureResults() {
    return addFeatureResults;
  }

  public Flowable<Boolean> getUpdateFeatureResults() {
    return updateFeatureResults;
  }

  public Flowable<Boolean> getDeleteFeatureResults() {
    return deleteFeatureResults;
  }

  public Flowable<Throwable> getErrors() {
    return errors;
  }

  public void addFeature(Layer layer, Point point) {
    getActiveProject()
        .map(Project::getId)
        .ifPresentOrElse(
            projectId ->
                addFeatureRequests.onNext(
                    featureRepository.newMutation(projectId, layer.getId(), point)),
            () -> {
              throw new IllegalStateException("Empty project");
            });
  }

  public void updateFeature(Feature feature) {
    updateFeatureRequests.onNext(
        feature.toMutation(Type.UPDATE, userRepository.getCurrentUser().getId()));
  }

  public void deleteFeature(Feature feature) {
    deleteFeatureRequests.onNext(
        feature.toMutation(Type.DELETE, userRepository.getCurrentUser().getId()));
  }

  public boolean shouldShowProjectSelectorOnStart() {
    return projectRepository.getLastActiveProjectId().isEmpty();
  }

  public Flowable<Nil> getOpenDrawerRequests() {
    return openDrawerRequests;
  }

  public void openNavDrawer() {
    openDrawerRequests.onNext(NIL);
  }

  public LiveData<Loadable<Project>> getProjectLoadingState() {
    return projectLoadingState;
  }

  public LiveData<BottomSheetState> getBottomSheetState() {
    return bottomSheetState;
  }

  public void onMarkerClick(MapPin marker) {
    showBottomSheet(marker.getFeature());
  }

  public void onFeatureSelected(Feature feature) {
    showBottomSheet(feature);
  }

  private void showBottomSheet(Feature feature) {
    Timber.d("showing bottom sheet");
    isObservationButtonVisible.setValue(true);
    bottomSheetState.setValue(BottomSheetState.visible(feature));
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

  public void onFeatureClick(ImmutableList<MapFeature> mapFeatures) {
    ImmutableList<Feature> features =
        stream(mapFeatures)
            .map(MapFeature::getFeature)
            .filter(Objects::nonNull)
            .collect(toImmutableList());

    if (features.isEmpty()) {
      Timber.e("onFeatureClick called with empty or null map features");
      return;
    }

    if (features.size() == 1) {
      onFeatureSelected(features.get(0));
      return;
    }

    showFeatureSelectorRequests.onNext(features);
  }

  public Optional<Project> getActiveProject() {
    return Loadable.getValue(getProjectLoadingState());
  }

  public void showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus());
  }
}
