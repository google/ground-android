/*
 * Copyright 2021 Google LLC
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

package com.google.android.gnd.ui.home.mapcontainer;

import static com.google.android.gnd.rx.RxAutoDispose.disposeOnDestroy;

import android.content.Context;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.MapMoveFeatureLayoutBinding;
import com.google.android.gnd.ui.common.AbstractView;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFragment;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.WithFragmentBindings;

@WithFragmentBindings
@AndroidEntryPoint
public class FeatureRepositionView extends AbstractView {

  public FeatureRepositionView(Context context, MapFragment mapFragment) {
    super(context);
    FeatureRepositionViewModel viewModel = getViewModel(FeatureRepositionViewModel.class);
    MapMoveFeatureLayoutBinding binding =
        (MapMoveFeatureLayoutBinding) inflate(R.layout.map_move_feature_layout);
    binding.setViewModel(viewModel);

    mapFragment
        .getCameraMovedEvents()
        .map(CameraPosition::getTarget)
        .onBackpressureLatest()
        .as(disposeOnDestroy(getActivity()))
        .subscribe(viewModel::onCameraMoved);
  }
}
