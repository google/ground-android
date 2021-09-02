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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.editobservation.AddPhotoDialogAdapter.PhotoStorageResource.PHOTO_SOURCE_CAMERA;
import static com.google.android.gnd.ui.editobservation.AddPhotoDialogAdapter.PhotoStorageResource.PHOTO_SOURCE_STORAGE;
import static java.util.Objects.requireNonNull;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.DateInputFieldBinding;
import com.google.android.gnd.databinding.EditObservationBottomSheetBinding;
import com.google.android.gnd.databinding.EditObservationFragBinding;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.NumberInputFieldBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.databinding.TimeInputFieldBinding;
import com.google.android.gnd.model.form.Element;
import com.google.android.gnd.model.form.Field;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.form.MultipleChoice;
import com.google.android.gnd.model.form.Option;
import com.google.android.gnd.model.observation.MultipleChoiceResponse;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java8.util.Optional;
import java8.util.function.Consumer;
import javax.inject.Inject;
import timber.log.Timber;

@AndroidEntryPoint
public class EditObservationFragment extends AbstractFragment implements BackPressListener {

  private final List<AbstractFieldViewModel> fieldViewModelList = new ArrayList<>();

  @Inject Navigator navigator;
  @Inject FieldViewFactory fieldViewFactory;
  @Inject EphemeralPopups popups;

  private EditObservationViewModel viewModel;
  private EditObservationFragBinding binding;

  private static AbstractFieldViewModel getViewModel(ViewDataBinding binding) {
    if (binding instanceof TextInputFieldBinding) {
      return ((TextInputFieldBinding) binding).getViewModel();
    } else if (binding instanceof MultipleChoiceInputFieldBinding) {
      return ((MultipleChoiceInputFieldBinding) binding).getViewModel();
    } else if (binding instanceof NumberInputFieldBinding) {
      return ((NumberInputFieldBinding) binding).getViewModel();
    } else if (binding instanceof PhotoInputFieldBinding) {
      return ((PhotoInputFieldBinding) binding).getViewModel();
    } else if (binding instanceof DateInputFieldBinding) {
      return ((DateInputFieldBinding) binding).getViewModel();
    } else if (binding instanceof TimeInputFieldBinding) {
      return ((TimeInputFieldBinding) binding).getViewModel();
    } else {
      throw new IllegalArgumentException("Unknown binding type: " + binding.getClass());
    }
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(EditObservationViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = EditObservationFragBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(this);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.saveObservationBtn.setOnClickListener(this::onSaveClick);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TwoLineToolbar toolbar = binding.editObservationToolbar;
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
        popups.showFyi(R.string.no_changes_to_save);
        navigator.navigateUp();
        break;
      case SAVED:
        popups.showSuccess(R.string.saved);
        navigator.navigateUp();
        break;
      default:
        Timber.e("Unknown save result type: %s", saveResult);
        break;
    }
  }

