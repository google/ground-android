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

package com.google.android.gnd.ui.syncstatus;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.databinding.SyncStatusFragBinding;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.common.Navigator;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

/** Fragment containing a list of mutations and their respective upload statuses. */
@AndroidEntryPoint
public class SyncStatusFragment extends AbstractFragment {
  @Inject Navigator navigator;
  @Inject FeatureHelper featureHelper;

  private SyncStatusViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(SyncStatusViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    SyncStatusFragBinding binding = SyncStatusFragBinding.inflate(inflater, container, false);

    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);

    ((MainActivity) getActivity()).setActionBar(binding.syncStatusToolbar, true);

    SyncStatusListAdapter syncStatusListAdapter =
        new SyncStatusListAdapter(getContext().getApplicationContext(), featureHelper);
    RecyclerView recyclerView = binding.syncStatusList;
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(syncStatusListAdapter);

    viewModel.getMutations().observe(getViewLifecycleOwner(), syncStatusListAdapter::update);

    return binding.getRoot();
  }
}
