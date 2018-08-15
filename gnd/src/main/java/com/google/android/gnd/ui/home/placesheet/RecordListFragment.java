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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;

public class RecordListFragment extends AbstractFragment {

  private RecordListAdapter recordListAdapter;

  private RecordListViewModel viewModel;
  private PlaceSheetViewModel placeSheetViewModel;
  private HomeScreenViewModel homeScreenViewModel;

  static RecordListFragment newInstance() {
    return new RecordListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    recordListAdapter = new RecordListAdapter();
    super.onCreate(savedInstanceState);
    viewModel = get(RecordListViewModel.class);
    placeSheetViewModel = get(PlaceSheetViewModel.class);
    homeScreenViewModel = get(HomeScreenViewModel.class);

    recordListAdapter.getItemClicks().observe(this, this::showRecordDetails);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    RecyclerView recyclerView = new RecyclerView(getContext());
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(recordListAdapter);
    return recyclerView;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    viewModel.getRecordSummaries().observe(this, recordListAdapter::update);
    placeSheetViewModel.getSelectedForm().observe(this, this::onFormChange);
  }

  private void showRecordDetails(Record record) {
    NavHostFragment.findNavController(this)
        .navigate(
            HomeScreenFragmentDirections.showRecordDetails(
                record.getProject().getId(), record.getPlace().getId(), record.getId()));
  }

  private void onFormChange(Optional<Form> form) {
    viewModel.clearRecords();
    // TODO: Use fragment args, load form and place if not present.
    Optional<Place> place = placeSheetViewModel.getSelectedPlace().getValue();
    if (!form.isPresent() || !place.isPresent()) {
      // TODO: Report error.
      return;
    }
    homeScreenViewModel.onFormChange(form.get());
    viewModel.loadRecordSummaries(place.get(), form.get());
  }
}
