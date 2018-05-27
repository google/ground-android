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
package com.google.android.gnd.ui;

import android.annotation.SuppressLint;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.Log;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.ProjectActivationEvent;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.ui.AddPlaceDialogFragment.AddPlaceRequest;
import com.google.android.gnd.ui.map.MapMarker;
import java.util.List;
import javax.inject.Inject;

public class MainViewModel extends ViewModel {
  private static final String TAG = MainViewModel.class.getSimpleName();
  private final GndDataRepository dataRepository;
  private final LiveData<ProjectActivationEvent> projectActivationEvents;
  private final MutableLiveData<List<Project>> showProjectSelectorDialogRequests;
  private final MutableLiveData<Point> addPlaceDialogRequests;
  private final MutableLiveData<Place> showPlaceSheetRequests;

  @Inject
  MainViewModel(GndDataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.showProjectSelectorDialogRequests = new MutableLiveData<>();
    this.addPlaceDialogRequests = new MutableLiveData<>();
    this.projectActivationEvents = RxLiveData.fromObservable(dataRepository.activeProject());
    this.showPlaceSheetRequests = new MutableLiveData<>();
  }

  public LiveData<List<Project>> showProjectSelectorDialogRequests() {
    return showProjectSelectorDialogRequests;
  }

  public LiveData<ProjectActivationEvent> projectActivationEvents() {
    return projectActivationEvents;
  }

  public LiveData<Point> showAddPlaceDialogRequests() {
    return addPlaceDialogRequests;
  }

  public LiveData<Place> getShowPlaceSheetRequests() {
    return showPlaceSheetRequests;
  }

  @SuppressLint("CheckResult")
  public void showProjectSelectorDialog() {
    // TODO: Dispose of this and other subscriptions correctly.
    dataRepository.getProjectSummaries().subscribe(showProjectSelectorDialogRequests::setValue);
  }

  public void onMarkerClick(MapMarker marker) {
    Log.d(TAG, "User clicked marker");
    if (marker.getObject() instanceof Place) {
      Log.e(TAG, "TODO: Implement onMarkerClick");
      showPlaceSheetRequests.setValue((Place) marker.getObject());
    }
  }

  public void onAddPlaceBtnClick(Point location) {
    if (projectActivationEvents.getValue().isActivated()) {
      // TODO: Pause location updates while dialog is open.
      addPlaceDialogRequests.setValue(location);
    }
  }

  public void onAddPlace(AddPlaceRequest addPlaceRequest) {
    Log.e(TAG, "TODO: Implement Add Place functionality");
  }

  public void onPlaceSheetCollapsed() {
  }

  public void onPlaceSheetHidden() {
  }
}
