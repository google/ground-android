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

package com.google.android.gnd.ui.home;

import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;

import android.app.ProgressDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.HomeScreenFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.BottomSheetBehavior;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import javax.inject.Inject;

/**
 * Fragment containing the map container and place sheet fragments. This is the default view in the
 * application, and gets swapped out for other fragments (e.g., view record and edit record) at
 * runtime.
 */
@ActivityScoped
public class HomeScreenFragment extends AbstractFragment
    implements BackPressListener, OnNavigationItemSelectedListener {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f;
  private static final String TAG = HomeScreenFragment.class.getSimpleName();

  @Inject AddPlaceDialogFragment addPlaceDialogFragment;
  @Inject AuthenticationManager authenticationManager;

  @BindView(R.id.toolbar_wrapper)
  ViewGroup toolbarWrapper;

  @BindView(R.id.toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.status_bar_scrim)
  View statusBarScrim;

  @BindView(R.id.drawer_layout)
  DrawerLayout drawerLayout;

  @BindView(R.id.nav_view)
  NavigationView navView;

  @BindView(R.id.bottom_sheet_header)
  ViewGroup bottomSheetHeader;

  @BindView(R.id.bottom_sheet_scroll_view)
  View bottomSheetScrollView;

  @BindView(R.id.add_record_btn)
  View addRecordBtn;

  @BindView(R.id.bottom_sheet_bottom_inset_scrim)
  View bottomSheetBottomInsetScrim;

  @BindView(R.id.version_text)
  TextView versionTextView;

  private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    get(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    viewModel = get(HomeScreenViewModel.class);
    viewModel.getActiveProject().observe(this, this::onActiveProjectChange);
    viewModel.getShowAddPlaceDialogRequests().observe(this, this::onShowAddPlaceDialogRequest);
    viewModel.getPlaceSheetState().observe(this, this::onPlaceSheetStateChange);
    viewModel.getOpenDrawerRequests().observe(this, __ -> openDrawer());
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    HomeScreenFragBinding binding = HomeScreenFragBinding.inflate(inflater, container, false);
    binding.placeSheetChrome.setViewModel(viewModel);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    versionTextView.setText("Build " + getVersionName());
    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

    navView.setNavigationItemSelectedListener(this);
    getView().getViewTreeObserver().addOnGlobalLayoutListener(this::onToolbarLayout);

    if (savedInstanceState == null) {
      mapContainerFragment = new MapContainerFragment();
      replaceFragment(R.id.map_container_fragment, mapContainerFragment);
    } else {
      mapContainerFragment = restoreChildFragment(savedInstanceState, MapContainerFragment.class);
    }

    setUpBottomSheetBehavior();
  }

  private String getVersionName() {
    try {
      PackageInfo pInfo =
          getContext().getPackageManager().getPackageInfo(getContext().getPackageName(), 0);
      return pInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
      return "?";
    }
  }

  private void onToolbarLayout() {
    if (toolbarWrapper == null || bottomSheetBehavior == null || bottomSheetHeader == null) {
      return;
    }
    bottomSheetBehavior.setFitToContents(false);
    bottomSheetBehavior.setExpandedOffset(
        toolbarWrapper.getHeight() - bottomSheetHeader.getHeight());
  }

  private void setUpBottomSheetBehavior() {
    bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetScrollView);
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);

    ((MainActivity) getActivity()).setActionBar(toolbar);
  }

  private void openDrawer() {
    drawerLayout.openDrawer(Gravity.START);
  }

  private void closeDrawer() {
    drawerLayout.closeDrawer(Gravity.START);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.place_sheet_menu, menu);
  }

  @Override
  public void onStart() {
    super.onStart();
    // TODO: Persist last selected project in local db.
    // TODO: Create startup flow and move this logic there.
    Resource<Project> activeProject = viewModel.getActiveProject().getValue();
    if (activeProject == null || !activeProject.isLoaded()) {
      showProjectSelector();
    }
  }

  private void showProjectSelector() {
    ProjectSelectorDialogFragment.show(getFragmentManager());
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    statusBarScrim.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    toolbarWrapper.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    bottomSheetBottomInsetScrim.setMinimumHeight(insets.getSystemWindowInsetBottom());
    updateNavViewInsets(insets);
    updateBottomSheetPeekHeight(insets);
  }

  private void updateNavViewInsets(WindowInsetsCompat insets) {
    View headerView = navView.getHeaderView(0);
    headerView.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
  }

  private void updateBottomSheetPeekHeight(WindowInsetsCompat insets) {
    double width =
        getScreenWidth(getActivity())
            + insets.getSystemWindowInsetLeft()
            + insets.getSystemWindowInsetRight();
    double height =
        getScreenHeight(getActivity())
            + insets.getSystemWindowInsetTop()
            + insets.getSystemWindowInsetBottom();
    double mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO;
    double peekHeight = height - mapHeight;
    bottomSheetBehavior.setPeekHeight((int) peekHeight);
  }

  private void onActiveProjectChange(Resource<Project> project) {
    switch (project.getStatus()) {
      case NOT_LOADED:
        dismissLoadingDialog();
        break;
      case LOADING:
        showProjectLoadingDialog();
        break;
      case LOADED:
        dismissLoadingDialog();
        break;
      case NOT_FOUND:
      case ERROR:
        EphemeralPopups.showError(getContext(), R.string.project_load_error);
        Log.e(TAG, "Project load error", project.getError().orElse(new UnknownError()));
        break;
    }
  }

  private void onShowAddPlaceDialogRequest(Point location) {
    if (!Resource.getData(viewModel.getActiveProject()).isPresent()) {
      return;
    }
    // TODO: Pause location updates while dialog is open.
    // TODO: Show spinner?
    addPlaceDialogFragment
        .show(getChildFragmentManager())
        .as(autoDisposable(this))
        .subscribe(viewModel::addPlace);
  }

  private void onPlaceSheetStateChange(PlaceSheetState state) {
    // TODO: WHY IS CALLED 3x ON CLICK?
    switch (state.getVisibility()) {
      case VISIBLE:
        Place place = state.getPlace();
        toolbar.setTitle(place.getTitle());
        toolbar.setSubtitle(place.getSubtitle());
        showBottomSheet();
        break;
      case HIDDEN:
        hideBottomSheet();
        break;
    }
  }

  private void showBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    addRecordBtn.setVisibility(View.VISIBLE);
  }

  private void hideBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    addRecordBtn.setVisibility(View.GONE);
  }

  private void showProjectLoadingDialog() {
    if (progressDialog == null) {
      progressDialog =
          ProgressDialogs.modalSpinner(getContext(), R.string.project_loading_please_wait);
      progressDialog.show();
    }
  }

  public void dismissLoadingDialog() {
    if (progressDialog != null) {
      progressDialog.dismiss();
      progressDialog = null;
    }
  }

  @Override
  public boolean onBack() {
    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
      return false;
    } else {
      hideBottomSheet();
      return true;
    }
  }

  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    switch (item.getItemId()) {
      case R.id.nav_join_project:
        showProjectSelector();
        closeDrawer();
        break;
      case R.id.nav_sign_out:
        authenticationManager.signOut();
        break;
    }
    return false;
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
