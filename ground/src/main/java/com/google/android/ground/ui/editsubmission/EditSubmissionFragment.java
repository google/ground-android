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

package com.google.android.ground.ui.editsubmission;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.ground.MainActivity;
import com.google.android.ground.R;
import com.google.android.ground.databinding.EditSubmissionFragBinding;
import com.google.android.ground.model.job.Job;
import com.google.android.ground.repository.UserMediaRepository;
import com.google.android.ground.rx.Schedulers;
import com.google.android.ground.ui.common.AbstractFragment;
import com.google.android.ground.ui.common.BackPressListener;
import com.google.android.ground.ui.common.EphemeralPopups;
import com.google.android.ground.ui.common.Navigator;
import com.google.android.ground.ui.common.TwoLineToolbar;
import com.google.android.ground.ui.datacollection.AbstractTaskViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

@AndroidEntryPoint
public class EditSubmissionFragment extends AbstractFragment implements BackPressListener {

  /** String constant keys used for persisting state in {@see Bundle} objects. */
  private static final class BundleKeys {

    /** Key used to store unsaved responses across activity re-creation. */
    private static final String RESTORED_RESPONSES = "restoredResponses";
  }

  private final List<AbstractTaskViewModel> taskViewModels = new ArrayList<>();

  @Inject Navigator navigator;
  @Inject TaskViewFactory taskViewFactory;
  @Inject EphemeralPopups popups;
  @Inject Schedulers schedulers;
  @Inject UserMediaRepository userMediaRepository;

  private EditSubmissionViewModel viewModel;
  private EditSubmissionFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = getViewModel(EditSubmissionViewModel.class);
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
    viewModel.getJob().observe(getViewLifecycleOwner(), this::rebuildForm);

    // Initialize view model.
    Bundle args = getArguments();
    if (savedInstanceState != null) {
      args.putSerializable(
          BundleKeys.RESTORED_RESPONSES,
          savedInstanceState.getSerializable(BundleKeys.RESTORED_RESPONSES));
    }
    viewModel.initialize(EditSubmissionFragmentArgs.fromBundle(args));
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(BundleKeys.RESTORED_RESPONSES, viewModel.getDraftResponses());
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
    for (AbstractTaskViewModel fieldViewModel : taskViewModels) {
      String error = fieldViewModel.validate();
      if (error != null) {
        errors.put(fieldViewModel.getTask().getId(), error);
      }
    }
    return errors;
  }

  private void rebuildForm(Job job) {
    LinearLayout formLayout = binding.editSubmissionLayout;
    formLayout.removeAllViews();
    taskViewModels.clear();
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
        .setNegativeButton(R.string.continue_editing, (d, i) -> {})
        .create()
        .show();
  }
}
