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

import static com.google.android.gnd.ui.util.ViewUtil.assignGeneratedId;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LiveData;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.EditRecordFragBinding;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.observation.Record;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.repository.Persistable;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import java8.util.Optional;
import javax.inject.Inject;

@ActivityScoped
public class EditRecordFragment extends AbstractFragment implements BackPressListener {
  private static final String TAG = EditRecordFragment.class.getSimpleName();

  private ProgressDialog savingProgressDialog;

  private EditRecordViewModel viewModel;
  private SingleSelectDialogFactory singleSelectDialogFactory;
  private MultiSelectDialogFactory multiSelectDialogFactory;
  private static final String NEW_RECORD_ID_ARG_PLACEHOLDER = "NEW_RECORD";

  @Inject Navigator navigator;

  @BindView(R.id.edit_record_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.form_name)
  TextView formNameView;

  @BindView(R.id.edit_record_layout)
  LinearLayout formLayout;

  @BindView(R.id.save_record_btn)
  View saveRecordButton;

  @Override
  public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    singleSelectDialogFactory = new SingleSelectDialogFactory(getContext());
    multiSelectDialogFactory = new MultiSelectDialogFactory(getContext());
    viewModel = getViewModel(EditRecordViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    EditRecordFragBinding binding = EditRecordFragBinding.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(
      @NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(toolbar, R.drawable.ic_close_black_24dp);
    toolbar.setNavigationOnClickListener(__ -> onCloseButtonClick());
    savingProgressDialog = ProgressDialogs.modalSpinner(getContext(), R.string.saving);
  }

  @Override
  public void onActivityCreated(@androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    viewModel.getRecord().observe(this, this::onRecordChange);
    viewModel.getShowUnsavedChangesDialogEvents().observe(this, __ -> showUnsavedChangesDialog());
    viewModel.getShowErrorDialogEvents().observe(this, __ -> showFormErrorsDialog());
  }

  @Override
  public void onStart() {
    super.onStart();
    // TODO: Make reactive instead of reading getValue explicitly.
    LiveData<Persistable<Record>> liveData = viewModel.getRecord();
    Persistable<Record> record = liveData.getValue();
    if (record != null && record.isLoaded()) {
      onRecordChange(record);
      return;
    }
    EditRecordFragmentArgs args = EditRecordFragmentArgs.fromBundle(getArguments());
    viewModel.editRecord(args, args.getRecordId().equals(NEW_RECORD_ID_ARG_PLACEHOLDER));
  }

  private void onRecordChange(Persistable<Record> record) {
    switch (record.state()) {
      case LOADING:
        saveRecordButton.setVisibility(View.GONE);
        break;
      case LOADED:
        record.value().ifPresent(this::editRecord);
        break;
      case SAVING:
        savingProgressDialog.show();
        break;
      case SAVED:
        savingProgressDialog.hide();
        EphemeralPopups.showSuccess(getContext(), R.string.saved);
        navigator.navigateUp();
        break;
      case NOT_FOUND:
      case ERROR:
        record.error().ifPresent(t -> Log.e(TAG, "Failed to load/save record", t));
        EphemeralPopups.showError(getContext());
        navigator.navigateUp();
        break;
    }
  }

  private void editRecord(Record record) {
    toolbar.setTitle(record.getFeature().getTitle());
    toolbar.setSubtitle(record.getFeature().getSubtitle());
    formNameView.setText(record.getForm().getTitle());
    rebuildForm(record);
    saveRecordButton.setVisibility(View.VISIBLE);
  }

  private void rebuildForm(Record record) {
    formLayout.removeAllViews();
    for (Element element : record.getForm().getElements()) {
      switch (element.getType()) {
        case FIELD:
          addField(element.getField());
          break;
        default:
          Log.d(TAG, element.getType() + " elements not yet supported");
      }
    }
  }

  private void addField(Field field) {
    switch (field.getType()) {
      case TEXT:
        addTextField(field);
        break;
      case MULTIPLE_CHOICE:
        addMultipleChoiceField(field);
        break;
      default:
        Log.w(TAG, "Unimplemented field type: " + field.getType());
    }
  }

  private void addTextField(Field field) {
    TextInputFieldBinding binding =
        TextInputFieldBinding.inflate(getLayoutInflater(), formLayout, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.setField(field);
    formLayout.addView(binding.getRoot());
    assignGeneratedId(binding.getRoot().findViewById(R.id.text_input_edit_text));
  }

  public void addMultipleChoiceField(Field field) {
    MultipleChoiceInputFieldBinding binding =
        MultipleChoiceInputFieldBinding.inflate(getLayoutInflater(), formLayout, false);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.setField(field);
    formLayout.addView(binding.getRoot());
    assignGeneratedId(binding.getRoot().findViewById(R.id.multiple_choice_input_edit_text));
  }

  public void onShowDialog(Field field) {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    Optional<Response> currentResponse = viewModel.getResponse(field.getId());
    switch (cardinality) {
      case SELECT_MULTIPLE:
        multiSelectDialogFactory
            .create(field, currentResponse, r -> viewModel.onResponseChanged(field, r))
            .show();
        break;
      case SELECT_ONE:
        singleSelectDialogFactory
            .create(field, currentResponse, r -> viewModel.onResponseChanged(field, r))
            .show();
        break;
      default:
        Log.e(TAG, "Unknown cardinality: " + cardinality);
        return;
    }
  }

  @OnClick(R.id.save_record_btn)
  void onSaveClick() {
    if (!viewModel.onSaveClick()) {
      EphemeralPopups.showFyi(getContext(), R.string.no_changes_to_save);
      navigator.navigateUp();
    }
  }

  @Override
  public boolean onBack() {
    return viewModel.onBack();
  }

  private void onCloseButtonClick() {
    if (!viewModel.onBack()) {
      navigator.navigateUp();
    }
  }

  private void showUnsavedChangesDialog() {
    new AlertDialog.Builder(getContext())
        .setMessage(R.string.unsaved_changes)
        .setPositiveButton(R.string.close_without_saving, (d, i) -> navigator.navigateUp())
        .setNegativeButton(R.string.continue_editing, (d, i) -> {})
        .create()
        .show();
  }

  private void showFormErrorsDialog() {
    new AlertDialog.Builder(getContext())
        .setMessage(R.string.invalid_data_warning)
        .setPositiveButton(R.string.invalid_data_confirm, (a, b) -> {})
        .create()
        .show();
  }
}
