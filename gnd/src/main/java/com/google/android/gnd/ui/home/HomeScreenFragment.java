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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.navigation.fragment.NavHostFragment;
import butterknife.BindView;
import butterknife.OnClick;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.repository.Resource;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BottomSheetBehavior;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.OnBackListener;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.common.TwoLineToolbar;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.vo.Form;
import com.google.android.gnd.vo.Place;
import com.google.android.gnd.vo.Point;
import com.google.android.gnd.vo.Project;
import javax.inject.Inject;

/**
 * Fragment containing the map container and place sheet fragments. This is the default view in the
 * application, and gets swapped out for other fragments (e.g., view record and edit record) at
 * runtime.
 */
public class HomeScreenFragment extends AbstractFragment implements OnBackListener {
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f;
  private static final String TAG = HomeScreenFragment.class.getSimpleName();

  @Inject AddPlaceDialogFragment addPlaceDialogFragment;

  @BindView(R.id.toolbar_wrapper)
  ViewGroup toolbarWrapper;

  @BindView(R.id.toolbar)
  TwoLineToolbar toolbar;

  @BindView(R.id.drawer_layout)
  DrawerLayout drawerLayout;

  @BindView(R.id.status_bar_scrim)
  View statusBarScrim;

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

  private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private MainViewModel mainViewModel;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    viewModel = get(HomeScreenViewModel.class);
    mainViewModel = get(MainViewModel.class);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.home_screen_frag, container, false);
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    saveChildFragment(outState, mapContainerFragment);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

    getView().getViewTreeObserver().addOnGlobalLayoutListener(this::onToolbarLayout);

    if (savedInstanceState == null) {
      mapContainerFragment = new MapContainerFragment();
      replaceFragment(R.id.map_container_fragment, mapContainerFragment);
    } else {
      mapContainerFragment = restoreChildFragment(savedInstanceState, MapContainerFragment.class);
    }

    setUpBottomSheetBehavior();
  }

  private void onToolbarLayout() {
    if (toolbarWrapper == null || bottomSheetBehavior == null || bottomSheetHeader == null) {
      return;
    }
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

    viewModel.getActiveProject().observe(this, this::onActiveProjectChange);
    viewModel.getShowAddPlaceDialogRequests().observe(this, this::onShowAddPlaceDialogRequest);
    viewModel.getPlaceSheetState().observe(this, this::onPlaceSheetStateChange);
    viewModel.getOpenDrawerRequests().observe(this, __ -> openDrawer());
    mainViewModel.getWindowInsets().observe(this, this::onApplyWindowInsets);
  }

  private void openDrawer() {
    drawerLayout.openDrawer(Gravity.START);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.place_sheet_menu, menu);
  }

  @Override
  public void onStart() {
    super.onStart();

    // TODO: Persist last selected project in local db instead of asking to select every time.
    // TODO: Trigger this from welcome flow and nav drawer instead of here.
    if (viewModel.getActiveProject().getValue() == null) {
      ProjectSelectorDialogFragment.show(getFragmentManager());
    }
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    statusBarScrim.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    toolbarWrapper.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    bottomSheetBottomInsetScrim.setMinimumHeight(insets.getSystemWindowInsetBottom());
    updateBottomSheetPeekHeight(insets);
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
        break;
    }
  }

  // TODO: Put record button and chrome into its own fragment.
  @OnClick(R.id.add_record_btn)
  void addRecord() {
    PlaceSheetState placeSheetState = viewModel.getPlaceSheetState().getValue();
    if (placeSheetState == null) {
      Log.e(TAG, "Missing placeSheetState");
      return;
    }
    Form form = viewModel.getSelectedForm().getValue();
    if (form == null) {
      Log.e(TAG, "Missing form");
      return;
    }
    Place place = placeSheetState.getPlace();
    NavHostFragment.findNavController(this)
        .navigate(
            HomeScreenFragmentDirections.addRecord(
                place.getProject().getId(), place.getId(), form.getId()));
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
        toolbar.setTitle(state.getPlace().getTitle());
        toolbar.setSubtitle(state.getPlace().getSubtitle());
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
    progressDialog =
        ProgressDialogs.modalSpinner(getContext(), R.string.project_loading_please_wait);
    progressDialog.show();
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
