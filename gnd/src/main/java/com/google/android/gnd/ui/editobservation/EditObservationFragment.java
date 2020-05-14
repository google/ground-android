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
import com.google.android.gnd.databinding.EditObservationBottomSheetBinding;
import com.google.android.gnd.databinding.EditObservationFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice.Cardinality;
import com.google.android.gnd.model.observation.Response;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java8.util.Optional;
import java8.util.function.Consumer;
import javax.inject.Inject;
import timber.log.Timber;

@ActivityScoped
public class EditObservationFragment extends AbstractFragment implements BackPressListener {

  @Inject Navigator navigator;

  @BindView(R.id.edit_observation_toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.edit_observation_layout)
  LinearLayout formLayout;

  private FieldViewBindingFactory factory;
  private EditObservationViewModel viewModel;
  private SingleSelectDialogFactory singleSelectDialogFactory;
  private MultiSelectDialogFactory multiSelectDialogFactory;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    singleSelectDialogFactory = new SingleSelectDialogFactory(getContext());
    multiSelectDialogFactory = new MultiSelectDialogFactory(getContext());
    viewModel = getViewModel(EditObservationViewModel.class);
    factory = new FieldViewBindingFactory(this, viewModelFactory);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    EditObservationFragBinding binding =
        EditObservationFragBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(this);
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
        Timber.e("Unknown save result type: %s", saveResult);
        break;
    }
  }

  private void rebuildForm(Form form) {
    formLayout.removeAllViews();
    for (Element element : form.getElements()) {
      switch (element.getType()) {
        case FIELD:
          Field field = element.getField();
          final AbstractFieldViewModel fieldViewModel = factory.create(field.getType(), formLayout);
          fieldViewModel.setField(field);
          fieldViewModel.setResponse(viewModel.getResponse(field.getId()));

          if (fieldViewModel instanceof PhotoFieldViewModel) {
            ((PhotoFieldViewModel) fieldViewModel)
                .getShowDialogClicks()
                .observe(this, this::onShowPhotoSelectorDialog);
          } else if (fieldViewModel instanceof MultipleChoiceFieldViewModel) {
            final MultipleChoiceFieldViewModel viewModel =
                (MultipleChoiceFieldViewModel) fieldViewModel;
            viewModel
                .getShowDialogClicks()
                .observe(
                    this,
                    __ ->
                        onShowDialog(
                            viewModel.getField(),
                            viewModel.getResponse().getValue(),
                            viewModel::setResponse));
          }

          fieldViewModel
              .responseUpdates()
              .observe(
                  this,
                  responseOptional ->
                      viewModel.onResponseChanged(fieldViewModel.getField(), responseOptional));
          break;
        default:
          throw new IllegalArgumentException(element.getType() + " elements not yet supported");
      }
    }
  }

  private void onShowDialog(
      Field field, Optional<Response> currentResponse, Consumer<Optional<Response>> consumer) {
    Cardinality cardinality = field.getMultipleChoice().getCardinality();
    switch (cardinality) {
      case SELECT_MULTIPLE:
        multiSelectDialogFactory.create(field, currentResponse, consumer).show();
        break;
      case SELECT_ONE:
        singleSelectDialogFactory.create(field, currentResponse, consumer).show();
        break;
      default:
        Timber.e("Unknown cardinality: %s", cardinality);
        break;
    }
  }

  private void onShowPhotoSelectorDialog(Field field) {
    EditObservationBottomSheetBinding addPhotoBottomSheetBinding =
        EditObservationBottomSheetBinding.inflate(getLayoutInflater());
    addPhotoBottomSheetBinding.setViewModel(viewModel);
    addPhotoBottomSheetBinding.setField(field);

    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(getContext());
    bottomSheetDialog.setContentView(addPhotoBottomSheetBinding.getRoot());
    bottomSheetDialog.setCancelable(true);
    bottomSheetDialog.show();
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
