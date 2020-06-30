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

import android.view.View;
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
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapPin;
import io.reactivex.Single;
import io.reactivex.subjects.PublishSubject;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  private final ProjectRepository projectRepository;
  private final Navigator navigator;
  /** The state and value of the currently active project (loading, loaded, etc.). */
  private final LiveData<Loadable<Project>> activeProject;

  private final PublishSubject<Feature> addFeatureClicks;

  // TODO: Move into MapContainersViewModel
  private final MutableLiveData<Event<Point>> addFeatureDialogRequests;

  // TODO: Move into FeatureDetailsViewModel.
  private final MutableLiveData<Action> openDrawerRequests;
  private final MutableLiveData<BottomSheetState> bottomSheetState;
  private final MutableLiveData<Integer> addObservationButtonVisibility =
      new MutableLiveData<>(View.GONE);

  @Inject
  HomeScreenViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      AuthenticationManager authManager,
      Navigator navigator,
      Schedulers schedulers) {
    this.projectRepository = projectRepository;
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
                        .saveFeature(newFeature, authManager.getCurrentUser())
                        .toSingleDefault(newFeature)
                        .doOnError(this::onAddFeatureError)
                        .onErrorResumeNext(Single.never())) // Prevent from breaking upstream.
            .filter(feature -> feature.getLayer().getForm().isPresent())
            .doOnNext(this::addNewObservation)
            .observeOn(schedulers.ui())
            .subscribe());
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

  public MutableLiveData<Integer> getAddObservationButtonVisibility() {
    return addObservationButtonVisibility;
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
    addObservationButtonVisibility.setValue(View.VISIBLE);
    bottomSheetState.setValue(BottomSheetState.visible(feature));
  }

  public void onAddFeatureBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addFeatureDialogRequests.setValue(Event.create(location));
  }

  public void addFeature(Feature feature) {
    addFeatureClicks.onNext(feature);
  }

  public void onBottomSheetHidden() {
    bottomSheetState.setValue(BottomSheetState.hidden());
    addObservationButtonVisibility.setValue(View.GONE);
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
