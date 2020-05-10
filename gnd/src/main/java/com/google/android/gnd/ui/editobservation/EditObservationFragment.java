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

package com.google.android.gnd.ui.editobservation;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.EditObservationFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Element.Type;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.model.observation.ResponseMap;
import com.google.android.gnd.system.ActivityStreams;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.field.FieldFactory;
import com.google.android.gnd.ui.field.FieldView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@ActivityScoped
public class EditObservationFragment extends AbstractFragment implements BackPressListener {

  @Inject Navigator navigator;
  @Inject ActivityStreams activityStreams;

  @BindView(R.id.edit_observation_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.edit_observation_layout)
  LinearLayout formLayout;

  private EditObservationViewModel viewModel;
  private FieldFactory fieldFactory;
  private List<FieldView> fieldViews = new ArrayList<>();
  private EditObservationFragmentArgs args;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    args = EditObservationFragmentArgs.fromBundle(getArguments());
    viewModel = getViewModel(EditObservationViewModel.class);
    fieldFactory = new FieldFactory(this, viewModelFactory, args);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    EditObservationFragBinding binding =
        EditObservationFragBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(this);
    binding.setFragment(this);
    binding.setViewModel(viewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(toolbar, R.drawable.ic_close_black_24dp);
    toolbar.setNavigationOnClickListener(__ -> onCloseButtonClick());

    // Observe state changes.
    viewModel.getForm().observe(getViewLifecycleOwner(), this::rebuildForm);
    viewModel
        .getSaveResults()
        .observe(getViewLifecycleOwner(), e -> e.ifUnhandled(this::handleSaveResult));

    // Initialize view model.
    viewModel.initialize(args);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }

  private Map<Field, Optional<Response>> getResponses() {
    HashMap<Field, Optional<Response>> map = new HashMap<>();
    for (FieldView fieldView : fieldViews) {
      map.put(fieldView.getField(), fieldView.getResponse());
    }
    return map;
  }

  public void onSaveClick() {
    viewModel.onSaveResponses(getResponses());
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
        Timber.e("Unknown save result type: %s", saveResult);
        break;
    }
  }

  private void rebuildForm(Form form) {
    formLayout.removeAllViews();
    fieldViews.clear();

    ResponseMap initialResponses = viewModel.getResponses();

    for (Element element : form.getElements()) {
      if (element.getType() == Type.FIELD && element.getField() != null) {
        Field field = element.getField();
        Optional<Response> response = initialResponses.getResponse(field.getId());
        FieldView fieldView = fieldFactory.createFieldView(field, response);
        fieldViews.add(fieldView);
        formLayout.addView(fieldView);
      } else {
        Timber.e("%s elements not yet supported", element.getType());
      }
    }
  }

  @Override
  public void onPause() {
    ResponseMap.Builder builder = ResponseMap.builder();
    for (FieldView fieldView : fieldViews) {
      fieldView.onPause();
      fieldView
          .getResponse()
          .ifPresent(response -> builder.putResponse(fieldView.getField().getId(), response));
    }
    viewModel.setLastSavedResponses(builder.build());
    super.onPause();
  }

  @Override
  public boolean onBack() {
    if (viewModel.hasUnsavedChanges(getResponses())) {
      showUnsavedChangesDialog();
      return true;
    }
    return false;
  }

  private void onCloseButtonClick() {
    if (viewModel.hasUnsavedChanges(getResponses())) {
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
