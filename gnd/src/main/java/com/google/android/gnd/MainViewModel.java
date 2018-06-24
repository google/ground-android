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
import android.view.View;
import com.google.android.gnd.repository.RecordSummary;
import com.google.android.gnd.vo.Record;
import javax.inject.Inject;

public class MainViewModel extends ViewModel {
  private MutableLiveData<WindowInsetsCompat> windowInsetsLiveData;
  private MutableLiveData<MainViewState> mainViewState;

  @Inject
  public MainViewModel() {
    windowInsetsLiveData = new MutableLiveData<>();
    mainViewState = new MutableLiveData<>();
    mainViewState.setValue(new MainViewState(MainViewState.View.MAP));
  }

  public LiveData<WindowInsetsCompat> getWindowInsets() {
    return windowInsetsLiveData;
  }

  WindowInsetsCompat onApplyWindowInsets(View view, WindowInsetsCompat insets) {
    windowInsetsLiveData.setValue(insets);
    return insets;
  }

  public void onRecordListItemClick(RecordSummary recordSummary) {
    mainViewState.setValue(MainViewState.viewRecord(recordSummary.getRecord()));
  }

  public LiveData<MainViewState> getViewState() {
    return mainViewState;
  }

  // TODO: Merge PlaceSheetEvent into this state.
  public static class MainViewState {
    enum View {
      MAP,
      PLACE_SHEET,
      VIEW_RECORD
    }

    private View view;
    @Nullable
    private Record record;

    MainViewState(View view) {
      this.view = view;
    }

    MainViewState(View view, Record record) {
      this.view = view;
      this.record = record;
    }

    public static MainViewState viewRecord(Record record) {
      return new MainViewState(View.VIEW_RECORD, record);
    }

    public View getView() {
      return view;
    }

    @Nullable
    public Record getRecord() {
      return record;
    }
  }
}
