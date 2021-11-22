/*
 * Copyright 2020 Google LLC
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

package com.google.android.gnd.ui.offlinebasemap.viewer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.databinding.OfflineBaseMapViewerFragBinding;
import com.google.android.gnd.model.basemap.OfflineArea;
import com.google.android.gnd.ui.common.AbstractMapViewerFragment;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.map.MapFragment;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * The OfflineAreaViewerFragment provides a UI for managing a single offline area on the user's
 * device.
 */
@AndroidEntryPoint
public class OfflineAreaViewerFragment extends AbstractMapViewerFragment {

  @Inject Navigator navigator;

  private OfflineAreaViewerViewModel viewModel;

  @Inject
  public OfflineAreaViewerFragment() {}

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    OfflineAreaViewerFragmentArgs args =
        OfflineAreaViewerFragmentArgs.fromBundle(getArguments());
    viewModel = getViewModel(OfflineAreaViewerViewModel.class);
    viewModel.loadOfflineArea(args);
    viewModel.getOfflineArea().observe(this, this::panMap);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineBaseMapViewerFragBinding binding =
        OfflineBaseMapViewerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.removeButton.setOnClickListener(__ -> onRemoveClick());
    ((MainActivity) getActivity()).setActionBar(binding.offlineAreaViewerToolbar, true);
    return binding.getRoot();
  }

  @Override
  protected void onMapReady(MapFragment map) {
    map.disableGestures();
  }

  private void panMap(OfflineArea offlineArea) {
    getMapFragment().setViewport(offlineArea.getBounds());
  }

  /** Removes the area associated with this fragment from the user's device. */
  public void onRemoveClick() {
    viewModel.removeArea();
  }
}
