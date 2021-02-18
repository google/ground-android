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

package com.google.android.gnd.ui.projectselector;

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
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.ProjectSelectorDialogBinding;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.List;
import javax.inject.Inject;
import timber.log.Timber;

/** User interface implementation of project selector dialog. */
@AndroidEntryPoint
public class ProjectSelectorDialogFragment extends AbstractDialogFragment {
  @Inject EphemeralPopups popups;

  private ProjectSelectorViewModel viewModel;

  @SuppressWarnings("NullAway")
  private ProjectSelectorDialogBinding binding;

  @Nullable private ArrayAdapter listAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.viewModel = getViewModel(ProjectSelectorViewModel.class);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
    dialog.setTitle(R.string.join_project);
    LayoutInflater inflater = getActivity().getLayoutInflater();
    binding = ProjectSelectorDialogBinding.inflate(inflater);
    listAdapter =
        new ArrayAdapter(getContext(), R.layout.project_selector_list_item, R.id.project_name);
    binding.projectSelectorListView.setAdapter(listAdapter);
    viewModel.getProjectSummaries().observe(this, this::updateProjectList);
    binding.projectSelectorListView.setOnItemClickListener(
        (parent, view, index, id) -> onItemSelected(index));
    dialog.setView(binding.getRoot());
    dialog.setCancelable(false);
    return dialog.create();
  }

  private void updateProjectList(Loadable<List<Project>> projectSummaries) {
    switch (projectSummaries.getState()) {
      case LOADING:
        Timber.i("Loading projects");
        break;
      case LOADED:
        projectSummaries.value().ifPresent(this::showProjectList);
        break;
      case NOT_FOUND:
      case ERROR:
        onProjectListLoadError(projectSummaries.error().orElse(new UnknownError()));
        break;
      default:
        Timber.e("Unhandled state: %s", projectSummaries.getState());
        break;
    }
  }

  private void onProjectListLoadError(Throwable t) {
    Timber.e(t, "Project list not available");
    popups.showError(R.string.project_list_load_error);
    dismiss();
  }

  private void showProjectList(List<Project> list) {
    binding.listLoadingProgressBar.setVisibility(View.GONE);

    checkNotNull(listAdapter, "listAdapter was null when attempting to show project list");

    listAdapter.clear();
    stream(list).map(Project::getTitle).forEach(listAdapter::add);
    binding.projectSelectorListView.setVisibility(View.VISIBLE);
  }

  private void onItemSelected(int index) {
    dismiss();
    viewModel.activateProject(index);
  }
}
