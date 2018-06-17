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
import javax.inject.Inject;

public class RecordListFragment extends GndFragment {
  private static final String PLACE_TYPE_ID = "placeTypeId";
  private static final String PLACE_ID = "placeId";
  private static final String FORM_ID = "formId";

  @Inject GndViewModelFactory viewModelFactory;

  private RecordListViewModel viewModel;
  private RecordListRecyclerViewAdapter adapter;

  static RecordListFragment newInstance(String placeTypeId, String placeId, String formId) {
    RecordListFragment fragment = new RecordListFragment();
    Bundle args = new Bundle();
    args.putString(PLACE_TYPE_ID, placeTypeId);
    args.putString(PLACE_ID, placeId);
    args.putString(FORM_ID, formId);
    fragment.setArguments(args);
    return fragment;
  }

  private String getPlaceTypeId() {
    return getArguments().getString(PLACE_TYPE_ID);
  }

  private String getPlaceId() {
    return getArguments().getString(PLACE_ID);
  }

  private String getFormId() {
    return getArguments().getString(FORM_ID);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    adapter = new RecordListRecyclerViewAdapter();
  }

  @Override
  protected void createViewModel() {
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(RecordListViewModel.class);
  }

  @Nullable
  @Override
  public View createView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    RecyclerView recyclerView = new RecyclerView(getContext());
    recyclerView.setNestedScrollingEnabled(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(adapter);
    viewModel
      .loadRecords(getPlaceTypeId(), getFormId(), getPlaceId())
      .as(autoDisposable(this))
      .subscribe();
    return recyclerView;
  }

  @Override
  protected void observeViewModel() {
    viewModel.getRecords().observe(this, adapter::update);
  }
}
