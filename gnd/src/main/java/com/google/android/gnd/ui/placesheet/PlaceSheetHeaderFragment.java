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

package com.google.android.gnd.ui.placesheet;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.MainViewModel;
import com.google.android.gnd.ui.PlaceSheetEvent;
import com.google.android.gnd.ui.common.GndFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import javax.inject.Inject;

public class PlaceSheetHeaderFragment extends GndFragment {
  @Inject
  GndViewModelFactory viewModelFactory;

  @BindView(R.id.place_sheet_title)
  TextView placeSheetTitle;

  @BindView(R.id.place_sheet_subtitle)
  TextView placeSheetSubtitle;

  private MainViewModel mainViewModel;

  @Inject
  public PlaceSheetHeaderFragment() {
  }

  @Override
  protected View createView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_place_sheet_header, container, false);
  }

  @Override
  protected void createViewModel() {
    mainViewModel =
      ViewModelProviders.of(getParentFragment(), viewModelFactory).get(MainViewModel.class);
  }

  @Override
  protected void observeViewModel() {
    mainViewModel.getPlaceSheetEvents().observe(this, this::onPlaceSheetEvent);
  }

  private void onPlaceSheetEvent(PlaceSheetEvent placeSheetEvent) {
    if (placeSheetEvent.isShowEvent()) {
      placeSheetTitle.setText(placeSheetEvent.getTitle());
      placeSheetSubtitle.setText(placeSheetEvent.getSubtitle());
    }
  }
}