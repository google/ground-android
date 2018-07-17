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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;

public class RecordDetailsFragment extends AbstractFragment {
  private static final String TAG = RecordDetailsFragment.class.getSimpleName();

  @BindView(R.id.record_details_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.form_name)
  TextView formNameView;

  @BindView(R.id.record_details_progress_bar)
  ProgressBar progressBar;

  @BindView(R.id.record_details_layout)
  LinearLayout recordDetailsLayout;

  private RecordDetailsViewModel viewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = get(RecordDetailsViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.record_details_frag, container, false);
  }

  @Override
  public void onViewCreated(
    @NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(toolbar);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.record_details_menu, menu);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
    viewModel.getRecord().observe(this, this::onUpdate);
  }

  @Override
  public void onStart() {
    super.onStart();
    RecordDetailsFragmentArgs args = getRecordDetailFragmentArgs();
    viewModel.loadRecordDetails(args.getProjectId(), args.getPlaceId(), args.getRecordId());
  }

  private void onUpdate(Resource<Record> record) {
    switch (record.getStatus()) {
      case LOADING:
        showProgressBar();
        break;
      case LOADED:
        record.ifPresent(this::showRecord);
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
    toolbar.setTitle("");
    toolbar.setSubtitle("");
    formNameView.setText("");
    recordDetailsLayout.setVisibility(View.GONE);
    progressBar.setVisibility(View.VISIBLE);
  }

  // TODO: Move into separate ViewHolder class.
  private void showRecord(Record record) {
    progressBar.setVisibility(View.GONE);
    toolbar.setTitle(record.getPlace().getTitle());
    toolbar.setSubtitle(record.getPlace().getSubtitle());
    formNameView.setText(record.getForm().getTitle());
    recordDetailsLayout.removeAllViews();
    for (Form.Element element : record.getForm().getElements()) {
      switch (element.getType()) {
        case FIELD:
          addField(element.getField(), record);
          break;
        case SUBFORM:
          Log.d(TAG, "Subforms not yet supported");
          break;
        default:
      }
    }
  }

  private void addField(Form.Field field, Record record) {
    FieldViewHolder fieldViewHolder = FieldViewHolder.newInstance(getLayoutInflater());
    fieldViewHolder.setLabel(field.getLabel());
    record
        .getValue(field.getId())
        .map(v -> v.getDetailsText(field))
        .ifPresent(fieldViewHolder::setValue);
    recordDetailsLayout.addView(fieldViewHolder.getRoot());
  }

  // TODO: Extract into outer class.
  static class FieldViewHolder {
    private ViewGroup root;

    @BindView(R.id.field_label)
    TextView labelView;

    @BindView(R.id.field_value)
    TextView valueView;

    FieldViewHolder(ViewGroup root) {
      this.root = root;
    }

    static FieldViewHolder newInstance(LayoutInflater inflater) {
      ViewGroup root = (ViewGroup) inflater.inflate(R.layout.record_details_field, null);
      FieldViewHolder holder = new FieldViewHolder(root);
      ButterKnife.bind(holder, root);
      return holder;
    }

    void setLabel(String label) {
      labelView.setText(label);
    }

    void setValue(String value) {
      valueView.setText(value);
    }

    public ViewGroup getRoot() {
      return root;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.edit_record_menu_item:
        // This is required to prevent menu from reappearing on back.
        getActivity().closeOptionsMenu();
        RecordDetailsFragmentArgs args = getRecordDetailFragmentArgs();
        NavHostFragment.findNavController(this)
                       .navigate(
                         RecordDetailsFragmentDirections.editRecord(
                           args.getProjectId(), args.getPlaceId(), args.getRecordId()));
        return true;
      case R.id.delete_record_menu_item:
        // TODO: Implement delete record.
        return true;
      default:
        return false;
    }
  }

  private RecordDetailsFragmentArgs getRecordDetailFragmentArgs() {
    return RecordDetailsFragmentArgs.fromBundle(getArguments());
  }
}
