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

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import com.google.android.gnd.BuildConfig;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.HomeScreenFragBinding;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.Collections;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import timber.log.Timber;

/**
 * Fragment containing the map container and feature sheet fragments and NavigationView side drawer.
 * This is the default view in the application, and gets swapped out for other fragments (e.g., view
 * observation and edit observation) at runtime.
 */
@AndroidEntryPoint
public class HomeScreenFragment extends AbstractFragment
    implements BackPressListener, OnNavigationItemSelectedListener, OnGlobalLayoutListener {
  // TODO: It's not obvious which feature are in HomeScreen vs MapContainer; make this more
  // intuitive.
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f;

  @Inject AddFeatureDialogFragment addFeatureDialogFragment;
  @Inject AuthenticationManager authenticationManager;
  @Inject Schedulers schedulers;
  @Inject Navigator navigator;
  @Inject EphemeralPopups popups;
  MapContainerViewModel mapContainerViewModel;

  @Nullable private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private ProjectSelectorDialogFragment projectSelectorDialogFragment;
  private ProjectSelectorViewModel projectSelectorViewModel;
  private List<Project> projects = Collections.emptyList();
  private HomeScreenFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    projectSelectorDialogFragment = new ProjectSelectorDialogFragment();

    getViewModel(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    projectSelectorViewModel = getViewModel(ProjectSelectorViewModel.class);

    viewModel = getViewModel(HomeScreenViewModel.class);
    viewModel.getProjectLoadingState().observe(this, this::onActiveProjectChange);
    viewModel
        .getShowAddFeatureDialogRequests()
        .observe(this, e -> e.ifUnhandled(this::onShowAddFeatureDialogRequest));
    viewModel.getBottomSheetState().observe(this, this::onBottomSheetStateChange);
    viewModel.getOpenDrawerRequests().observe(this, e -> e.ifUnhandled(this::openDrawer));
    viewModel.getAddFeatureResults().observe(this, this::onFeatureAdded);
    viewModel.getUpdateFeatureResults().observe(this, this::onFeatureUpdated);
    viewModel.getDeleteFeatureResults().observe(this, this::onFeatureDeleted);
    viewModel.getErrors().observe(this, this::onError);
  }

  private void onFeatureAdded(Feature feature) {
    feature.getLayer().getForm().ifPresent(form -> addNewObservation(feature, form));
  }

  private void addNewObservation(Feature feature, Form form) {
    String projectId = feature.getProject().getId();
    String featureId = feature.getId();
    String formId = form.getId();
    navigator.navigate(HomeScreenFragmentDirections.addObservation(projectId, featureId, formId));
  }

  /** This is only possible after updating the location of the feature. So, reset the UI. */
  private void onFeatureUpdated(Boolean result) {
    if (result) {
      mapContainerFragment.setDefaultMode();
    }
  }

  private void onFeatureDeleted(Boolean result) {
    if (result) {
      // TODO: Re-position map to default location after successful deletion.
      hideBottomSheet();
    }
  }

  /** Generic handler to display error messages to the user. */
  private void onError(Throwable throwable) {
    Timber.e(throwable);
    // Don't display the exact error message as it might not be user-readable.
    Toast.makeText(getContext(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    binding = HomeScreenFragBinding.inflate(inflater, container, false);
    binding.featureDetailsChrome.setViewModel(viewModel);
    binding.setLifecycleOwner(this);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    binding.versionText.setText(String.format(getString(R.string.build), BuildConfig.VERSION_NAME));
    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

    binding.navView.setNavigationItemSelectedListener(this);
    getView().getViewTreeObserver().addOnGlobalLayoutListener(this);

    if (savedInstanceState == null) {
      mapContainerFragment = new MapContainerFragment();
      replaceFragment(R.id.map_container_fragment, mapContainerFragment);
    } else {
      mapContainerFragment = restoreChildFragment(savedInstanceState, MapContainerFragment.class);
    }

    setUpBottomSheetBehavior();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    saveChildFragment(outState, mapContainerFragment, MapContainerFragment.class.getName());
  }

  /** Fetches offline saved projects and adds them to navigation drawer. */
  private void updateNavDrawer() {
    projectSelectorViewModel
        .getOfflineProjects()
        .subscribeOn(schedulers.io())
        .observeOn(schedulers.ui())
        .as(autoDisposable(this))
        .subscribe(this::addProjectToNavDrawer);
  }

  private MenuItem getProjectsNavItem() {
    // Below index is the order of the projects item in nav_drawer_menu.xml
    return binding.navView.getMenu().getItem(1);
  }

  private void addProjectToNavDrawer(List<Project> projects) {
    this.projects = projects;

    // clear last saved projects list
    getProjectsNavItem().getSubMenu().removeGroup(R.id.group_join_project);

    for (int index = 0; index < projects.size(); index++) {
      getProjectsNavItem()
          .getSubMenu()
          .add(R.id.group_join_project, Menu.NONE, index, projects.get(index).getTitle())
          .setIcon(R.drawable.ic_menu_project);
    }

    // Highlight active project
    Loadable.getValue(viewModel.getProjectLoadingState())
        .ifPresent(project -> updateSelectedProjectUI(getSelectedProjectIndex(project)));
  }

  @Override
  public void onGlobalLayout() {
    FrameLayout toolbarWrapper = binding.featureDetailsChrome.toolbarWrapper;
    FrameLayout bottomSheetHeader = binding.getRoot().findViewById(R.id.bottom_sheet_header);
    if (toolbarWrapper == null || bottomSheetBehavior == null || bottomSheetHeader == null) {
      return;
    }
    bottomSheetBehavior.setFitToContents(false);

    // When the bottom sheet is expanded, the bottom edge of the header needs to be aligned with
    // the bottom edge of the toolbar (the header slides up under it).
    BottomSheetMetrics metrics = new BottomSheetMetrics(binding.bottomSheetLayout);
    bottomSheetBehavior.setExpandedOffset(metrics.getExpandedOffset());

    getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
  }

  private void setUpBottomSheetBehavior() {
    bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout);
    bottomSheetBehavior.setHideable(true);
    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    bottomSheetBehavior.setBottomSheetCallback(new BottomSheetCallback());
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);

    ((MainActivity) getActivity()).setActionBar(binding.featureDetailsChrome.toolbar, false);
  }

  private void openDrawer() {
    binding.drawerLayout.openDrawer(GravityCompat.START);
  }

  private void closeDrawer() {
    binding.drawerLayout.closeDrawer(GravityCompat.START);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.feature_sheet_menu, menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    BottomSheetState state = viewModel.getBottomSheetState().getValue();
    if (state == null) {
      Timber.e("BottomSheetState is null");
      return false;
    }

    switch (item.getItemId()) {
      case R.id.move_feature_menu_item:
        hideBottomSheet();
        mapContainerFragment.setRepositionMode(state.getFeature());
        return false;
      case R.id.delete_feature_menu_item:
        hideBottomSheet();
        Optional<Feature> featureToDelete = state.getFeature();
        if (featureToDelete.isPresent()) {
          viewModel.deleteFeature(featureToDelete.get());
        } else {
          Timber.e("Attempted to delete non-existent feature");
        }
        return true;
      default:
        return false;
    }
  }

  @Override
  public void onStart() {
    super.onStart();

    if (viewModel.shouldShowProjectSelectorOnStart()) {
      showProjectSelector();
    }

    viewModel.init();
  }

  @Override
  public void onStop() {
    super.onStop();

    if (projectSelectorDialogFragment.isVisible()) {
      dismissProjectSelector();
    }
  }

  private void showProjectSelector() {
    if (!projectSelectorDialogFragment.isVisible()) {
      projectSelectorDialogFragment.show(
          getFragmentManager(), ProjectSelectorDialogFragment.class.getSimpleName());
    }
  }

  private void dismissProjectSelector() {
    projectSelectorDialogFragment.dismiss();
  }

  private void showOfflineAreas() {
    viewModel.showOfflineAreas();
  }

  private void onApplyWindowInsets(WindowInsetsCompat insets) {
    binding.featureDetailsChrome.toolbarWrapper.setPadding(
        0, insets.getSystemWindowInsetTop(), 0, 0);
    binding.featureDetailsChrome.bottomSheetBottomInsetScrim.setMinimumHeight(
        insets.getSystemWindowInsetBottom());
    updateNavViewInsets(insets);
    updateBottomSheetPeekHeight(insets);
  }

  private void updateNavViewInsets(WindowInsetsCompat insets) {
    View headerView = binding.navView.getHeaderView(0);
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
    double mapHeight = 0;
    if (getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT) {
      mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO;
    } else if (getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE) {
      mapHeight = height / COLLAPSED_MAP_ASPECT_RATIO;
    }
    double peekHeight = height - mapHeight;
    bottomSheetBehavior.setPeekHeight((int) peekHeight);
  }

  private void onActiveProjectChange(Loadable<Project> project) {
    switch (project.getState()) {
      case NOT_LOADED:
        dismissLoadingDialog();
        break;
      case LOADED:
        dismissLoadingDialog();
        updateNavDrawer();
        break;
      case LOADING:
        showProjectLoadingDialog();
        break;
      case NOT_FOUND:
      case ERROR:
        project.error().ifPresent(this::onActivateProjectFailure);
        break;
      default:
        Timber.e("Unhandled case: %s", project.getState());
        break;
    }
  }

  private void updateSelectedProjectUI(int selectedIndex) {
    SubMenu subMenu = getProjectsNavItem().getSubMenu();
    for (int i = 0; i < projects.size(); i++) {
      MenuItem menuItem = subMenu.getItem(i);
      menuItem.setChecked(i == selectedIndex);
    }
  }

  private int getSelectedProjectIndex(Project activeProject) {
    for (Project project : projects) {
      if (project.getId().equals(activeProject.getId())) {
        return projects.indexOf(project);
      }
    }
    Timber.e("Selected project not found.");
    return -1;
  }

  private void onShowAddFeatureDialogRequest(Point point) {
    Loadable.getValue(viewModel.getProjectLoadingState())
        .ifPresentOrElse(
            project -> {
              // TODO: Pause location updates while dialog is open.
              // TODO: Show spinner?
              addFeatureDialogFragment.show(
                  project.getLayers(),
                  getChildFragmentManager(),
                  (layer) -> viewModel.addFeature(project, layer, point));
            },
            () -> Timber.e("Attempting to add feature while no project loaded"));
  }

  private void onBottomSheetStateChange(BottomSheetState state) {
    switch (state.getVisibility()) {
      case VISIBLE:
        showBottomSheet();
        break;
      case HIDDEN:
        hideBottomSheet();
        break;
      default:
        Timber.e("Unhandled visibility: %s", state.getVisibility());
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
    if (item.getGroupId() == R.id.group_join_project) {
      Project selectedProject = projects.get(item.getOrder());
      projectSelectorViewModel.activateOfflineProject(selectedProject.getId());
      closeDrawer();
    } else {
      switch (item.getItemId()) {
        case R.id.nav_join_project:
          showProjectSelector();
          closeDrawer();
          break;
        case R.id.nav_offline_areas:
          showOfflineAreas();
          closeDrawer();
          break;
        case R.id.nav_settings:
          viewModel.showSettings();
          closeDrawer();
          break;
        case R.id.nav_sign_out:
          authenticationManager.signOut();
          break;
        default:
          Timber.e("Unhandled id: %s", item.getItemId());
          break;
      }
    }
    return false;
  }

  private void onActivateProjectFailure(Throwable throwable) {
    Timber.e(RxJava2Debug.getEnhancedStackTrace(throwable), "Error activating project");
    dismissLoadingDialog();
    popups.showError(R.string.project_load_error);
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
