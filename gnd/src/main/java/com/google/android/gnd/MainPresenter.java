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
import com.google.android.gnd.model.GndDataRepository;
import com.google.android.gnd.model.Place;
import com.google.android.gnd.system.LocationManager;

public class MainPresenter {
  private static final String TAG = MainPresenter.class.getSimpleName();
  private final MainActivity mainActivity;
  private final DataSheetPresenter dataSheetPresenter;
  private final GndDataRepository model;

  MainPresenter(
      MainActivity mainActivity,
      GndDataRepository model) {
    this.mainActivity = mainActivity;
    this.model = model;
    // TODO: Resolve circular deps and inject these presenters.
    this.dataSheetPresenter = new DataSheetPresenter(this, mainActivity, model);
  }

  void onCreate(Bundle savedInstanceState) {
    dataSheetPresenter.onCreate(savedInstanceState);
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
                              mainActivity.enableAddPlaceButton();
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

  public void showPlaceDetails(Place place) {
    try {
      dataSheetPresenter.showPlaceDetails(place);
      // TODO: update main view mode?
      //      uiMode = UiMode.VIEW;
    } catch (UserOperationFailed e) {
      // TODO: Show error in toast or snackbar.
      Log.e(TAG, e.getMessage());
    }
  }

  public boolean onToolbarSaveButtonClick() {
    return dataSheetPresenter.onSaveClick();
  }
}
