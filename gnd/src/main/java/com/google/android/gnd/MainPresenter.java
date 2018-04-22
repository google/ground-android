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

package com.google.android.gnd;

import static java8.util.stream.StreamSupport.stream;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import com.google.android.gnd.model.Feature;
import com.google.android.gnd.model.FeatureType;
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.system.LocationManager;
import com.google.android.gnd.system.PermissionManager;

public class MainPresenter {
  private static final String TAG = MainPresenter.class.getSimpleName();
  private final MainActivity mainActivity;
  private final PermissionManager permissionManager;
  private final DataSheetPresenter dataSheetPresenter;
  private final MapPresenter mapPresenter;
  private final GndDataRepository model;

  MainPresenter(
      MainActivity mainActivity,
      GndDataRepository model,
      PermissionManager permissionManager,
      LocationManager locationManager) {
    this.mainActivity = mainActivity;
    this.model = model;
    this.permissionManager = permissionManager;
    // TODO: Resolve circular deps and inject these presenters.
    this.dataSheetPresenter = new DataSheetPresenter(this, mainActivity, model);
    this.mapPresenter = new MapPresenter(this, mainActivity, model, locationManager);
  }

  public MapPresenter getMapPresenter() {
    return mapPresenter;
  }

  void onCreate(Bundle savedInstanceState) {
    dataSheetPresenter.onCreate(savedInstanceState);
    mapPresenter.onCreate(savedInstanceState);
    model.onCreate();
    showProjectSelector();
  }

  public void showProjectSelector() {
    model
        .getProjectSummaries()
        .thenAccept(
            summaries -> {
              AlertDialog.Builder dialog = new AlertDialog.Builder(mainActivity);
              dialog.setTitle(R.string.select_project_dialog_title);
              // TODO: i18n.
              String[] projectTitles =
                  stream(summaries)
                      .map(p -> p.getTitleOrDefault("pt", "<Untitled>"))
                      .toArray(String[]::new);
              dialog.setItems(
                  projectTitles,
                  (d, which) -> {
                    // TODO: ProgressDialog is deprecated; replace with custom dialog.
                    mainActivity.showProjectLoadingDialog();
                    model
                        .activateProject(summaries.get(which).getId())
                        .thenAccept(
                            project -> {
                              mainActivity.enableAddFeatureButton();
                              mainActivity.dismissLoadingDialog();
                            })
                        .exceptionally(
                            e -> {
                              Log.e(TAG, "Error activating project", e);
                              mainActivity.showErrorMessage(e.getCause().toString());
                              return null;
                            });
                  });
              dialog.setCancelable(false);
              dialog.show();
            });
  }

  void onStart() {
    mapPresenter.onStart();
  }

  void onResume() {
    mapPresenter.onResume();
  }

  void onPause() {
    mapPresenter.onPause();
  }

  void onStop() {
    mapPresenter.onStop();
  }

  void onDestroy() {
    mapPresenter.onDestroy();
  }

  void onLowMemory() {
    mapPresenter.onLowMemory();
  }

  public PermissionManager getPermissionManager() {
    return permissionManager;
  }

  public void showFeatureDetails(Feature feature) {
    try {
      dataSheetPresenter.showFeatureDetails(feature);
      // TODO: update main view mode?
      //      uiMode = UiMode.VIEW;
    } catch (UserOperationFailed e) {
      // TODO: Show error in toast or snackbar.
      Log.e(TAG, e.getMessage());
    }
  }

  public void onAddFeatureClick() {
    if (model.getOldActiveProject() != null) {
      mainActivity.showAddFeatureDialog(
          model.getOldActiveProject().getFeatureTypesList(), this::onSelectFeatureTypeForAdd);
    }
  }

  private void onSelectFeatureTypeForAdd(FeatureType featureType) {
    dataSheetPresenter.onSelectFeatureTypeForAdd(featureType);
  }

  public boolean onToolbarSaveButtonClick() {
    return dataSheetPresenter.onSaveClick();
  }

  public GndDataRepository getModel() {
    return model;
  }

  public void onMapReady() {
  }
}
