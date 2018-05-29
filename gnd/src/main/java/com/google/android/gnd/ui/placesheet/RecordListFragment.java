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

package com.google.android.gnd.ui.placesheet;

import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RecordListFragment extends Fragment {
  private static final String FORM_NO = "formNo";

  static RecordListFragment newInstance(int formId) {
    RecordListFragment fragment = new RecordListFragment();
    Bundle args = new Bundle();
    args.putInt(FORM_NO, formId);
    fragment.setArguments(args);
    return fragment;
  }

  //  private DataSheetPresenter dataSheetPresenter;
  //  private Form form;
  int getFormId() {
    return getArguments().getInt(FORM_NO);
  }

  @RequiresApi(api = VERSION_CODES.KITKAT_WATCH)
  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
    RecyclerView recyclerView = new RecyclerView(getContext());
    recyclerView.setNestedScrollingEnabled(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(new RecordListRecyclerViewAdapter());
    return recyclerView;
  }
}
