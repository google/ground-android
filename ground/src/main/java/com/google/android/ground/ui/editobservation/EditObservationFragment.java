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

package com.google.android.ground.ui.editobservation;

import static com.google.android.ground.ui.util.ViewUtil.assignGeneratedId;
import static com.google.android.ground.util.ImmutableListCollector.toImmutableList;
import static java8.util.stream.StreamSupport.stream;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import butterknife.BindView;
import com.google.android.ground.MainActivity;
import com.google.android.ground.R;
import com.google.android.ground.databinding.EditObservationFragBinding;
import com.google.android.ground.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.ground.databinding.TextInputFieldBinding;
import com.google.android.ground.inject.ActivityScoped;
import com.google.android.ground.model.form.Element;
import com.google.android.ground.model.form.Field;
import com.google.android.ground.model.form.Form;
import com.google.android.ground.model.form.MultipleChoice.Cardinality;
import com.google.android.ground.model.observation.Response;
import com.google.android.ground.ui.common.AbstractFragment;
import com.google.android.ground.ui.common.BackPressListener;
import com.google.android.ground.ui.common.EphemeralPopups;
import com.google.android.ground.ui.common.Navigator;
import com.google.android.ground.ui.common.TwoLineToolbar;
import java8.util.Optional;
import javax.inject.Inject;

@ActivityScoped
public class EditObservationFragment extends AbstractFragment implements BackPressListener {
  private static final String TAG = EditObservationFragment.class.getSimpleName();

  private EditObservationViewModel viewModel;
  private SingleSelectDialogFactory singleSelectDialogFactory;
  private MultiSelectDialogFactory multiSelectDialogFactory;

  @Inject Navigator navigator;

  @BindView(R.id.edit_observation_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.edit_observation_layout)
  LinearLayout formLayout;

  @Override
  public void onCreate(@androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    singleSelectDialogFactory = new SingleSelectDialogFactory(getContext());
    multiSelectDialogFactory = new MultiSelectDialogFactory(getContext());
    viewModel = getViewModel(EditObservationViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    EditObservationFragBinding binding =
        EditObservationFragBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(this);
    binding.setViewModel(viewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(
      @NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(toolbar, R.drawable.ic_close_black_24dp);
    toolbar.setNavigationOnClickListener(__ -> onCloseButtonClick());
    // Observe state changes.
    viewModel.getForm().observe(this, this::rebuildForm);
    viewModel.getToolbarTitle().observe(this, toolbar::setTitle);
    viewModel.getSaveResults().observe(this, e -> e.ifUnhandled(this::handleSaveResult));
    // Initialize view model.
    viewModel.initialize(EditObservationFragmentArgs.fromBundle(getArguments()));
  }

  private void handleSaveResult(EditObservationViewModel.SaveResult saveResult) {
    switch (saveResult) {
      case HAS_VALIDATION_ERRORS:
        showValidationErrorsAlert();
        break;
      case NO_CHANGES_TO_SAVE:
        EphemeralPopups.showFyi(getContext(), R.string.no_changes_to_save);
        navigator.navigateUp();
        break;
      case SAVED:
        EphemeralPopups.showSuccess(getContext(), R.string.saved);
        navigator.navigateUp();
        break;
      default:
        Log.e(TAG, "Unknown save result type: " + saveResult);
        break;
    }
  }

  private void rebuildForm(Form form) {
    formLayout.removeAllViews();
    stream(form.getFieldsSorted()).forEach(this::addField);
  }

  private void addField(Field field) {
    switch (field.getType()) {
      case TEXT:
        addTextField(field);
        break;
      case MULTIPLE_CHOICE:
        addMultipleChoiceField(field);
        break;
      case PHOTO:
        addPhotoField(field);
        break;
      default:
        Log.w(TAG, "Unimplemented field type: " + field.getType());
        break;
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

  public void addPhotoField(Field field) {
    PhotoInputFieldBinding binding =
        PhotoInputFieldBinding.inflate(getLayoutInflater(), formLayout, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    binding.setField(field);
    formLayout.addView(binding.getRoot());
    assignGeneratedId(binding.getRoot().findViewById(R.id.image_thumbnail_preview));
    assignGeneratedId(binding.getRoot().findViewById(R.id.btn_select_photo));
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

  @Override
  public boolean onBack() {
    if (viewModel.hasUnsavedChanges()) {
      showUnsavedChangesDialog();
      return true;
    }
    return false;
  }

  private void onCloseButtonClick() {
    if (viewModel.hasUnsavedChanges()) {
      showUnsavedChangesDialog();
    } else {
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

  private void showValidationErrorsAlert() {
    new AlertDialog.Builder(getContext())
        .setMessage(R.string.invalid_data_warning)
        .setPositiveButton(R.string.invalid_data_confirm, (a, b) -> {})
        .create()
        .show();
  }
}
