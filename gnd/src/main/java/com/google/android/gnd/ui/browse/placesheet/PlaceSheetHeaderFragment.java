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

package com.google.android.gnd.ui.browse.placesheet;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gnd.R;
import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.ui.browse.BrowseViewModel;
import com.google.android.gnd.ui.browse.PlaceSheetState;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.AbstractViewModelFactory;

import javax.inject.Inject;

import butterknife.BindView;

public class PlaceSheetHeaderFragment extends AbstractFragment {
  @Inject
  AbstractViewModelFactory viewModelFactory;

  @BindView(R.id.place_sheet_title)
  TextView placeSheetTitle;

  @BindView(R.id.place_sheet_subtitle)
  TextView placeSheetSubtitle;

  @BindView(R.id.place_header_icon)
  ImageView placeHeaderIcon;

  private BrowseViewModel browseViewModel;

  @Inject
  public PlaceSheetHeaderFragment() {}

  @Override
  protected View createView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_place_sheet_header, container, false);
  }

  @Override
  protected void obtainViewModels() {
    browseViewModel =
      ViewModelProviders.of(getActivity(), viewModelFactory).get(BrowseViewModel.class);
  }

  @Override
  protected void observeViewModels() {
      browseViewModel.getPlaceSheetState().observe(this, this::onPlaceSheetStateChange);
  }

    private void onPlaceSheetStateChange(PlaceSheetState placeSheetState) {
        if (placeSheetState.isVisible()) {
      getView().setVisibility(View.VISIBLE);
      placeHeaderIcon.setImageResource(
        MapIcon.getResourceId(
                getContext(), placeSheetState.getPlace().getPlaceType().getIconId()));
            placeSheetTitle.setText(placeSheetState.getTitle());
            placeSheetSubtitle.setText(placeSheetState.getSubtitle());
      placeSheetSubtitle.setVisibility(
              placeSheetState.getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);
    } else {
      getView().setVisibility(View.GONE);
    }
  }
}
