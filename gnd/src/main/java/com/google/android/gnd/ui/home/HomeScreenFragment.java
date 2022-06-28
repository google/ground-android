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
import static androidx.navigation.fragment.NavHostFragment.findNavController;
import static com.google.android.gnd.rx.RxAutoDispose.autoDisposable;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenHeight;
import static com.google.android.gnd.ui.util.ViewUtil.getScreenWidth;
import static java.util.Objects.requireNonNull;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.os.Bundle;
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
import androidx.navigation.NavDestination;
import com.akaita.java.rxjava2debug.RxJava2Debug;
import com.google.android.gnd.BuildConfig;
import com.google.android.gnd.MainActivity;
import com.google.android.gnd.MainViewModel;
import com.google.android.gnd.R;
import com.google.android.gnd.databinding.HomeScreenFragBinding;
import com.google.android.gnd.databinding.NavDrawerHeaderBinding;
import com.google.android.gnd.model.Survey;
import com.google.android.gnd.model.feature.Feature;
import com.google.android.gnd.model.feature.GeoJsonFeature;
import com.google.android.gnd.model.task.Task;
import com.google.android.gnd.repository.FeatureRepository;
import com.google.android.gnd.rx.Loadable;
import com.google.android.gnd.rx.Schedulers;
import com.google.android.gnd.system.auth.AuthenticationManager;
import com.google.android.gnd.ui.common.AbstractFragment;
import com.google.android.gnd.ui.common.BackPressListener;
import com.google.android.gnd.ui.common.EphemeralPopups;
import com.google.android.gnd.ui.common.FeatureHelper;
import com.google.android.gnd.ui.common.Navigator;
import com.google.android.gnd.ui.common.ProgressDialogs;
import com.google.android.gnd.ui.home.featureselector.FeatureSelectorViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerFragment;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel;
import com.google.android.gnd.ui.home.mapcontainer.MapContainerViewModel.Mode;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel;
import com.google.android.gnd.ui.home.mapcontainer.PolygonDrawingViewModel.PolygonDrawingState;
import com.google.android.gnd.ui.surveyselector.SurveySelectorViewModel;
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
 * submission and edit submission) at runtime.
 */
