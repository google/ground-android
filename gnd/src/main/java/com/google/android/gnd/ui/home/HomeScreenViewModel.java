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

import android.util.Log;
import android.view.View;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.repository.Loadable;
import com.google.android.gnd.repository.ProjectRepository;
import com.google.android.gnd.rx.Action;
import com.google.android.gnd.rx.Event;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.map.MapMarker;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;
import javax.annotation.Nullable;
import javax.inject.Inject;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  private static final String TAG = HomeScreenViewModel.class.getSimpleName();
  private final ProjectRepository projectRepository;
  private final Navigator navigator;
  /** The state and value of the currently active project (loading, loaded, etc.). */
  private final LiveData<Loadable<Project>> activeProject;

  private final PublishSubject<Feature> addFeatureClicks;

  // TODO: Move into MapContainersViewModel
  private final MutableLiveData<Event<Point>> addFeatureDialogRequests;

  // TODO: Move into FeatureSheetViewModel.
  private final MutableLiveData<Action> openDrawerRequests;
  private final MutableLiveData<FeatureSheetState> featureSheetState;
  private final MutableLiveData<Integer> addObservationButtonVisibility =
      new MutableLiveData<>(View.GONE);
  @Nullable private Form selectedForm;

  @Inject
  HomeScreenViewModel(
      ProjectRepository projectRepository,
      FeatureRepository featureRepository,
      Navigator navigator) {
    this.projectRepository = projectRepository;
    this.addFeatureDialogRequests = new MutableLiveData<>();
    this.openDrawerRequests = new MutableLiveData<>();
    this.featureSheetState = new MutableLiveData<>();
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
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::showFeatureSheet));
  }

  public boolean shouldShowProjectSelectorOnStart() {
    return projectRepository.getLastActiveProjectId().isEmpty();
  }

  public MutableLiveData<Integer> getAddObservationButtonVisibility() {
    return addObservationButtonVisibility;
  }

  private void onAddFeatureError(Throwable throwable) {
    // TODO: Show an error message to the user.
    Log.e(TAG, "Couldn't add feature.", throwable);
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

  public LiveData<FeatureSheetState> getFeatureSheetState() {
    return featureSheetState;
  }

  // TODO: Remove extra indirection here?
  public void onMarkerClick(MapMarker marker) {
    marker.getFeature().ifPresent(this::showFeatureSheet);
  }

  private void showFeatureSheet(Feature feature) {
    addObservationButtonVisibility.setValue(View.VISIBLE);
    featureSheetState.setValue(FeatureSheetState.visible(feature));
  }

  public void onAddFeatureBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addFeatureDialogRequests.setValue(Event.create(location));
  }

  public void addFeature(Feature feature) {
    addFeatureClicks.onNext(feature);
  }

  public void onBottomSheetHidden() {
    featureSheetState.setValue(FeatureSheetState.hidden());
    addObservationButtonVisibility.setValue(View.GONE);
  }

  public void onFormChange(Form form) {
    this.selectedForm = form;
  }

  public void addObservation() {
    FeatureSheetState state = featureSheetState.getValue();
    if (state == null) {
      Log.e(TAG, "Missing featureSheetState");
      return;
    }
    if (selectedForm == null) {
      Log.e(TAG, "Missing form");
      return;
    }
    Feature feature = state.getFeature();
    if (feature == null) {
      Log.e(TAG, "Missing feature");
      return;
    }
    Project project = feature.getProject();
    if (project == null) {
      Log.e(TAG, "Missing project");
      return;
    }
    navigator.addObservation(project.getId(), feature.getId(), selectedForm.getId());
  }

  public void init() {
    // Last active project will be loaded once view subscribes to activeProject.
    projectRepository.getLastActiveProjectId().ifPresent(projectRepository::activateProject);
  }

  // TODO: Move to OfflineAreaViewModel
  public void showBasemapSelector() {
    navigator.showBasemapSelector();
  }

  public void showOfflineAreas() {
    navigator.showOfflineAreas();
  }
}
