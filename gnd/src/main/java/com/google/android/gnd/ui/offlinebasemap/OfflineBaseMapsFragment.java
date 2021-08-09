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

package com.google.android.gnd.ui.offlinebasemap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.databinding.OfflineBaseMapsFragBinding;
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
public class OfflineBaseMapsFragment extends AbstractFragment {
  @Inject Navigator navigator;

  private OfflineBaseMapListAdapter offlineBaseMapListAdapter;
  private OfflineBaseMapsViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineBaseMapsViewModel.class);
    offlineBaseMapListAdapter = new OfflineBaseMapListAdapter(navigator);

    viewModel.getOfflineAreas().observe(this, offlineBaseMapListAdapter::update);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineBaseMapsFragBinding binding =
        OfflineBaseMapsFragBinding.inflate(inflater, container, false);

    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);

    ((MainActivity) getActivity()).setActionBar(binding.offlineAreasToolbar, true);

    RecyclerView recyclerView = binding.offlineAreasList;
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(offlineBaseMapListAdapter);

    return binding.getRoot();
  }
}
