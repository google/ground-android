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

package com.google.android.gnd.ui.home.placesheet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.MapIcon;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.ui.home.PlaceSheetState;
import javax.inject.Inject;

public class PlaceSheetHeaderFragment extends AbstractFragment {
  @BindView(R.id.place_sheet_title)
  TextView placeSheetTitle;

  @BindView(R.id.place_sheet_subtitle)
  TextView placeSheetSubtitle;

  @BindView(R.id.place_header_icon)
  ImageView placeHeaderIcon;

  private HomeScreenViewModel homeScreenViewModel;

  @Inject
  public PlaceSheetHeaderFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    homeScreenViewModel =
        get(HomeScreenViewModel.class);
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.place_sheet_header_frag, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    homeScreenViewModel.getPlaceSheetState().observe(this, this::onPlaceSheetStateChange);
  }

  private void onPlaceSheetStateChange(PlaceSheetState placeSheetState) {
    if (placeSheetState.isVisible()) {
      getView().setVisibility(View.VISIBLE);
      placeHeaderIcon.setImageResource(
          MapIcon.getResourceId(
              getContext(), placeSheetState.getPlace().getPlaceType().getIconId()));
      placeSheetTitle.setText(placeSheetState.getPlace().getTitle());
      placeSheetSubtitle.setText(placeSheetState.getPlace().getSubtitle());
      placeSheetSubtitle.setVisibility(
          placeSheetState.getPlace().getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);
    } else {
      getView().setVisibility(View.GONE);
    }
  }
}
