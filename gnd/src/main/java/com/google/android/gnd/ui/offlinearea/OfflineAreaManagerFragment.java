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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gnd.MainActivity;
import com.google.android.gnd.databinding.OfflineAreaManagerFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.TwoLineToolbar;

import butterknife.BindView;

/**
 * Fragment containing a list of downloaded areas on the device. An area is a set of offline imagery.
 * Users can manage their areas within this fragment. They can delete areas they no longer need
 * or initiate a flow to select and download a new area to the device.
 */
@ActivityScoped
public class OfflineAreaManagerFragment extends AbstractFragment {

  @BindView(R.id.offline_maps_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.offline_maps_list)
  RecyclerView areaList;

  private OfflineAreaManagerViewModel viewModel;

  public static OfflineAreaManagerFragment newInstance() {
    return new OfflineAreaManagerFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(OfflineAreaManagerViewModel.class);
    // TODO: use the viewmodel
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    OfflineAreaManagerFragBinding binding =
        OfflineAreaManagerFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    toolbar.setTitle(R.string.offline_maps);
    ((MainActivity) getActivity()).setActionBar(toolbar);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
  }
}
