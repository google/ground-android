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

package com.google.android.gnd.ui.editrecord;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.util.Streams.toImmutableList;
import static com.google.android.gnd.vo.PlaceUpdate.Operation;
import static java8.util.stream.StreamSupport.stream;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.common.ViewModelFactory;
import com.google.android.gnd.ui.editrecord.input.Editable;
import com.google.android.gnd.ui.editrecord.input.MultipleChoiceFieldView;
import com.google.android.gnd.ui.editrecord.input.TextFieldView;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Record;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.inject.Inject;

public class EditRecordFragment extends AbstractFragment {
  private static final String TAG = EditRecordFragment.class.getSimpleName();
  private ProgressDialog savingProgressDialog;

  // TODO: Refactor viewModel creation and access into AbstractFragment.
  @Inject
  ViewModelFactory viewModelFactory;
  private EditRecordViewModel viewModel;

  @BindView(R.id.edit_record_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.form_name)
  TextView formNameView;

  @BindView(R.id.edit_record_progress_bar)
  ProgressBar progressBar;

  @BindView(R.id.edit_record_layout)
  LinearLayout formLayout;

  @BindView(R.id.save_record_btn)
  View saveButton;

  private List<Editable> fields;

  @Override
  protected View createView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    return inflater.inflate(R.layout.edit_record_frag, container, false);
  }

  @Override
  protected void obtainViewModels() {
    viewModel = viewModelFactory.create(EditRecordViewModel.class);
  }

  @Override
  protected void setUpView() {
    ((MainActivity) getActivity()).setActionBar(toolbar, R.drawable.ic_close);
    savingProgressDialog = ProgressDialogs.modalSpinner(getContext(), R.string.saving);
  }

  @Override
  protected void observeViewModels() {
    EditRecordFragmentArgs args = EditRecordFragmentArgs.fromBundle(getArguments());
    if (args.getProjectId() == null || args.getPlaceId() == null || args.getRecordId() == null) {
      Log.e(TAG, "Missing fragment args");
      EphemeralPopups.showError(getContext());
      return;
    }
    // TODO: Store and retrieve latest edits from cache and/or db.
    viewModel
      .getRecordSnapshot(args.getProjectId(), args.getPlaceId(), args.getRecordId())
      .as(autoDisposable(this))
      .subscribe(this::onSnapshotLoaded);
  }

  @Override
  protected void observeViews() {
    saveButton.setOnClickListener(__ -> onSaveClick());
  }

  private void onSnapshotLoaded(Resource<Record> record) {
    switch (record.getStatus()) {
      case LOADED:
        record.ifPresent(this::editRecord);
        break;
      case NOT_FOUND:
      case ERROR:
        // TODO: Replace w/error view?
        Log.e(TAG, "Failed to load record");
        EphemeralPopups.showError(getContext());
        break;
    }
  }

  private void editRecord(Record record) {
    progressBar.setVisibility(View.GONE);
    toolbar.setTitle(record.getPlace().getTitle());
    toolbar.setSubtitle(record.getPlace().getSubtitle());
    formNameView.setText(record.getForm().getTitle());
    formLayout.removeAllViews();
    fields = new ArrayList<>();
    for (Form.Element element : record.getForm().getElements()) {
      switch (element.getType()) {
        case FIELD:
          Editable editable = addField(element.getField(), record);
          if (editable != null) {
            fields.add(editable);
          }
          break;
        default:
          Log.d(TAG, element.getType() + " elements not yet supported");
      }
    }
    if (fields.isEmpty()) {
      // TODO: Show "empty form" error message.
    }
  }

  @Nullable
  private Editable addField(Form.Field field, Record record) {
    switch (field.getType()) {
      case TEXT:
        // TODO: Refactor these views into ViewHolders and use normal instances of Android view
        // components instead of extending them.
        TextFieldView textFieldView =
          (TextFieldView) getLayoutInflater().inflate(R.layout.text_field, formLayout, false);
        textFieldView.init(field, record);
        formLayout.addView(textFieldView);
        return textFieldView;
      case MULTIPLE_CHOICE:
        MultipleChoiceFieldView multipleChoiceFieldView =
          (MultipleChoiceFieldView)
            getLayoutInflater().inflate(R.layout.multiple_choice_field, formLayout, false);
        multipleChoiceFieldView.init(field, record);
        formLayout.addView(multipleChoiceFieldView);
        return multipleChoiceFieldView;
      default:
        return null;
    }
  }

  private void onSaveClick() {
    savingProgressDialog.show();
    viewModel
      .saveChanges(
        stream(fields)
          .map(Editable::getUpdate)
          .filter(u -> !u.getOperation().equals(Operation.NO_CHANGE))
          .collect(toImmutableList()))
      .as(autoDisposable(this))
      .subscribe(
        () -> {
          savingProgressDialog.hide();
          EphemeralPopups.showSuccess(getContext(), R.string.saved);
          NavHostFragment.findNavController(this).navigateUp();
          // TODO: Hide saving spinner, return to view mode.
        },
        t -> EphemeralPopups.showError(getContext()));
  }
}
