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
import com.google.android.gnd.repository.GndDataRepository;
import com.google.android.gnd.repository.ProjectActivationEvent;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.ui.AddPlaceDialogFragment.AddPlaceRequest;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.PlaceType;
import com.google.android.gnd.vo.Project;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;

public class MainViewModel extends ViewModel {
  private static final String TAG = MainViewModel.class.getSimpleName();
  private final GndDataRepository dataRepository;
  private final LiveData<ProjectActivationEvent> projectActivationEvents;
  private final MutableLiveData<List<Project>> showProjectSelectorDialogRequests;
  private final MutableLiveData<Point> addPlaceDialogRequests;
  private final MutableLiveData<PlaceSheetEvent> placeSheetEvents;

  @Inject
  MainViewModel(GndDataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.showProjectSelectorDialogRequests = new MutableLiveData<>();
    this.addPlaceDialogRequests = new MutableLiveData<>();
    this.projectActivationEvents = RxLiveData.fromFlowable(dataRepository.activeProject());
    this.placeSheetEvents = new MutableLiveData<>();
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

  public LiveData<PlaceSheetEvent> getPlaceSheetEvents() {
    return placeSheetEvents;
  }

  @SuppressLint("CheckResult")
  public void showProjectSelectorDialog() {
    // TODO: Dispose of this and other subscriptions correctly.
    dataRepository.getProjectSummaries().subscribe(showProjectSelectorDialogRequests::setValue);
  }

  public void onMarkerClick(MapMarker marker) {
    if (marker.getObject() instanceof Place) {
      Place place = (Place) marker.getObject();
      Optional<PlaceType> placeType = dataRepository.getPlaceType(place.getPlaceTypeId());
      if (!placeType.isPresent()) {
        Log.e(TAG, "Place " + place.getId() + " has unknown type: " + place.getPlaceTypeId());
        // TODO: Show error message to user.
        return;
      }
      placeSheetEvents.setValue(PlaceSheetEvent.show(placeType.get(), place));
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

  public void onBottomSheetHidden() {
    placeSheetEvents.setValue(PlaceSheetEvent.hide());
  }
}
