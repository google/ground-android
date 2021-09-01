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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
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
import com.google.android.gnd.databinding.NavDrawerHeaderBinding;
import com.google.android.gnd.model.Project;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.FeatureType;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.feature.Point;
import com.google.android.gnd.model.form.Form;
import com.google.android.gnd.model.layer.Layer;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.home.featureselector.FeatureSelectorFragment;
import com.google.android.gnd.ui.home.featureselector.FeatureSelectorViewModel;
import com.google.android.gnd.ui.home.mapcontainer.FeatureDataTypeSelectorDialogFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.Mode;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingInfoDialogFragment;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel.PolygonDrawing;
import com.google.android.gnd.ui.projectselector.ProjectSelectorDialogFragment;
import com.google.android.gnd.ui.projectselector.ProjectSelectorViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener;
import com.google.common.collect.ImmutableList;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java8.util.Optional;
import javax.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
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
  @Inject FeatureSelectorFragment featureSelectorDialogFragment;
  MapContainerViewModel mapContainerViewModel;
  PolygonDrawingViewModel polygonDrawingViewModel;

  @Nullable private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private ProjectSelectorDialogFragment projectSelectorDialogFragment;
  @Nullable private FeatureDataTypeSelectorDialogFragment featureDataTypeSelectorDialogFragment;
  @Nullable private PolygonDrawingInfoDialogFragment polygonDrawingInfoDialogFragment;
  private ProjectSelectorViewModel projectSelectorViewModel;
  private FeatureSelectorViewModel featureSelectorViewModel;
  private List<Project> projects = Collections.emptyList();
  private HomeScreenFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    projectSelectorDialogFragment = new ProjectSelectorDialogFragment();

    getViewModel(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    polygonDrawingViewModel = getViewModel(PolygonDrawingViewModel.class);
    projectSelectorViewModel = getViewModel(ProjectSelectorViewModel.class);
    featureSelectorViewModel = getViewModel(FeatureSelectorViewModel.class);

    viewModel = getViewModel(HomeScreenViewModel.class);
    viewModel.getProjectLoadingState().observe(this, this::onActiveProjectChange);
    viewModel.getBottomSheetState().observe(this, this::onBottomSheetStateChange);
    viewModel
        .getShowFeatureSelectorRequests()
        .as(autoDisposable(this))
        .subscribe(this::showFeatureSelector);
    viewModel.getOpenDrawerRequests().as(autoDisposable(this)).subscribe(__ -> openDrawer());
    viewModel
        .getAddFeatureResults()
        .observeOn(schedulers.ui())
        .as(autoDisposable(this))
        .subscribe(this::onFeatureAdded);
    viewModel.getUpdateFeatureResults().as(autoDisposable(this)).subscribe(this::onFeatureUpdated);
    viewModel.getDeleteFeatureResults().as(autoDisposable(this)).subscribe(this::onFeatureDeleted);
    viewModel.getErrors().as(autoDisposable(this)).subscribe(this::onError);
    polygonDrawingViewModel.getDrawingCompleted()
        .as(autoDisposable(this))
        .subscribe(__ -> viewModel
            .addPolygonFeature(polygonDrawingViewModel
                .getPolygonFeature().getValue()));
    featureSelectorViewModel
        .getFeatureClicks()
        .as(autoDisposable(this))
        .subscribe(viewModel::onFeatureSelected);
    mapContainerViewModel
        .getAddFeatureButtonClicks()
        .as(autoDisposable(this))
        .subscribe(viewModel::onAddFeatureButtonClick);
    viewModel
        .getShowAddFeatureDialogRequests()
        .as(autoDisposable(this))
        .subscribe(this::showAddFeatureDialog);
  }

  private void showAddFeatureDialog(Pair<ImmutableList<Layer>, Point> args) {
    ImmutableList<Layer> layers = args.first;
    Point point = args.second;
    addFeatureDialogFragment.show(
        layers,
        getChildFragmentManager(),
        layer -> {
          if (layer.getContributorsCanAdd().contains(FeatureType.POINT)
              && layer.getContributorsCanAdd().contains(FeatureType.POLYGON)) {
            showFeatureTypeDialog(layer, point);
          } else {
            viewModel.addFeature(layer, point);
          }
        });
  }

  private void showFeatureSelector(ImmutableList<Feature> features) {
    featureSelectorViewModel.setFeatures(features);
    if (!featureSelectorDialogFragment.isVisible()) {
      featureSelectorDialogFragment.show(
          getFragmentManager(), FeatureSelectorFragment.class.getSimpleName());
    }
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

    updateNavHeader();
    setUpBottomSheetBehavior();
  }

  private void updateNavHeader() {
    View navHeader = binding.navView.getHeaderView(0);
    NavDrawerHeaderBinding headerBinding = NavDrawerHeaderBinding.bind(navHeader);
    headerBinding.setUser(authenticationManager.getCurrentUser());
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
    FrameLayout bottomSheetHeader = binding.getRoot().findViewById(R.id.bottom_sheet_header);
    if (bottomSheetBehavior == null || bottomSheetHeader == null) {
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
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    BottomSheetState state = viewModel.getBottomSheetState().getValue();
    if (state == null) {
      Timber.e("BottomSheetState is null");
      return false;
    }

    if (item.getItemId() == R.id.move_feature_menu_item) {
      hideBottomSheet();
      mapContainerFragment.setRepositionMode(state.getFeature());
    } else if (item.getItemId() == R.id.delete_feature_menu_item) {
      hideBottomSheet();
      Optional<Feature> featureToDelete = state.getFeature();
      if (featureToDelete.isPresent()) {
        viewModel.deleteFeature(featureToDelete.get());
      } else {
        Timber.e("Attempted to delete non-existent feature");
      }
    } else if (item.getItemId() == R.id.feature_properties_menu_item) {
      showFeatureProperties();
    } else {
      return false;
    }

    return true;
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

    if (featureDataTypeSelectorDialogFragment != null
        && featureDataTypeSelectorDialogFragment.isVisible()) {
      featureDataTypeSelectorDialogFragment.dismiss();
    }

    if (polygonDrawingInfoDialogFragment != null && polygonDrawingInfoDialogFragment.isVisible()) {
      polygonDrawingInfoDialogFragment.dismiss();
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

  private void showFeatureTypeDialog(Layer layer, Point point) {
    featureDataTypeSelectorDialogFragment =
        new FeatureDataTypeSelectorDialogFragment(
            featureType -> {
              if (featureType == 0) {
                viewModel.addFeature(layer, point);
              } else {
                showPolygonInfoDialog(layer);
              }
            });
    featureDataTypeSelectorDialogFragment.show(
        getChildFragmentManager(), FeatureDataTypeSelectorDialogFragment.class.getSimpleName());
  }

  private void showPolygonInfoDialog(Layer layer) {
    polygonDrawingInfoDialogFragment =
        new PolygonDrawingInfoDialogFragment(
            () ->
                viewModel
                    .getActiveProject()
                    .ifPresentOrElse(
                        project -> {
                          polygonDrawingViewModel.setSelectedProject(Optional.of(project));
                          polygonDrawingViewModel.setSelectedLayer(Optional.of(layer));
                          polygonDrawingViewModel.updateDrawingState(PolygonDrawing.STARTED);
                          mapContainerViewModel.setViewMode(Mode.DRAW_POLYGON);
                        },
                        () -> {
                          throw new IllegalStateException("Empty project");
                        }));
    polygonDrawingInfoDialogFragment.show(
        getChildFragmentManager(), PolygonDrawingInfoDialogFragment.class.getName());
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
    } else if (item.getItemId() == R.id.nav_join_project) {
      showProjectSelector();
    } else if (item.getItemId() == R.id.sync_status) {
      viewModel.showSyncStatus();
    } else if (item.getItemId() == R.id.nav_offline_areas) {
      showOfflineAreas();
    } else if (item.getItemId() == R.id.nav_settings) {
      viewModel.showSettings();
    } else if (item.getItemId() == R.id.nav_sign_out) {
      authenticationManager.signOut();
    }
    closeDrawer();
    return true;
  }

  private void onActivateProjectFailure(Throwable throwable) {
    Timber.e(RxJava2Debug.getEnhancedStackTrace(throwable), "Error activating project");
    dismissLoadingDialog();
    popups.showError(R.string.project_load_error);
    showProjectSelector();
  }

  private void showFeatureProperties() {
    // TODO(#841): Move business logic into view model.
    BottomSheetState state = viewModel.getBottomSheetState().getValue();
    if (state == null) {
      Timber.e("BottomSheetState is null");
      return;
    }
    if (state.getFeature().isEmpty()) {
      Timber.e("No feature selected");
      return;
    }
    Feature feature = state.getFeature().get();
    List<String> items = new ArrayList<>();
    // TODO(#843): Let properties apply to other feature types as well.
    if (feature instanceof GeoJsonFeature) {
      items = getFeatureProperties((GeoJsonFeature) feature);
    }
    if (items.isEmpty()) {
      items.add("No properties defined for this feature");
    }
    new AlertDialog.Builder(requireContext())
        .setCancelable(true)
        .setTitle(R.string.feature_properties)
        // TODO(#842): Use custom view to format feature properties as table.
        .setItems(items.toArray(new String[] {}), (a, b) -> {})
        .setPositiveButton(R.string.close_feature_properties, (a, b) -> {})
        .create()
        .show();
  }

  private ImmutableList<String> getFeatureProperties(GeoJsonFeature feature) {
    String jsonString = feature.getGeoJsonString();
    try {
      JSONObject jsonObject = new JSONObject(jsonString);
      JSONObject properties = jsonObject.optJSONObject("properties");
      if (properties == null) {
        return ImmutableList.of();
      }
      ImmutableList.Builder items = new ImmutableList.Builder();
      Iterator<String> keyIter = properties.keys();
      while (keyIter.hasNext()) {
        String key = keyIter.next();
        Object value = properties.opt(key);
        // TODO(#842): Use custom view to format feature properties as table.
        items.add(key + ": " + value);
      }
      return items.build();
    } catch (JSONException e) {
      Timber.d("Encountered invalid feature GeoJSON in feature %s", feature.getId());
      return ImmutableList.of();
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
