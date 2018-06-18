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

package com.google.android.gnd.ui.browse.placesheet;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gnd.ui.common.GndFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import java8.util.Optional;
import javax.inject.Inject;

public class RecordListFragment extends GndFragment {
  @Inject GndViewModelFactory viewModelFactory;

  private RecordListAdapter recordListAdapter;

  private RecordListViewModel viewModel;
  private PlaceSheetBodyViewModel placeSheetViewModel;

  static RecordListFragment newInstance() {
    return new RecordListFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    recordListAdapter = new RecordListAdapter();
  }

  @Override
  protected void createViewModel() {
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(RecordListViewModel.class);
    placeSheetViewModel =
      ViewModelProviders.of(getActivity(), viewModelFactory).get(PlaceSheetBodyViewModel.class);
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
  protected void observeViewModel() {
    viewModel.getRecords().observe(this, recordListAdapter::update);
    placeSheetViewModel.getSelectedForm().observe(this, this::update);
  }

  private void update(Optional<Form> form) {
    viewModel.clearRecords();
    Optional<Place> place = placeSheetViewModel.getSelectedPlace().getValue();
    if (!form.isPresent() || !place.isPresent()) {
      return;
    }
    viewModel.loadRecords(place.get(), form.get()).as(autoDisposable(this)).subscribe();
  }
}
