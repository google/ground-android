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

package com.google.android.ground.ui.surveyselector;

import static com.google.common.base.Preconditions.checkNotNull;
import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.google.android.ground.R;
import com.google.android.ground.databinding.SurveySelectorDialogBinding;
import com.google.android.ground.model.Survey;
import com.google.android.ground.rx.Loadable;
import com.google.android.ground.ui.common.AbstractDialogFragment;
import com.google.android.ground.ui.common.EphemeralPopups;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

/** User interface implementation of survey selector dialog. */
@AndroidEntryPoint
public class SurveySelectorDialogFragment extends AbstractDialogFragment {
  @Inject EphemeralPopups popups;

  private SurveySelectorViewModel viewModel;

  @SuppressWarnings("NullAway")
  private SurveySelectorDialogBinding binding;

  @Nullable private ArrayAdapter listAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.viewModel = getViewModel(SurveySelectorViewModel.class);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
    dialog.setTitle(R.string.join_survey);
    LayoutInflater inflater = getActivity().getLayoutInflater();
    binding = SurveySelectorDialogBinding.inflate(inflater);
    listAdapter =
        new ArrayAdapter(getContext(), R.layout.survey_selector_list_item, R.id.survey_name);
    binding.surveySelectorListView.setAdapter(listAdapter);
    viewModel.getSurveySummaries().observe(this, this::updateSurveyList);
    binding.surveySelectorListView.setOnItemClickListener(
        (parent, view, index, id) -> onItemSelected(index));
    dialog.setView(binding.getRoot());
    dialog.setCancelable(false);
    return dialog.create();
  }

  private void updateSurveyList(Loadable<List<Survey>> surveySummaries) {
    switch (surveySummaries.getState()) {
      case LOADING:
        Timber.i("Loading surveys");
        break;
      case LOADED:
        surveySummaries.value().ifPresent(this::showSurveyList);
        break;
      case ERROR:
        onSurveyListLoadError(surveySummaries.error().orElse(new UnknownError()));
        break;
      default:
        Timber.e("Unhandled state: %s", surveySummaries.getState());
        break;
    }
  }

  private void onSurveyListLoadError(Throwable t) {
    Timber.e(t, "Survey list not available");
    popups.showError(R.string.survey_list_load_error);
    dismiss();
  }

  private void showSurveyList(List<Survey> list) {
    binding.listLoadingProgressBar.setVisibility(View.GONE);

    checkNotNull(listAdapter, "listAdapter was null when attempting to show survey list");

    listAdapter.clear();
    stream(list).map(Survey::getTitle).forEach(listAdapter::add);
    binding.surveySelectorListView.setVisibility(View.VISIBLE);
  }

  private void onItemSelected(int index) {
    dismiss();
    viewModel.activateSurvey(index);
  }
}
