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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.Action;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Nil;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapPin;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  public final MutableLiveData<Boolean> isObservationButtonVisible = new MutableLiveData<>(false);
  private final ProjectRepository projectRepository;
  private final FeatureRepository featureRepository;
  private final Navigator navigator;
  /** The state and value of the currently active project (loading, loaded, etc.). */
  private final LiveData<Loadable<Project>> activeProject;

  private final PublishSubject<Feature> addFeatureClicks;
  // TODO: Move into MapContainersViewModel
  private final MutableLiveData<Event<Point>> addFeatureDialogRequests;
  // TODO: Move into FeatureDetailsViewModel.
  private final MutableLiveData<Action> openDrawerRequests;
  private final MutableLiveData<BottomSheetState> bottomSheetState;

  private final FlowableProcessor<Nil> deleteFeatureRequests = PublishProcessor.create();
  private final LiveData<Boolean> deleteFeature;

  @Inject
  HomeScreenViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      Navigator navigator,
      Schedulers schedulers) {
    this.projectRepository = projectRepository;
    this.featureRepository = featureRepository;
    this.addFeatureDialogRequests = new MutableLiveData<>();
    this.openDrawerRequests = new MutableLiveData<>();
    this.bottomSheetState = new MutableLiveData<>();
    this.activeProject =
        LiveDataReactiveStreams.fromPublisher(projectRepository.getActiveProjectOnceAndStream());
    this.navigator = navigator;
    this.addFeatureClicks = PublishSubject.create();

    disposeOnClear(
        addFeatureClicks
            .switchMapSingle(
                newFeature ->
                    featureRepository
                        .saveFeature(newFeature)
                        .toSingleDefault(newFeature)
                        .doOnError(this::onAddFeatureError)
                        .onErrorResumeNext(Single.never())) // Prevent from breaking upstream.
            .observeOn(schedulers.ui())
            .subscribe(this::onAddFeature));

    deleteFeature =
        LiveDataReactiveStreams.fromPublisher(
            deleteFeatureRequests.switchMapSingle(
                __ -> deleteActiveFeature().toSingleDefault(true).onErrorReturnItem(false)));
  }

  public LiveData<Boolean> getDeleteFeature() {
    return deleteFeature;
  }

  public void deleteFeature() {
    deleteFeatureRequests.onNext(Nil.NIL);
  }

  private void onAddFeature(Feature feature) {
    if (feature.getLayer().getForm().isPresent()) {
      addNewObservation(feature);
    }
  }

  private void addNewObservation(Feature feature) {
    String projectId = feature.getProject().getId();
    String featureId = feature.getId();
    String formId = feature.getLayer().getForm().get().getId();
    navigator.addObservation(projectId, featureId, formId);
  }

  public boolean shouldShowProjectSelectorOnStart() {
    return projectRepository.getLastActiveProjectId().isEmpty();
  }

  private void onAddFeatureError(Throwable throwable) {
    // TODO: Show an error message to the user.
    Timber.e(throwable, "Couldn't add feature.");
  }

  public LiveData<Action> getOpenDrawerRequests() {
    return openDrawerRequests;
  }

  public void openNavDrawer() {
    openDrawerRequests.setValue(Action.create());
  }

  public LiveData<Loadable<Project>> getActiveProject() {
    return activeProject;
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
    bottomSheetState.postValue(BottomSheetState.visible(feature));
  }

  public void onAddFeatureBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addFeatureDialogRequests.setValue(Event.create(location));
  }

  public void addFeature(Feature feature) {
    addFeatureClicks.onNext(feature);
  }

  public Completable deleteActiveFeature() {
    BottomSheetState state = bottomSheetState.getValue();
    if (state == null) {
      return Completable.error(new IllegalStateException("Missing bottomSheetState"));
    }
    Feature feature = state.getFeature();
    if (feature == null) {
      Timber.e("Missing feature");
      return Completable.error(new IllegalStateException("Missing feature"));
    }
    return featureRepository.deleteFeature(feature);
  }

  public void onBottomSheetHidden() {
    bottomSheetState.postValue(BottomSheetState.hidden());
    isObservationButtonVisible.setValue(false);
  }

  public void addObservation() {
    BottomSheetState state = bottomSheetState.getValue();
    if (state == null) {
      Timber.e("Missing bottomSheetState");
      return;
    }
    Feature feature = state.getFeature();
    if (feature == null) {
      Timber.e("Missing feature");
      return;
    }
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
    navigator.addObservation(project.getId(), feature.getId(), form.get().getId());
  }

  public void init() {
    // Last active project will be loaded once view subscribes to activeProject.
    projectRepository.getLastActiveProjectId().ifPresent(projectRepository::activateProject);
  }

  public void showOfflineAreas() {
    navigator.showOfflineAreas();
  }

  public void showSettings() {
    navigator.showSettings();
  }
}
