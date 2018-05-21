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

package com.google.android.gnd.ui;

import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gnd.R;
import com.google.android.gnd.model.Point;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.ProjectActivationEvent;
import com.google.android.gnd.ui.common.GndFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import com.google.android.gnd.ui.mapcontainer.MapContainerFragment;
import java.util.List;
import javax.inject.Inject;

public class MainFragment extends GndFragment {
  @Inject
  GndViewModelFactory viewModelFactory;

  @Inject
  MapContainerFragment mapContainerFragment;

  @Inject
  AddPlaceDialogFragment addPlaceDialogFragment;

  private ProgressDialog progressDialog;
  private MainViewModel viewModel;

  @Override
  public void onCreateViewModel() {
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel.class);
  }

  @Override
  public View onInflateView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main, container, false);
  }

  @Override
  protected void onAddFragments() {
    addFragment(R.id.map_fragment, mapContainerFragment);
  }

  protected void onObserveViewModel() {
    viewModel
      .showProjectSelectorDialogRequests()
      .observe(this, this::onShowProjectSelectorDialogRequest);
    viewModel.projectActivationEvents().observe(this, this::onProjectActivationEvent);
    viewModel.showAddPlaceDialogRequests().observe(this, this::onShowAddPlaceDialogRequest);
  }

  private void onShowProjectSelectorDialogRequest(List<Project> projects) {
    ProjectSelectorDialogFragment.show(getFragmentManager(), projects);
  }

  private void onProjectActivationEvent(ProjectActivationEvent event) {
    if (event.isLoading()) {
      showProjectLoadingDialog();
    } else if (event.isActivated()) {
      dismissLoadingDialog();
    }
  }

  private void onShowAddPlaceDialogRequest(Point location) {
    // TODO: Pause location updates while dialog is open.
    addPlaceDialogFragment
      .show(getChildFragmentManager())
      .subscribe(viewModel::onAddPlace);

  }

  @Override
  public void onStart() {
    super.onStart();
    // TODO: Reuse last selected project instead of asking to sign in every time.
    // TODO: Trigger this from welcome flow and nav drawer instead of here.
    viewModel.showProjectSelectorDialog();
  }

  public void showProjectLoadingDialog() {
    // TODO: Move this into ProjectSelectorDialogFragment and observe Rx stream emitted by
    // activateProject rather than keeping reference here.
    progressDialog = new ProgressDialog(getContext());
    progressDialog.setMessage(getResources().getString(R.string.project_loading_please_wait));
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.setCancelable(false);
    progressDialog.setCanceledOnTouchOutside(false);
    progressDialog.show();
  }

  public void dismissLoadingDialog() {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }
}
