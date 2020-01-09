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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import butterknife.BindView;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.HomeScreenFragBinding;
import com.google.android.gnd.inject.ActivityScoped;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.repository.Loadable;
import com.google.android.gnd.system.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.BottomSheetBehavior;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import io.reactivex.subjects.PublishSubject;
import java.util.Objects;
import javax.inject.Inject;

/**
 * Fragment containing the map container and feature sheet fragments and NavigationView side drawer.
 * This is the default view in the application, and gets swapped out for other fragments (e.g., view
 * observation and edit observation) at runtime.
 */
@ActivityScoped
public class HomeScreenFragment extends AbstractFragment
    implements BackPressListener, OnNavigationItemSelectedListener {
  // TODO: It's not obvious which feature are in HomeScreen vs MapContainer; make this more
  // intuitive.
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f;
  private static final String TAG = HomeScreenFragment.class.getSimpleName();

  @Inject AddFeatureDialogFragment addFeatureDialogFragment;
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

  @BindView(R.id.bottom_sheet_bottom_inset_scrim)
  View bottomSheetBottomInsetScrim;

  @BindView(R.id.version_text)
  TextView versionTextView;

  private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private PublishSubject<Object> showFeatureDialogRequests;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getViewModel(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    viewModel = getViewModel(HomeScreenViewModel.class);
    viewModel.getActiveProject().observe(this, this::onActiveProjectChange);
    viewModel
        .getShowAddFeatureDialogRequests()
        .observe(this, e -> e.ifUnhandled(this::onShowAddFeatureDialogRequest));
    viewModel.getFeatureSheetState().observe(this, this::onFeatureSheetStateChange);
    viewModel.getOpenDrawerRequests().observe(this, e -> e.ifUnhandled(this::openDrawer));

    showFeatureDialogRequests = PublishSubject.create();

    showFeatureDialogRequests
        .switchMapMaybe(__ -> addFeatureDialogFragment.show(getChildFragmentManager()))
        .as(autoDisposable(this))
        .subscribe(viewModel::addFeature);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    HomeScreenFragBinding binding = HomeScreenFragBinding.inflate(inflater, container, false);
    binding.featureSheetChrome.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
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
      setUpBottomSheetBehavior();
    } else {
      mapContainerFragment = restoreChildFragment(savedInstanceState, MapContainerFragment.class);
    }
  }

  private String getVersionName() {
    try {
      return Objects.requireNonNull(getContext())
          .getPackageManager()
          .getPackageInfo(getContext().getPackageName(), 0)
          .versionName;
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

    ((MainActivity) getActivity()).setActionBar(toolbar, false);
  }

  private void openDrawer() {
    drawerLayout.openDrawer(GravityCompat.START);
  }

  private void closeDrawer() {
    drawerLayout.closeDrawer(GravityCompat.START);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.feature_sheet_menu, menu);
  }

  @Override
  public void onStart() {
    super.onStart();

    if (viewModel.shouldShowProjectSelectorOnStart()) {
      showProjectSelector();
    }

    viewModel.init();
  }

  private void showProjectSelector() {
    ProjectSelectorDialogFragment.show(getFragmentManager());
  }

  // TODO: Move to OfflineAreasFragment
  // private void showBasemapSelector() {
  //  viewModel.showBasemapSelector();
  // }

  private void showOfflineAreas() {
    viewModel.showOfflineAreas();
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

  private void onActiveProjectChange(Loadable<Project> project) {
    switch (project.getState()) {
      case NOT_LOADED:
      case LOADED:
        dismissLoadingDialog();
        break;
      case LOADING:
        showProjectLoadingDialog();
        break;
      case NOT_FOUND:
      case ERROR:
        project.error().ifPresent(this::onActivateProjectFailure);
        break;
    }
  }

  private void onShowAddFeatureDialogRequest(Point location) {
    if (!Loadable.getData(viewModel.getActiveProject()).isPresent()) {
      Log.e(TAG, "Attempting to add feature while no project loaded");
      return;
    }
    // TODO: Pause location updates while dialog is open.
    // TODO: Show spinner?
    showFeatureDialogRequests.onNext(new Object());
  }

  private void onFeatureSheetStateChange(FeatureSheetState state) {
    switch (state.getVisibility()) {
      case VISIBLE:
        Feature feature = state.getFeature();
        toolbar.setTitle(feature.getTitle());
        toolbar.setSubtitle(feature.getSubtitle());
        showBottomSheet();
        break;
      case HIDDEN:
        hideBottomSheet();
        break;
    }
  }

  private void showBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
  }

  private void hideBottomSheet() {
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
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
      case R.id.nav_offline_areas:
        showOfflineAreas();
        closeDrawer();
        break;
      case R.id.nav_sign_out:
        authenticationManager.signOut();
        break;
    }
    return false;
  }

  private void onActivateProjectFailure(Throwable throwable) {
    Log.e(TAG, "Error activating project", throwable);
    dismissLoadingDialog();
    EphemeralPopups.showError(getContext(), R.string.project_load_error);
    showProjectSelector();
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
