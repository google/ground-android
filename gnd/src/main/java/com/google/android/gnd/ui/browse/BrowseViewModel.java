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
package com.google.android.gnd.ui.browse;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.util.Log;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.rx.RxLiveData;
import com.google.android.gnd.ui.browse.AddPlaceDialogFragment.AddPlaceRequest;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import javax.inject.Inject;

public class BrowseViewModel extends AbstractViewModel {
  private static final String TAG = BrowseViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private final LiveData<Resource<Project>> activeProject;
  // TODO: Implement this as a state and remove Consumable.
  private final MutableLiveData<Point> addPlaceDialogRequests;
  private final MutableLiveData<PlaceSheetState> placeSheetState;

  @Inject
  BrowseViewModel(DataRepository dataRepository) {
    this.dataRepository = dataRepository;
    this.addPlaceDialogRequests = new MutableLiveData<>();
    this.placeSheetState = new MutableLiveData<>();
    this.activeProject = RxLiveData.fromFlowable(dataRepository.getActiveProject());
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

  public void onMarkerClick(MapMarker marker) {
    marker.getPlace().ifPresent(place -> placeSheetState.setValue(PlaceSheetState.visible(place)));
  }

  public void onAddPlaceBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    // TODO: Ignore clicks when project not active.
    addPlaceDialogRequests.setValue(location);
  }

  public void onAddPlace(AddPlaceRequest addPlaceRequest) {
    Log.e(TAG, "TODO: Implement Add Place functionality");
  }

  public void onBottomSheetHidden() {
    placeSheetState.setValue(PlaceSheetState.hidden());
  }
}
