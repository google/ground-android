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

package com.google.android.gnd.ui.editsubmission;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.editsubmission.AddPhotoDialogAdapter.PhotoStorageResource.PHOTO_SOURCE_CAMERA;
import static com.google.android.gnd.ui.editsubmission.AddPhotoDialogAdapter.PhotoStorageResource.PHOTO_SOURCE_STORAGE;
import static java.util.Objects.requireNonNull;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.activity.result.contract.ActivityResultContracts.TakePicture;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gnd.BuildConfig;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.DateInputFieldBinding;
import com.google.android.gnd.databinding.EditSubmissionBottomSheetBinding;
import com.google.android.gnd.databinding.EditSubmissionFragBinding;
import com.google.android.gnd.databinding.MultipleChoiceInputFieldBinding;
import com.google.android.gnd.databinding.NumberInputFieldBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBinding;
import com.google.android.gnd.databinding.PhotoInputFieldBindingImpl;
import com.google.android.gnd.databinding.TextInputFieldBinding;
import com.google.android.gnd.databinding.TimeInputFieldBinding;
import com.google.android.gnd.model.submission.MultipleChoiceResponse;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.model.task.MultipleChoice;
import com.google.android.gnd.model.task.Option;
import com.google.android.gnd.model.task.Step;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.repository.UserMediaRepository;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Completable;
import java.io.File;
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
public class EditSubmissionFragment extends AbstractFragment implements BackPressListener {

  /**
   * String constant keys used for persisting state in {@see Bundle} objects.
   */
  private static final class BundleKeys {

    /**
     * Key used to store unsaved responses across activity re-creation.
     */
    private static final String RESTORED_RESPONSES = "restoredResponses";

    /**
     * Key used to store field ID waiting for photo response across activity re-creation.
     */
    private static final String FIELD_WAITING_FOR_PHOTO = "photoFieldId";

    /**
     * Key used to store captured photo Uri across activity re-creation.
     */
    private static final String CAPTURED_PHOTO_PATH = "capturedPhotoPath";
  }

  private final List<AbstractFieldViewModel> fieldViewModelList = new ArrayList<>();

  @Inject
  Navigator navigator;
  @Inject
  FieldViewFactory fieldViewFactory;
  @Inject
  EphemeralPopups popups;
  @Inject
  Schedulers schedulers;
  @Inject
  UserMediaRepository userMediaRepository;

  private EditSubmissionViewModel viewModel;
  private EditSubmissionFragBinding binding;

  private ActivityResultLauncher<String> selectPhotoLauncher;
  private ActivityResultLauncher<Uri> capturePhotoLauncher;

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
    viewModel = getViewModel(EditSubmissionViewModel.class);
    selectPhotoLauncher =
        registerForActivityResult(new GetContent(), viewModel::onSelectPhotoResult);
    capturePhotoLauncher =
        registerForActivityResult(new TakePicture(), viewModel::onCapturePhotoResult);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    binding = EditSubmissionFragBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(this);
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.saveSubmissionBtn.setOnClickListener(this::onSaveClick);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    TwoLineToolbar toolbar = binding.editSubmissionToolbar;
    ((MainActivity) getActivity()).setActionBar(toolbar, R.drawable.ic_close_black_24dp);
    toolbar.setNavigationOnClickListener(__ -> onCloseButtonClick());
    // Observe state changes.
    viewModel.getTask().observe(getViewLifecycleOwner(), this::rebuildTask);
    viewModel
        .getSaveResults()
        .observeOn(schedulers.ui())
        .as(autoDisposable(getViewLifecycleOwner()))
        .subscribe(this::handleSaveResult);

