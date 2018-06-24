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

package com.google.android.gnd.ui.browse;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;

import android.app.ProgressDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.ProjectState;
import com.google.android.gnd.ui.browse.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.AbstractViewModelFactory;
import com.google.android.gnd.ui.common.Consumable;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import java.util.List;
import javax.inject.Inject;

/**
 * Fragment containing the map container and place sheet fragments. This is the default view in the
 * application, and gets swapped out for other fragments (e.g., view record and edit record) at
 * runtime.
 */
public class BrowseFragment extends AbstractFragment {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 16.0f / 9.0f;

  @Inject
  AbstractViewModelFactory viewModelFactory;

  @Inject MapContainerFragment mapContainerFragment;

  @Inject AddPlaceDialogFragment addPlaceDialogFragment;

  @BindView(R.id.toolbar_wrapper)
  ViewGroup toolbarWrapper;

  @BindView(R.id.toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.bottom_sheet_scroll_view)
  NestedScrollView bottomSheetScrollView;

  @BindView(R.id.add_record_btn)
  View addRecordBtn;

  @BindView(R.id.bottom_sheet_bottom_inset_scrim)
  View bottomSheetBottomInsetScrim;

  private ProgressDialog progressDialog;
  private BrowseViewModel viewModel;
  private BottomSheetBehavior<NestedScrollView> bottomSheetBehavior;
  private MainViewModel mainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public void obtainViewModels() {
    viewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(BrowseViewModel.class);
    mainViewModel = ViewModelProviders.of(getActivity(), viewModelFactory).get(MainViewModel.class);
  }

  @Override
  public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main, container, false);
  }

  @Override
  protected void restoreViewState() {
    setUpBottomSheetBehavior();
    setUpToolbar();
  }

  private void setUpBottomSheetBehavior() {
    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetScrollView);
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
  }

  private void setUpToolbar() {
    MainActivity activity = (MainActivity) getActivity();
    activity.setSupportActionBar(toolbar);
    ActionBar actionBar = activity.getSupportActionBar();
    // Workaround to get rid of application title from toolbar. Setting "" here or in layout XML
    // doesn't work.
    actionBar.setDisplayShowTitleEnabled(false);
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setDisplayShowHomeEnabled(true);
  }

  @Override
  protected void addFragments() {
    addFragment(R.id.map_container_fragment, mapContainerFragment);
  }

  protected void observeViewModel() {
    viewModel
        .getShowProjectSelectorDialogRequests()
        .observe(this, this::onShowProjectSelectorDialogRequest);
    viewModel.getProjectState().observe(this, this::onProjectStateChange);
    viewModel.getShowAddPlaceDialogRequests().observe(this, this::onShowAddPlaceDialogRequest);
    viewModel.getPlaceSheetEvents().observe(this, this::onPlaceSheetEvent);
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.place_sheet_menu, menu);
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    bottomSheetBottomInsetScrim.setMinimumHeight(insets.getSystemWindowInsetBottom());
    toolbarWrapper.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
  }

  private void onShowProjectSelectorDialogRequest(Consumable<List<Project>> projects) {
    projects.get().ifPresent(p -> ProjectSelectorDialogFragment.show(getFragmentManager(), p));
  }

  private void onProjectStateChange(ProjectState event) {
    if (event.isLoading()) {
      showProjectLoadingDialog();
    } else if (event.isActivated()) {
      dismissLoadingDialog();
    } else {
      // TODO: Reuse last selected project instead of asking to select every time.
      // TODO: Trigger this from welcome flow and nav drawer instead of here.
      viewModel.showProjectSelectorDialog().as(autoDisposable(this)).subscribe();
    }
  }

  private void onShowAddPlaceDialogRequest(Point location) {
    // TODO: Pause location updates while dialog is open.
    addPlaceDialogFragment
        .show(getChildFragmentManager())
        .as(autoDisposable(this))
        .subscribe(viewModel::onAddPlace);
  }

  private void onPlaceSheetEvent(PlaceSheetEvent event) {
    // TODO: WHY IS CALLED 3x ON CLICK?
    switch (event.getType()) {
      case SHOW:
        toolbar.setTitle(event.getTitle());
        toolbar.setSubtitle(event.getSubtitle());
        showBottomSheet();
        break;
      case HIDE:
        hideBottomSheet();
        break;
    }
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
    addRecordBtn.setVisibility(View.VISIBLE);
  }

  private void hideBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    addRecordBtn.setVisibility(View.GONE);
  }

  private void showProjectLoadingDialog() {
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
