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

package com.google.android.gnd.ui.home.featuredetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.databinding.ObservationListFragBinding;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.observation.Observation;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import javax.inject.Inject;

@AndroidEntryPoint
public class ObservationListFragment extends AbstractFragment {

  @Inject Navigator navigator;

  private ObservationListAdapter observationListAdapter;
  private ObservationListViewModel viewModel;
  private FeatureDetailsViewModel featureDetailsViewModel;
  private ObservationListFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    observationListAdapter = new ObservationListAdapter(viewModelFactory);
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(ObservationListViewModel.class);
    featureDetailsViewModel = getViewModel(FeatureDetailsViewModel.class);

    observationListAdapter.getItemClicks().observe(this, this::onItemClick);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    binding = ObservationListFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    RecyclerView observationList = binding.observationListContainer;
    observationList.setLayoutManager(new LinearLayoutManager(getContext()));
    observationList.setAdapter(observationListAdapter);
    featureDetailsViewModel
        .getSelectedFeatureOnceAndStream()
        .observe(getViewLifecycleOwner(), this::onFeatureSelected);
  }

  private void onFeatureSelected(Optional<Feature> feature) {
    observationListAdapter.clear();
    feature.ifPresent(viewModel::loadObservationList);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    viewModel.getObservations().observe(getViewLifecycleOwner(), observationListAdapter::update);
  }

  private void onItemClick(Observation observation) {
    navigator.navigate(
        HomeScreenFragmentDirections.showObservationDetails(
            observation.getProject().getId(),
            observation.getFeature().getId(),
            observation.getId()));
  }
}