  private void addFieldViewModel(Field field, AbstractFieldViewModel fieldViewModel) {
    fieldViewModel.init(field, viewModel.getSavedOrOriginalResponse(field.getId()));

    if (fieldViewModel instanceof PhotoFieldViewModel) {
      initPhotoField((PhotoFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof MultipleChoiceFieldViewModel) {
      observeSelectChoiceClicks((MultipleChoiceFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof DateFieldViewModel) {
      observeDateDialogClicks((DateFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof TimeFieldViewModel) {
      observeTimeDialogClicks((TimeFieldViewModel) fieldViewModel);
    }

    fieldViewModel
        .getResponse()
        .observe(this, response -> viewModel.onResponseChanged(field, response));

    fieldViewModelList.add(fieldViewModel);
  }

  public void onSaveClick(View view) {
    hideKeyboard(view);
    viewModel.onSave(getValidationErrors());
  }

  private void hideKeyboard(View view) {
    if (getActivity() != null) {
      InputMethodManager inputMethodManager =
          (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
      inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
  }

  private Map<String, String> getValidationErrors() {
    HashMap<String, String> errors = new HashMap<>();
    for (AbstractFieldViewModel fieldViewModel : fieldViewModelList) {
      fieldViewModel
          .validate()
          .ifPresent(error -> errors.put(fieldViewModel.getField().getId(), error));
    }
    return errors;
  }

  private void observeDateDialogClicks(DateFieldViewModel dateFieldViewModel) {
    dateFieldViewModel
        .getShowDialogClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> showDateDialog(dateFieldViewModel));
  }

  private void observeTimeDialogClicks(TimeFieldViewModel timeFieldViewModel) {
    timeFieldViewModel
        .getShowDialogClicks()
        .as(autoDisposable(this))
        .subscribe(__ -> showTimeDialog(timeFieldViewModel));
  }

  private void rebuildForm(Form form) {
    LinearLayout formLayout = binding.editObservationLayout;
    formLayout.removeAllViews();
    fieldViewModelList.clear();
    for (Element element : form.getElementsSorted()) {
      switch (element.getType()) {
        case FIELD:
          Field field = element.getField();
          ViewDataBinding binding = fieldViewFactory.addFieldView(field.getType(), formLayout);
          addFieldViewModel(field, getViewModel(binding));
          break;
        case UNKNOWN:
        default:
          Timber.e("%s form elements not yet supported", element.getType());
          break;
      }
    }
  }

  private void observeSelectChoiceClicks(MultipleChoiceFieldViewModel viewModel) {
    viewModel
        .getShowDialogClicks()
        .observe(
            this,
            __ ->
                createDialog(
                        viewModel.getField(),
                        viewModel.getCurrentResponse(),
                        viewModel::updateResponse)
                    .show());
  }

  private AlertDialog createDialog(
      Field field,
      Optional<MultipleChoiceResponse> response,
      Consumer<ImmutableList<Option>> consumer) {
    MultipleChoice multipleChoice = requireNonNull(field.getMultipleChoice());
    switch (multipleChoice.getCardinality()) {
      case SELECT_MULTIPLE:
        return MultiSelectDialogFactory.builder()
            .setContext(requireContext())
            .setTitle(field.getLabel())
            .setMultipleChoice(multipleChoice)
            .setCurrentResponse(response)
            .setValueConsumer(consumer)
            .build()
            .createDialog();
      case SELECT_ONE:
        return SingleSelectDialogFactory.builder()
            .setContext(requireContext())
            .setTitle(field.getLabel())
            .setMultipleChoice(multipleChoice)
            .setCurrentResponse(response)
            .setValueConsumer(consumer)
            .build()
            .createDialog();
      default:
        throw new IllegalArgumentException(
            "Unknown cardinality: " + multipleChoice.getCardinality());
    }
  }

  private void initPhotoField(PhotoFieldViewModel photoFieldViewModel) {
    photoFieldViewModel.setEditable(true);
    observeSelectPhotoClicks(photoFieldViewModel);
    observePhotoAdded(photoFieldViewModel);
  }

  private void observeSelectPhotoClicks(PhotoFieldViewModel fieldViewModel) {
    fieldViewModel
        .getShowDialogClicks()
        .observe(this, __ -> onShowPhotoSelectorDialog(fieldViewModel.getField()));
  }

  private void observePhotoAdded(PhotoFieldViewModel fieldViewModel) {
    viewModel
        .getPhotoFieldUpdates()
        .observe(
            this,
            map -> {
              // TODO: Do not set response if already handled.
              Field field = fieldViewModel.getField();
              if (map.containsKey(field)) {
                fieldViewModel.updateResponse(map.get(field));
              }
            });
  }

  private void onShowPhotoSelectorDialog(Field field) {
    EditObservationBottomSheetBinding addPhotoBottomSheetBinding =
        EditObservationBottomSheetBinding.inflate(getLayoutInflater());
    addPhotoBottomSheetBinding.setViewModel(viewModel);
    addPhotoBottomSheetBinding.setField(field);

    BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
    bottomSheetDialog.setContentView(addPhotoBottomSheetBinding.getRoot());
    bottomSheetDialog.setCancelable(true);
    bottomSheetDialog.show();

    RecyclerView recyclerView = addPhotoBottomSheetBinding.recyclerView;
    recyclerView.setHasFixedSize(true);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(
        new AddPhotoDialogAdapter(
            type -> {
              bottomSheetDialog.dismiss();
              onSelectPhotoClick(type, field);
            }));
  }

  private void showDateDialog(DateFieldViewModel fieldViewModel) {
    Calendar calendar = Calendar.getInstance();
    int year = calendar.get(Calendar.YEAR);
    int month = calendar.get(Calendar.MONTH);
    int day = calendar.get(Calendar.DAY_OF_MONTH);
    DatePickerDialog datePickerDialog =
        new DatePickerDialog(
            requireContext(),
            (view, updatedYear, updatedMonth, updatedDayOfMonth) -> {
              Calendar c = Calendar.getInstance();
              c.set(Calendar.DAY_OF_MONTH, updatedDayOfMonth);
              c.set(Calendar.MONTH, updatedMonth);
              c.set(Calendar.YEAR, updatedYear);
              fieldViewModel.updateResponse(c.getTime());
            },
            year,
            month,
            day);
    datePickerDialog.show();
  }

  private void showTimeDialog(TimeFieldViewModel fieldViewModel) {
    Calendar calendar = Calendar.getInstance();
    int hour = calendar.get(Calendar.HOUR);
    int minute = calendar.get(Calendar.MINUTE);
    TimePickerDialog timePickerDialog =
        new TimePickerDialog(
            requireContext(),
            (view, updatedHourOfDay, updatedMinute) -> {
              Calendar c = Calendar.getInstance();
              c.set(Calendar.HOUR_OF_DAY, updatedHourOfDay);
              c.set(Calendar.MINUTE, updatedMinute);
              fieldViewModel.updateResponse(c.getTime());
            },
            hour,
            minute,
            DateFormat.is24HourFormat(requireContext()));
    timePickerDialog.show();
  }

  private void onSelectPhotoClick(int type, Field field) {
    switch (type) {
      case PHOTO_SOURCE_CAMERA:
        viewModel.showPhotoCapture(field);
        break;
      case PHOTO_SOURCE_STORAGE:
        viewModel.showPhotoSelector(field);
        break;
      default:
        throw new IllegalArgumentException("Unknown type: " + type);
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
    new AlertDialog.Builder(requireContext())
        .setMessage(R.string.unsaved_changes)
        .setPositiveButton(R.string.close_without_saving, (d, i) -> navigator.navigateUp())
        .setNegativeButton(R.string.continue_editing, (d, i) -> {})
        .create()
        .show();
  }

  private void showValidationErrorsAlert() {
    new AlertDialog.Builder(requireContext())
        .setMessage(R.string.invalid_data_warning)
        .setPositiveButton(R.string.invalid_data_confirm, (a, b) -> {})
        .create()
        .show();
  }
}
