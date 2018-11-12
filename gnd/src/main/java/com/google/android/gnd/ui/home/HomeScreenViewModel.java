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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.ActivityScope;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SingleLiveEvent;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import javax.annotation.Nullable;
import javax.inject.Inject;

@ActivityScope
public class HomeScreenViewModel extends AbstractViewModel {

  private static final String TAG = HomeScreenViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private final Navigator navigator;
  private final LiveData<Resource<Project>> activeProject;

  // TODO: Move in MapContainersViewModel
  private final SingleLiveEvent<Point> addPlaceDialogRequests;

  // TODO: Move into PlaceSheetViewModel.
  private final SingleLiveEvent<Void> openDrawerRequests;
  private final MutableLiveData<PlaceSheetState> placeSheetState;
  @Nullable private Form selectedForm;

  @Inject
  HomeScreenViewModel(DataRepository dataRepository, Navigator navigator) {
    this.dataRepository = dataRepository;
    this.addPlaceDialogRequests = new SingleLiveEvent<>();
    this.openDrawerRequests = new SingleLiveEvent<>();
    this.placeSheetState = new MutableLiveData<>();
    this.activeProject = RxLiveData.fromObservable(dataRepository.getActiveProject());
    this.navigator = navigator;
  }

  public LiveData<Void> getOpenDrawerRequests() {
    return openDrawerRequests;
  }

  public void openNavDrawer() {
    openDrawerRequests.setValue(null);
  }

  public LiveData<Resource<Project>> getActiveProject() {
    return activeProject;
  }

  public LiveData<Point> getShowAddPlaceDialogRequests() {
    return addPlaceDialogRequests;
  }

  public LiveData<PlaceSheetState> getPlaceSheetState() {
    return placeSheetState;
  }

  // TODO: Remove extra indirection here?
  public void onMarkerClick(MapMarker marker) {
    marker.getPlace().ifPresent(this::showPlaceSheet);
  }

  private void showPlaceSheet(Place place) {
    placeSheetState.setValue(PlaceSheetState.visible(place));
  }

  private void onPlaceAdded(Place place) {
    showPlaceSheet(place);
  }

  public void onAddPlaceBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addPlaceDialogRequests.setValue(location);
  }

  public void addPlace(Place place) {
    disposeOnClear(dataRepository.addPlace(place).subscribe(this::onPlaceAdded));
  }

  public void onBottomSheetHidden() {
    placeSheetState.setValue(PlaceSheetState.hidden());
  }

  public void onFormChange(Form form) {
    this.selectedForm = form;
  }

  public void addRecord() {
    PlaceSheetState state = placeSheetState.getValue();
    if (state == null) {
      Log.e(TAG, "Missing placeSheetState");
      return;
    }
    if (selectedForm == null) {
      Log.e(TAG, "Missing form");
      return;
    }
    Place place = state.getPlace();
    navigator.addRecord(place.getProject().getId(), place.getId(), selectedForm.getId());
  }
}
