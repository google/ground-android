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
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import com.google.android.gnd.R;
import com.google.android.gnd.ui.common.GndDialogFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import com.google.android.gnd.vo.Project;
import java.io.Serializable;
import java.util.List;
import javax.inject.Inject;

public class ProjectSelectorDialogFragment extends GndDialogFragment {
  private static final String TAG = ProjectSelectorDialogFragment.class.getSimpleName();
  private static final String PROJECTS_BUNDLE_KEY = "projects";

  @Inject GndViewModelFactory viewModelFactory;

  private ProjectSelectorViewModel viewModel;

  public ProjectSelectorDialogFragment() {}

  public static void show(FragmentManager fragmentManager, List<Project> availableProjects) {
    ProjectSelectorDialogFragment dialog = new ProjectSelectorDialogFragment();
    Bundle bundle = new Bundle();
    bundle.putSerializable(PROJECTS_BUNDLE_KEY, (Serializable) availableProjects);
    dialog.setArguments(bundle);
    dialog.show(fragmentManager, TAG);
  }

  @Override
  protected void onCreateViewModel() {
    this.viewModel = viewModelFactory.create(ProjectSelectorViewModel.class);
  }

  // TODO: Replace AlertDialog with a properly designed project selector.
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    List<Project> availableProjects =
        (List<Project>) getArguments().getSerializable(PROJECTS_BUNDLE_KEY);
    if (availableProjects == null) {
      Log.e(TAG, "Null availableProjects when showing project selector dialog");
      return null;
    }
    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
    dialog.setTitle(R.string.select_project_dialog_title);
    // TODO: i18n.
    String[] projectTitles =
        stream(availableProjects).map(p -> p.getTitle()).toArray(String[]::new);
    dialog.setItems(
        projectTitles,
        (d, which) -> {
          onProjectSelection(availableProjects.get(which).getId());
        });
    dialog.setCancelable(false);

    return dialog.create();
  }

  private void onProjectSelection(String id) {
    viewModel.activateProject(id).as(autoDisposable(this)).subscribe();
  }
}
