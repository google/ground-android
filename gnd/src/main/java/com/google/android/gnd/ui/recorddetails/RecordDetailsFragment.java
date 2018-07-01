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

package com.google.android.gnd.ui.recorddetails;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import javax.inject.Inject;

public class RecordDetailsFragment extends AbstractFragment {

  @Inject
  ViewModelFactory viewModelFactory;

  @BindView(R.id.record_details_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.record_details_layout)
  LinearLayout recordDetailsLayout;

  private RecordDetailsViewModel viewModel;

  public RecordDetailsFragment() {}

  @Override
  protected View createView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.record_details_frag, container, false);
  }

  @Override
  protected void obtainViewModels() {
    viewModel = viewModelFactory.create(RecordDetailsViewModel.class);
  }

  @Override
  protected void setUpView() {
    ((MainActivity) getActivity()).setActionBar(toolbar);
  }
}
