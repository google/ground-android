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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import javax.inject.Inject;

public class RecordDetailsFragment extends AbstractFragment {
  private static final String TAG = RecordDetailsFragment.class.getSimpleName();

  @Inject ViewModelFactory viewModelFactory;

  @BindView(R.id.record_details_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.record_details_progress_bar)
  ProgressBar progressBar;

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

  @Override
  protected void observeViewModels() {
    RecordDetailsFragmentArgs args = RecordDetailsFragmentArgs.fromBundle(getArguments());
    if (args.getProjectId() == null || args.getPlaceId() == null || args.getRecordId() == null) {
      Log.e(TAG, "Missing fragment args");
      EphemeralPopups.showError(getContext());
      return;
    }
    viewModel
        .getRecordDetails(args.getProjectId(), args.getPlaceId(), args.getRecordId())
        .observe(this, this::onUpdate);
  }

  private void onUpdate(Resource<Record> record) {
    switch (record.getStatus()) {
      case LOADING:
        showProgressBar();
        break;
      case LOADED:
        record.ifPresent(this::update);
        break;
      case NOT_FOUND:
      case ERROR:
        // TODO: Replace w/error view?
        Log.e(TAG, "Failed to load record");
        EphemeralPopups.showError(getContext());
        break;
    }
  }

  private void showProgressBar() {
    progressBar.setVisibility(View.VISIBLE);
    recordDetailsLayout.setVisibility(View.GONE);
  }

  private void update(Record record) {
    progressBar.setVisibility(View.GONE);
    recordDetailsLayout.removeAllViews();
    for (Form.Element element : record.getForm().getElements()) {
      switch (element.getType()) {
        case FIELD:
          addField(element.getField());
          break;
        case SUBFORM:
          Log.d(TAG, "Subforms not yet supported");
          break;
        default:
      }
    }
    // TODO: Attach place to Record.
    // TODO: Attach form to Record.
  }

  private void addField(Form.Field field) {
    TextView text = new TextView(getContext());
    text.setText(field.getLabel());
    recordDetailsLayout.addView(text);
  }
}
