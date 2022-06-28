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

package com.google.android.gnd.ui.submissiondetails;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog.Builder;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.PhotoFieldBinding;
import com.google.android.gnd.databinding.SubmissionDetailsFieldBinding;
import com.google.android.gnd.databinding.SubmissionDetailsFieldBindingImpl;
import com.google.android.gnd.databinding.SubmissionDetailsFragBinding;
import com.google.android.gnd.databinding.SubmissionDetailsFragBindingImpl;
import com.google.android.gnd.model.submission.Response;
import com.google.android.gnd.model.submission.Submission;
import com.google.android.gnd.model.task.Field;
import com.google.android.gnd.model.task.Field.Type;
import com.google.android.gnd.model.task.Step;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.editsubmission.PhotoFieldViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

@AndroidEntryPoint
public class SubmissionDetailsFragment extends AbstractFragment {

  @Inject
  Navigator navigator;
  @Inject
  EphemeralPopups popups;

  private SubmissionDetailsViewModel viewModel;
  private SubmissionDetailsFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SubmissionDetailsFragmentArgs args = getSubmissionDetailsFragmentArgs();
    viewModel = getViewModel(SubmissionDetailsViewModel.class);
    viewModel.submission.observe(this, this::onUpdate);
    viewModel.loadSubmissionDetails(args);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    binding = SubmissionDetailsFragBindingImpl.inflate(inflater, container, false);
    binding.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((MainActivity) getActivity()).setActionBar(binding.submissionDetailsToolbar, false);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.submission_details_menu, menu);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
  }

  private void onUpdate(Loadable<Submission> submission) {
    switch (submission.getState()) {
      case LOADED:
        submission.value().ifPresent(this::showSubmission);
        break;
      case NOT_FOUND:
      case ERROR:
        // TODO: Replace w/error view?
        Timber.e("Failed to load submission");
        popups.showError();
        break;
      default:
        Timber.e("Unhandled state: %s", submission.getState());
        break;
    }
  }

  private void showSubmission(Submission submission) {
    binding.submissionDetailsLayout.removeAllViews();
    for (Step step : submission.getTask().getStepsSorted()) {
      if (step.getType() == Step.Type.FIELD) {
        addField(step.getField(), submission);
      }
    }
  }

  private void addField(Field field, Submission submission) {
    SubmissionDetailsFieldBinding fieldBinding =
        SubmissionDetailsFieldBindingImpl.inflate(getLayoutInflater());
    fieldBinding.setField(field);
    fieldBinding.setLifecycleOwner(this);
    binding.submissionDetailsLayout.addView(fieldBinding.getRoot());

    submission
        .getResponses()
        .getResponse(field.getId())
        .ifPresent(
            response -> {
              if (field.getType() == Type.PHOTO) {
                fieldBinding.fieldValue.setVisibility(View.GONE);
                addPhotoField((ViewGroup) fieldBinding.getRoot(), response);
              } else {
                fieldBinding.fieldValue.setText(response.getDetailsText());
              }
            });
  }

  private void addPhotoField(ViewGroup container, Response response) {
    PhotoFieldViewModel photoFieldViewModel = viewModelFactory.create(PhotoFieldViewModel.class);
    photoFieldViewModel.setResponse(Optional.of(response));
    PhotoFieldBinding photoFieldBinding = PhotoFieldBinding.inflate(getLayoutInflater());
    photoFieldBinding.setLifecycleOwner(this);
    photoFieldBinding.setViewModel(photoFieldViewModel);
    container.addView(photoFieldBinding.getRoot());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    SubmissionDetailsFragmentArgs args = getSubmissionDetailsFragmentArgs();
    String surveyId = args.getSurveyId();
    String featureId = args.getFeatureId();
    String submissionId = args.getSubmissionId();

    if (item.getItemId() == R.id.edit_submission_menu_item) {
      navigator.navigate(
          SubmissionDetailsFragmentDirections.editSubmission(surveyId, featureId, submissionId));
    } else if (item.getItemId() == R.id.delete_submission_menu_item) {
      new Builder(requireActivity())
          .setTitle(R.string.submission_delete_confirmation_dialog_title)
          .setMessage(R.string.submission_delete_confirmation_dialog_message)
          .setPositiveButton(
              R.string.delete_button_label,
              (dialog, id) ->
                  viewModel
                      .deleteCurrentSubmission(surveyId, featureId, submissionId)
                      .as(autoDisposable(this))
                      .subscribe(() -> navigator.navigateUp()))
          .setNegativeButton(
              R.string.cancel_button_label,
              (dialog, id) -> {
                // Do nothing.
              })
          .create()
          .show();
    } else {
      return false;
    }

    return true;
  }

  private SubmissionDetailsFragmentArgs getSubmissionDetailsFragmentArgs() {
    return SubmissionDetailsFragmentArgs.fromBundle(getArguments());
  }
}
