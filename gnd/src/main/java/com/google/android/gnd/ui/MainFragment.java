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

import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;

import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainActivityViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Point;
import com.google.android.gnd.repository.Project;
import com.google.android.gnd.repository.ProjectActivationEvent;
import com.google.android.gnd.ui.common.GndFragment;
import com.google.android.gnd.ui.common.GndViewModelFactory;
import com.google.android.gnd.ui.mapcontainer.MapContainerFragment;
import java.util.List;
import javax.inject.Inject;

public class MainFragment extends GndFragment {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 16.0f / 9.0f;

  @Inject
  GndViewModelFactory viewModelFactory;

  @Inject
  MapContainerFragment mapContainerFragment;

  @Inject
  AddPlaceDialogFragment addPlaceDialogFragment;

  @BindView(R.id.toolbar_wrapper)
  ViewGroup toolbarWrapper;

  @BindView(R.id.toolbar)
  Toolbar toolbar;

  @BindView(R.id.toolbar_title_text)
  TextView toolbarTitle;

  @BindView(R.id.toolbar_subtitle_text)
  TextView toolbarSubtitle;

  @BindView(R.id.bottom_sheet_scroll_view)
  NestedScrollView bottomSheetScrollView;

  @BindView(R.id.bottom_sheet_bottom_inset_scrim)
  View bottomSheetBottomInsetScrim;

  private ProgressDialog progressDialog;
  private MainViewModel viewModel;
  private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
  private MainActivityViewModel mainActivityViewModel;

  @Override
  public void createViewModel() {
    viewModel = ViewModelProviders.of(this, viewModelFactory).get(MainViewModel.class);
    mainActivityViewModel =
      ViewModelProviders.of(getActivity(), viewModelFactory).get(MainActivityViewModel.class);
  }

  @Override
  public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main, container, false);
  }

  @Override
  protected void initializeViews() {
    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetScrollView);
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
    hideDefaultToolbarTitle();
  }

  /**
   * Workaround to get rid of application title from toolbar. Setting "" here or in layout XML
   * doesn't work.
   */
  private void hideDefaultToolbarTitle() {
    MainActivity activity = (MainActivity) getActivity();
    activity.setSupportActionBar(toolbar);
    activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
  }

  @Override
  protected void addFragments() {
    addFragment(R.id.map_container_fragment, mapContainerFragment);
  }

  protected void observeViewModel() {
    viewModel
      .showProjectSelectorDialogRequests()
      .observe(this, this::onShowProjectSelectorDialogRequest);
    viewModel.projectActivationEvents().observe(this, this::onProjectActivationEvent);
    viewModel.showAddPlaceDialogRequests().observe(this, this::onShowAddPlaceDialogRequest);
    viewModel.getPlaceSheetEvents().observe(this, this::onPlaceSheetEvent);
    mainActivityViewModel.getWindowInsetsLiveData().observe(this, this::onApplyWindowInsets);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    bottomSheetBottomInsetScrim.setMinimumHeight(insets.getSystemWindowInsetBottom());
    toolbarWrapper.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
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
    addPlaceDialogFragment.show(getChildFragmentManager()).subscribe(viewModel::onAddPlace);
  }

  private void onPlaceSheetEvent(PlaceSheetEvent event) {
    switch (event.getType()) {
      case SHOW:
        updateToolbar(event);
        showBottomSheet();
        break;
      case HIDE:
        hideBottomSheet();
        break;
    }
  }

  private void updateToolbar(PlaceSheetEvent event) {
    toolbarTitle.setText(event.getTitle());
    toolbarSubtitle.setText(event.getSubtitle());
    toolbarSubtitle.setVisibility(event.getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);
  }

  private void showBottomSheet() {
    double width = getScreenWidth(getActivity());
    double screenHeight = getScreenHeight(getActivity());
    double mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO;
    double peekHeight = screenHeight - mapHeight;
    bottomSheetScrollView.setPadding(0, toolbarWrapper.getHeight(), 0, 0);
    // TODO: Take window insets into account; COLLAPSED_MAP_ASPECT_RATIO will be wrong on older
    // devices w/o translucent system windows.
    bottomSheetBehavior.setPeekHeight((int) peekHeight);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
  }

  private void hideBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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

  private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
    @Override
    public void onStateChanged(@NonNull View bottomSheet, int newState) {
      if (newState == BottomSheetBehavior.STATE_HIDDEN) {
        viewModel.onBottomSheetHidden();
      }
    }

    @Override
    public void onSlide(@NonNull View bottomSheet, float slideOffset) {
      // no-op.
    }
  }
}
