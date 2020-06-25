/*
 * Copyright 2019 Google LLC
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

package com.google.android.gnd.ui.offlinearea;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.OfflineAreasFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.Navigator;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/**
 * Fragment containing a list of downloaded areas on the device. An area is a set of offline raster
 * tiles. Users can manage their areas within this fragment. They can delete areas they no longer
 * need or access the UI used to select and download a new area to the device.
 */
@AndroidEntryPoint
public class OfflineAreasFragment extends AbstractFragment {
  @Inject Navigator navigator;

  // TODO: Remove. Right now removing this results in runtime crashes. Not precisely sure why.
  // It stems from AbstractFragment and its use of ButterKnife.
  @BindView(R.id.offline_areas_list)
  RecyclerView areaList;

  private OfflineAreasViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineAreasViewModel.class);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineAreasFragBinding binding = OfflineAreasFragBinding.inflate(inflater, container, false);

    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);

    ((MainActivity) getActivity()).setActionBar(binding.offlineAreasToolbar, true);

    OfflineAreaListAdapter offlineAreaListAdapter = new OfflineAreaListAdapter(navigator);
    RecyclerView recyclerView = binding.offlineAreasList;
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(offlineAreaListAdapter);

    viewModel.getOfflineAreas().observe(getViewLifecycleOwner(), offlineAreaListAdapter::update);

    return binding.getRoot();
  }
}
