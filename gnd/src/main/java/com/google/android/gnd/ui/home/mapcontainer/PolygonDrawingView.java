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
import android.util.AttributeSet;
import androidx.annotation.Nullable;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.PolygonDrawingControlsBinding;
import com.google.android.gnd.ui.common.AbstractView;
import com.google.android.gnd.ui.map.CameraPosition;
import com.google.android.gnd.ui.map.MapFragment;
import dagger.hilt.android.AndroidEntryPoint;
import dagger.hilt.android.WithFragmentBindings;

@WithFragmentBindings
@AndroidEntryPoint
public class PolygonDrawingView extends AbstractView {

  private final PolygonDrawingViewModel viewModel;
  private final PolygonDrawingControlsBinding binding;

  public PolygonDrawingView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);

    viewModel = getViewModel(PolygonDrawingViewModel.class);
    binding = (PolygonDrawingControlsBinding) inflate(R.layout.polygon_drawing_controls);
    binding.setViewModel(viewModel);
  }

  public void init(MapFragment mapFragment) {
    mapFragment
        .getCameraMovedEvents()
        .map(CameraPosition::getTarget)
        .onBackpressureLatest()
        .as(disposeOnDestroy(getActivity()))
        .subscribe(viewModel::onCameraMoved);

    // Using this approach as data binding approach did not work with view.
    viewModel
        .getPolygonDrawingCompletedVisibility()
        .observe(
            getActivity(),
            visible -> {
              binding.completePolygonButton.setVisibility(visible == 4 ? INVISIBLE : VISIBLE);
              binding.addPolygonButton.setVisibility(visible == 4 ? VISIBLE : INVISIBLE);
            });
  }
}