@AndroidEntryPoint
public class HomeScreenFragment extends AbstractFragment
    implements BackPressListener, OnNavigationItemSelectedListener, OnGlobalLayoutListener {

  // TODO: It's not obvious which feature are in HomeScreen vs MapContainer; make this more
  // intuitive.
  private static final float COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f;

  @Inject AuthenticationManager authenticationManager;
  @Inject Schedulers schedulers;
  @Inject Navigator navigator;
  @Inject EphemeralPopups popups;
  @Inject FeatureHelper featureHelper;
  @Inject FeatureRepository featureRepository;
  MapContainerViewModel mapContainerViewModel;
  PolygonDrawingViewModel polygonDrawingViewModel;

  @Nullable private ProgressDialog progressDialog;
  private HomeScreenViewModel viewModel;
  private MapContainerFragment mapContainerFragment;
  private BottomSheetBehavior<View> bottomSheetBehavior;
  private SurveySelectorViewModel surveySelectorViewModel;
  private FeatureSelectorViewModel featureSelectorViewModel;
  private List<Survey> surveys = Collections.emptyList();
  private HomeScreenFragBinding binding;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getViewModel(MainViewModel.class).getWindowInsets().observe(this, this::onApplyWindowInsets);

    mapContainerViewModel = getViewModel(MapContainerViewModel.class);
    polygonDrawingViewModel = getViewModel(PolygonDrawingViewModel.class);
    surveySelectorViewModel = getViewModel(SurveySelectorViewModel.class);
    featureSelectorViewModel = getViewModel(FeatureSelectorViewModel.class);

    viewModel = getViewModel(HomeScreenViewModel.class);
    viewModel.getSurveyLoadingState().observe(this, this::onActiveSurveyChange);
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
    polygonDrawingViewModel
        .getDrawingState()
        .distinctUntilChanged()
        .as(autoDisposable(this))
        .subscribe(this::onPolygonDrawingStateUpdated);
    featureSelectorViewModel
        .getFeatureClicks()
        .as(autoDisposable(this))
        .subscribe(viewModel::onFeatureSelected);
  }

  private void onPolygonDrawingStateUpdated(PolygonDrawingState state) {
    Timber.v("PolygonDrawing state : %s", state);
    if (state.isInProgress()) {
      mapContainerViewModel.setMode(Mode.DRAW_POLYGON);
    } else {
      mapContainerViewModel.setMode(Mode.DEFAULT);
      if (state.isCompleted()) {
        viewModel.addPolygonFeature(requireNonNull(state.getUnsavedPolygonFeature()));
      }
    }
  }

  private void showFeatureSelector(ImmutableList<Feature> features) {
    featureSelectorViewModel.setFeatures(features);
    navigator.navigate(
        HomeScreenFragmentDirections.actionHomeScreenFragmentToFeatureSelectorFragment());
  }

  private void onFeatureAdded(Feature feature) {
    feature.getJob().getTask().ifPresent(form -> addNewSubmission(feature, form));
  }

  private void addNewSubmission(Feature feature, Task task) {
    String surveyId = feature.getSurvey().getId();
    String featureId = feature.getId();
    String taskId = task.getId();
    navigator.navigate(HomeScreenFragmentDirections.addSubmission(surveyId, featureId, taskId));
  }

  /** This is only possible after updating the location of the feature. So, reset the UI. */
  private void onFeatureUpdated(Boolean result) {
    if (result) {
      mapContainerViewModel.setMode(Mode.DEFAULT);
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

  /** Fetches offline saved surveys and adds them to navigation drawer. */
  private void updateNavDrawer() {
    surveySelectorViewModel
        .getOfflineSurveys()
        .subscribeOn(schedulers.io())
        .observeOn(schedulers.ui())
        .as(autoDisposable(this))
        .subscribe(this::addSurveyToNavDrawer);
  }

  private MenuItem getSurveysNavItem() {
    // Below index is the order of the surveys item in nav_drawer_menu.xml
    return binding.navView.getMenu().getItem(1);
  }

  private void addSurveyToNavDrawer(List<Survey> surveys) {
    this.surveys = surveys;

    // clear last saved surveys list
    getSurveysNavItem().getSubMenu().removeGroup(R.id.group_join_survey);

    for (int index = 0; index < surveys.size(); index++) {
      getSurveysNavItem()
          .getSubMenu()
          .add(R.id.group_join_survey, Menu.NONE, index, surveys.get(index).getTitle())
          .setIcon(R.drawable.ic_menu_survey);
    }

    // Highlight active survey
    Loadable.getValue(viewModel.getSurveyLoadingState())
        .ifPresent(survey -> updateSelectedSurveyUI(getSelectedSurveyIndex(survey)));
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
      mapContainerViewModel.setMode(Mode.MOVE_POINT);
      mapContainerViewModel.setReposFeature(state.getFeature());
      Toast.makeText(getContext(), R.string.move_point_hint, Toast.LENGTH_SHORT).show();
    } else if (item.getItemId() == R.id.delete_feature_menu_item) {
      Optional<Feature> featureToDelete = state.getFeature();
      if (featureToDelete.isPresent()) {
        new Builder(requireActivity())
            .setTitle(
                getString(
                    R.string.feature_delete_confirmation_dialog_title,
                    featureHelper.getLabel(featureToDelete)))
            .setMessage(R.string.feature_delete_confirmation_dialog_message)
            .setPositiveButton(
                R.string.delete_button_label,
                (dialog, id) -> {
                  hideBottomSheet();
                  viewModel.deleteFeature(featureToDelete.get());
                })
            .setNegativeButton(
                R.string.cancel_button_label,
                (dialog, id) -> {
                  // Do nothing.
                })
            .create()
            .show();
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

    if (viewModel.shouldShowSurveySelectorOnStart()) {
      showSurveySelector();
    }

    viewModel.init();
  }

  private int getCurrentDestinationId() {
    NavDestination currentDestination = findNavController(this).getCurrentDestination();
    return currentDestination == null ? -1 : currentDestination.getId();
  }

  private void showSurveySelector() {
    if (getCurrentDestinationId() != R.id.surveySelectorDialogFragment) {
      navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToProjectSelectorDialogFragment());
    }
  }

  private void showDataCollection() {
    navigator.navigate(
        HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment()
    );
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

  private void onActiveSurveyChange(Loadable<Survey> loadable) {
    switch (loadable.getState()) {
      case NOT_LOADED:
        dismissLoadingDialog();
        break;
      case LOADED:
        dismissLoadingDialog();
        updateNavDrawer();
        break;
      case LOADING:
        showSurveyLoadingDialog();
        break;
      case NOT_FOUND:
      case ERROR:
        loadable.error().ifPresent(this::onActivateSurveyFailure);
        break;
      default:
        Timber.e("Unhandled case: %s", loadable.getState());
        break;
    }
  }

  private void updateSelectedSurveyUI(int selectedIndex) {
    SubMenu subMenu = getSurveysNavItem().getSubMenu();
    for (int i = 0; i < surveys.size(); i++) {
      MenuItem menuItem = subMenu.getItem(i);
      menuItem.setChecked(i == selectedIndex);
    }
  }

  private int getSelectedSurveyIndex(Survey activeSurvey) {
    for (Survey survey : surveys) {
      if (survey.getId().equals(activeSurvey.getId())) {
        return surveys.indexOf(survey);
      }
    }
    Timber.e("Selected survey not found.");
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

  private void showSurveyLoadingDialog() {
    if (progressDialog == null) {
      progressDialog =
          ProgressDialogs.modalSpinner(getContext(), R.string.survey_loading_please_wait);
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
    if (item.getGroupId() == R.id.group_join_survey) {
      Survey selectedSurvey = surveys.get(item.getOrder());
      surveySelectorViewModel.activateOfflineSurvey(selectedSurvey.getId());
    } else if (item.getItemId() == R.id.nav_join_survey) {
      showSurveySelector();
    } else if (item.getItemId() == R.id.tmp_collect_data) {
      showDataCollection();
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

  private void onActivateSurveyFailure(Throwable throwable) {
    Timber.e(RxJava2Debug.getEnhancedStackTrace(throwable), "Error activating survey");
    dismissLoadingDialog();
    popups.showError(R.string.survey_load_error);
    showSurveySelector();
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
