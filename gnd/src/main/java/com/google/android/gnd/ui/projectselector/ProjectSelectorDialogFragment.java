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

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static java8.util.stream.StreamSupport.stream;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractDialogFragment;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.vo.Project;
import java.util.List;

public class ProjectSelectorDialogFragment extends AbstractDialogFragment {

  private static final String TAG = ProjectSelectorDialogFragment.class.getSimpleName();

  @BindView(R.id.list_loading_progress_bar)
  View listLoadingProgressBar;

  @BindView(R.id.list_view)
  ListView listView;

  private ProjectSelectorViewModel viewModel;
  private ArrayAdapter listAdapter;

  public static void show(FragmentManager fragmentManager) {
    new ProjectSelectorDialogFragment().show(fragmentManager, TAG);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.viewModel = get(ProjectSelectorViewModel.class);
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    super.onCreateDialog(savedInstanceState);
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
    dialog.setTitle(R.string.join_project_dialog_title);
    LayoutInflater inflater = getActivity().getLayoutInflater();
    ViewGroup dialogView = (ViewGroup) inflater.inflate(R.layout.project_selector_dialog, null);
    ButterKnife.bind(this, dialogView);
    listAdapter =
        new ArrayAdapter(getContext(), R.layout.project_selector_list_item, R.id.project_name);
    listView.setAdapter(listAdapter);
    viewModel.getProjectSummaries().observe(this, this::update);
    listView.setOnItemClickListener(this::onItemSelected);
    dialog.setView(dialogView);
    dialog.setCancelable(false);
    return dialog.create();
  }

  @Override
  public void onStart() {
    super.onStart();
    viewModel.loadProjectSummaries();
  }

  private void update(Resource<List<Project>> projectSummaries) {
    switch (projectSummaries.getStatus()) {
      case LOADED:
        projectSummaries.ifPresent(this::showProjectList);
        break;
      case NOT_FOUND:
      case ERROR:
        Log.e(
          TAG,
          "Project list not available",
          projectSummaries.getError().orElse(new UnknownError()));
        EphemeralPopups.showError(getContext(), R.string.project_list_load_error);
        dismiss();
        break;
    }
  }

  private void showProjectList(List<Project> list) {
    listLoadingProgressBar.setVisibility(View.GONE);
    listAdapter.clear();
    stream(list).map(Project::getTitle).forEach(listAdapter::add);
    listView.setVisibility(View.VISIBLE);
  }

  private void onItemSelected(AdapterView<?> parent, View view, int idx, long id) {
    getDialog().hide();
    viewModel
      .activateProject(idx)
      .as(autoDisposable(this))
      .subscribe(this::dismiss, this::onActivateProjectFailure);
  }

  private void onActivateProjectFailure(Throwable throwable) {
    Log.e(TAG, "Project load exception", throwable);
    dismiss();
    EphemeralPopups.showError(getContext(), R.string.project_load_error);
  }
}
