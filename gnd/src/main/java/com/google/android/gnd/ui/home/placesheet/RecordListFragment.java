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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.fragment.NavHostFragment;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.home.HomeScreenFragmentDirections;
import com.google.android.gnd.ui.home.HomeScreenViewModel;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Record;
import java8.util.Optional;
import javax.inject.Inject;

public class RecordListFragment extends AbstractFragment {
  @Inject
  ViewModelFactory viewModelFactory;

  private RecordListAdapter recordListAdapter;

  private RecordListViewModel viewModel;
  private PlaceSheetBodyViewModel placeSheetViewModel;
  private HomeScreenViewModel homeScreenViewModel;

  static RecordListFragment newInstance() {
    return new RecordListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    recordListAdapter = new RecordListAdapter();
  }

  @Override
  protected void obtainViewModels() {
    // TODO: Roll "get()" calls into ViewModelFactory to enforce scoping.
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(RecordListViewModel.class);
    placeSheetViewModel =
        ViewModelProviders.of(getActivity(), viewModelFactory).get(PlaceSheetBodyViewModel.class);
    homeScreenViewModel =
        ViewModelProviders.of(getActivity(), viewModelFactory).get(HomeScreenViewModel.class);
  }

  @Nullable
  @Override
  public View createView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    RecyclerView recyclerView = new RecyclerView(getContext());
    recyclerView.setNestedScrollingEnabled(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(recordListAdapter);
    return recyclerView;
  }

  @Override
  protected void observeViewModels() {
    viewModel.getRecordSummaries().observe(this, recordListAdapter::update);
    placeSheetViewModel.getSelectedForm().observe(this, this::onFormChange);
    recordListAdapter.getItemClicks().as(autoDisposable(this)).subscribe(this::showRecordDetails);
  }

  private void showRecordDetails(Record record) {
    NavHostFragment.findNavController(this)
                   .navigate(
                     HomeScreenFragmentDirections.showRecordDetails(
                       record.getProject().getId(),
                       record.getPlace().getId(),
                       record.getId()));
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