    // Initialize view model.
    Bundle args = getArguments();
    if (savedInstanceState != null) {
      args.putSerializable(
          BundleKeys.RESTORED_RESPONSES,
          savedInstanceState.getSerializable(BundleKeys.RESTORED_RESPONSES));
      viewModel.setFieldWaitingForPhoto(
          savedInstanceState.getString(BundleKeys.FIELD_WAITING_FOR_PHOTO));
      viewModel.setCapturedPhotoPath(
          savedInstanceState.getParcelable(BundleKeys.CAPTURED_PHOTO_PATH));
    }
    viewModel.initialize(EditSubmissionFragmentArgs.fromBundle(args));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(BundleKeys.RESTORED_RESPONSES, viewModel.getDraftResponses());
    outState.putString(BundleKeys.FIELD_WAITING_FOR_PHOTO, viewModel.getFieldWaitingForPhoto());
    outState.putString(BundleKeys.CAPTURED_PHOTO_PATH, viewModel.getCapturedPhotoPath());
  }

  private void handleSaveResult(EditSubmissionViewModel.SaveResult saveResult) {
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

  private void addFieldViewModel(Field field, ViewDataBinding binding) {
    if (binding instanceof PhotoInputFieldBindingImpl) {
      ((PhotoInputFieldBindingImpl) binding).setEditSubmissionViewModel(viewModel);
    }

    AbstractFieldViewModel fieldViewModel = getViewModel(binding);
    fieldViewModel.initialize(field, viewModel.getResponse(field.getId()));

    if (fieldViewModel instanceof PhotoFieldViewModel) {
      initPhotoField((PhotoFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof MultipleChoiceFieldViewModel) {
      observeSelectChoiceClicks((MultipleChoiceFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof DateFieldViewModel) {
      observeDateDialogClicks((DateFieldViewModel) fieldViewModel);
    } else if (fieldViewModel instanceof TimeFieldViewModel) {
      observeTimeDialogClicks((TimeFieldViewModel) fieldViewModel);
    }

    fieldViewModel.getResponse().observe(this, response -> viewModel.setResponse(field, response));

    fieldViewModelList.add(fieldViewModel);
  }

  public void onSaveClick(View view) {
    hideKeyboard(view);
    viewModel.onSaveClick(getValidationErrors());
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

  private void rebuildTask(Task task) {
    LinearLayout formLayout = binding.editSubmissionLayout;
    formLayout.removeAllViews();
    fieldViewModelList.clear();
    for (Step step : task.getStepsSorted()) {
      switch (step.getType()) {
        case FIELD:
          Field field = step.getField();
          ViewDataBinding binding = fieldViewFactory.addFieldView(field.getType(), formLayout);
          addFieldViewModel(field, binding);
          break;
        case UNKNOWN:
        default:
          Timber.e("%s task steps not yet supported", step.getType());
          break;
      }
    }
  }

  private void observeSelectChoiceClicks(MultipleChoiceFieldViewModel viewModel) {
    viewModel
        .getShowDialogClicks()
        .as(autoDisposable(this))
        .subscribe(
            __ ->
                createMultipleChoiceDialog(
                    viewModel.getField(),
                    viewModel.getCurrentResponse(),
                    viewModel::updateResponse)
                    .show());
  }

  private AlertDialog createMultipleChoiceDialog(
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
    photoFieldViewModel.setSurveyId(viewModel.getSurveyId());
    photoFieldViewModel.setSubmissionId(viewModel.getSubmissionId());
    observeSelectPhotoClicks(photoFieldViewModel);
    observePhotoResults(photoFieldViewModel);
  }

  private void observeSelectPhotoClicks(PhotoFieldViewModel fieldViewModel) {
    fieldViewModel
        .getShowDialogClicks()
        .observe(this, __ -> onShowPhotoSelectorDialog(fieldViewModel.getField()));
  }

  private void observePhotoResults(PhotoFieldViewModel fieldViewModel) {
    viewModel
        .getLastPhotoResult()
        .as(autoDisposable(getViewLifecycleOwner()))
        .subscribe(fieldViewModel::onPhotoResult);
  }

  private void onShowPhotoSelectorDialog(Field field) {
    EditSubmissionBottomSheetBinding addPhotoBottomSheetBinding =
        EditSubmissionBottomSheetBinding.inflate(getLayoutInflater());
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
              onSelectPhotoClick(type, field.getId());
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

  private void onSelectPhotoClick(int type, String fieldId) {
    switch (type) {
      case PHOTO_SOURCE_CAMERA:
        // TODO: Launch intent is not invoked if the permission is not granted by default.
        viewModel
            .obtainCapturePhotoPermissions()
            .andThen(Completable.fromAction(() -> launchPhotoCapture(fieldId)))
            .as(autoDisposable(getViewLifecycleOwner()))
            .subscribe();
        break;
      case PHOTO_SOURCE_STORAGE:
        // TODO: Launch intent is not invoked if the permission is not granted by default.
        viewModel
            .obtainSelectPhotoPermissions()
            .andThen(Completable.fromAction(() -> launchPhotoSelector(fieldId)))
            .as(autoDisposable(getViewLifecycleOwner()))
            .subscribe();
        break;
      default:
        throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  private void launchPhotoCapture(String fieldId) {
    File photoFile = userMediaRepository.createImageFile(fieldId);
    Uri uri = FileProvider.getUriForFile(requireContext(), BuildConfig.APPLICATION_ID, photoFile);
    viewModel.setFieldWaitingForPhoto(fieldId);
    viewModel.setCapturedPhotoPath(photoFile.getAbsolutePath());
    capturePhotoLauncher.launch(uri);
    Timber.d("Capture photo intent sent");
  }

  private void launchPhotoSelector(String fieldId) {
    viewModel.setFieldWaitingForPhoto(fieldId);
    selectPhotoLauncher.launch("image/*");
    Timber.d("Select photo intent sent");
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
        .setPositiveButton(R.string.discard_changes, (d, i) -> navigator.navigateUp())
        .setNegativeButton(R.string.continue_editing, (d, i) -> {
        })
        .create()
        .show();
  }

  private void showValidationErrorsAlert() {
    new AlertDialog.Builder(requireContext())
        .setMessage(R.string.invalid_data_warning)
        .setPositiveButton(R.string.invalid_data_confirm, (a, b) -> {
        })
        .create()
        .show();
  }
}
