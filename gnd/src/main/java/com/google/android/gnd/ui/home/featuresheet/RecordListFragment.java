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

package com.google.android.gnd.ui.home.featuresheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.RecordListFragBinding;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import java8.util.Optional;
import javax.inject.Inject;

public class RecordListFragment extends AbstractFragment {

  @Inject Navigator navigator;
  private RecordListAdapter recordListAdapter;
  private RecordListViewModel viewModel;
  private FeatureSheetViewModel featureSheetViewModel;
  private HomeScreenViewModel homeScreenViewModel;

  @BindView(R.id.record_list_container)
  RecyclerView recyclerView;

  static RecordListFragment newInstance() {
    return new RecordListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    recordListAdapter = new RecordListAdapter();
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(RecordListViewModel.class);
    featureSheetViewModel = getViewModel(FeatureSheetViewModel.class);
    homeScreenViewModel = getViewModel(HomeScreenViewModel.class);

    recordListAdapter.getItemClicks().observe(this, this::onItemClick);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    RecordListFragBinding binding = RecordListFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(recordListAdapter);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    viewModel.getRecordSummaries().observe(this, recordListAdapter::update);
    featureSheetViewModel.getSelectedForm().observe(this, this::onFormChange);
  }

  private void onItemClick(Record record) {
    navigator.showRecordDetails(
        record.getProject().getId(), record.getFeature().getId(), record.getId());
  }

  private void onFormChange(Optional<Form> form) {
    recordListAdapter.clear();
    // TODO: Use fragment args, load form and feature if not present.
    Optional<Feature> feature = featureSheetViewModel.getSelectedFeature().getValue();
    if (!form.isPresent() || !feature.isPresent()) {
      // TODO: Report error.
      return;
    }
    homeScreenViewModel.onFormChange(form.get());
    viewModel.loadRecordSummaries(feature.get(), form.get());
  }
}
