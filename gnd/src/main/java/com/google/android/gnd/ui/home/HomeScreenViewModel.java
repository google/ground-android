/*
 * Copyright 2019 Google LLC
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
import android.util.Log;
import com.google.android.gnd.repository.DataRepository;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractViewModel;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.SharedViewModel;
import com.google.android.gnd.ui.common.SingleLiveEvent;
import com.google.android.gnd.ui.map.MapMarker;
import com.google.android.gnd.vo.Feature;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import javax.annotation.Nullable;
import javax.inject.Inject;

@SharedViewModel
public class HomeScreenViewModel extends AbstractViewModel {

  private static final String TAG = HomeScreenViewModel.class.getSimpleName();
  private final DataRepository dataRepository;
  private final Navigator navigator;
  private final LiveData<Resource<Project>> activeProject;

  // TODO: Move into MapContainersViewModel
  private final SingleLiveEvent<Point> addFeatureDialogRequests;

  // TODO: Move into FeatureSheetViewModel.
  private final SingleLiveEvent<Void> openDrawerRequests;
  private final MutableLiveData<FeatureSheetState> featureSheetState;
  @Nullable private Form selectedForm;

  @Inject
  HomeScreenViewModel(DataRepository dataRepository, Navigator navigator) {
    this.dataRepository = dataRepository;
    this.addFeatureDialogRequests = new SingleLiveEvent<>();
    this.openDrawerRequests = new SingleLiveEvent<>();
    this.featureSheetState = new MutableLiveData<>();
    this.activeProject = LiveDataReactiveStreams.fromPublisher(dataRepository.getActiveProject());
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

  public LiveData<Point> getShowAddFeatureDialogRequests() {
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
    featureSheetState.setValue(FeatureSheetState.visible(feature));
  }

  private void onFeatureAdded(Feature feature) {
    showFeatureSheet(feature);
  }

  public void onAddFeatureBtnClick(Point location) {
    // TODO: Pause location updates while dialog is open.
    addFeatureDialogRequests.setValue(location);
  }

  public void addFeature(Feature feature) {
    // TODO(#24): Fix leaky subscriptions!
    disposeOnClear(dataRepository.addFeature(feature).subscribe(this::onFeatureAdded));
  }

  public void onBottomSheetHidden() {
    featureSheetState.setValue(FeatureSheetState.hidden());
  }

  public void onFormChange(Form form) {
    this.selectedForm = form;
  }

  public void addRecord() {
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
    navigator.addRecord(feature.getProject().getId(), feature.getId(), selectedForm.getId());
  }
}
