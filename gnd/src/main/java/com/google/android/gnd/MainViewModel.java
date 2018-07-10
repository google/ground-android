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

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.Nullable;
import android.support.v4.view.WindowInsetsCompat;
import android.util.Log;
import android.view.View;
import androidx.navigation.NavController;
import com.google.android.gnd.repository.RecordSummary;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.recorddetails.RecordDetailsFragmentDirections;
import javax.inject.Inject;

public class MainViewModel extends ViewModel {
  private MutableLiveData<WindowInsetsCompat> windowInsetsLiveData;
  private MutableLiveData<MainViewState> mainViewState;

  @Inject
  public MainViewModel() {
    windowInsetsLiveData = new MutableLiveData<>();
    mainViewState = new MutableLiveData<>();
    mainViewState.setValue(new MainViewState(MainViewState.View.HOME));
  }

  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsetsLiveData;
  }

  WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
    windowInsetsLiveData.setValue(insets);
    return insets;
  }

  // TODO: Always use ids instead of obj.
  public void showRecordDetails(RecordSummary recordSummary) {
    showRecordDetails(
      recordSummary.getRecord().getProject().getId(),
      recordSummary.getRecord().getPlace().getId(),
      recordSummary.getRecord().getId());
  }

  public void showRecordDetails(String projectId, String placeId, String recordId) {
    mainViewState.setValue(MainViewState.recordDetails(projectId, placeId, recordId));
  }

  public void editRecord(String projectId, String placeId, String recordId) {
    mainViewState.setValue(MainViewState.editRecord(projectId, placeId, recordId));
  }

  public LiveData<MainViewState> getViewState() {
    return mainViewState;
  }

  /**
   * Top-level view state for application. Contains IDs instead of actual objects to reduce surface
   * area of shared state; to allow UIs to be reinitialized after independently, each Fragment is
   * responsible for restoring its own ViewModel state from IDs.
   */
  // TODO: Merge PlaceSheetState into this state.
  // TODO: Let views emit NavDirections directly instead of this wrapper?
  public static class MainViewState {
    private static final String TAG = MainViewState.class.getSimpleName();

    enum View {
      HOME,
      PLACE_SHEET,
      RECORD_DETAILS,
      EDIT_RECORD
    }

    // TODO: Rename to something other than View.
    private View view;
    @Nullable private String projectId;
    @Nullable private String placeId;
    @Nullable private String recordId;

    MainViewState(View view) {
      this.view = view;
    }

    MainViewState(View view, String projectId, String placeId, String recordId) {
      this.view = view;
      this.projectId = projectId;
      this.placeId = placeId;
      this.recordId = recordId;
    }

    public static MainViewState recordDetails(String projectId, String placeId, String recordId) {
      return new MainViewState(View.RECORD_DETAILS, projectId, placeId, recordId);
    }

    public static MainViewState editRecord(String projectId, String placeId, String recordId) {
      return new MainViewState(View.EDIT_RECORD, projectId, placeId, recordId);
    }

    public View getView() {
      return view;
    }

    public void navigate(NavController navController) {
      switch (view) {
        case RECORD_DETAILS:
          navController.navigate(
              HomeScreenFragmentDirections.showRecordDetails(projectId, placeId, recordId));
          break;
        case EDIT_RECORD:
          navController.navigate(
            RecordDetailsFragmentDirections.editRecord("", projectId, placeId, recordId));
          break;
        default:
          Log.e(TAG, "Unimplemented transition: " + view);
      }
    }
  }
}
