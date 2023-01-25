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
package com.google.android.ground.ui.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import com.akaita.java.rxjava2debug.RxJava2Debug
import com.google.android.ground.BuildConfig
import com.google.android.ground.MainViewModel
import com.google.android.ground.R
import com.google.android.ground.databinding.HomeScreenFragBinding
import com.google.android.ground.databinding.NavDrawerHeaderBinding
import com.google.android.ground.model.Survey
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Loadable.LoadState
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.rx.Schedulers
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.*
import com.google.android.ground.ui.home.locationofinterestselector.LocationOfInterestSelectorViewModel
import com.google.android.ground.ui.home.mapcontainer.HomeScreenMapContainerViewModel
import com.google.android.ground.ui.surveyselector.SurveySelectorViewModel
import com.google.android.ground.ui.util.ViewUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.common.collect.ImmutableList
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * Fragment containing the map container and location of interest sheet fragments and NavigationView
 * side drawer. This is the default view in the application, and gets swapped out for other
 * fragments (e.g., view submission and edit submission) at runtime.
 */
@AndroidEntryPoint
class HomeScreenFragment :
  AbstractFragment(),
  BackPressListener,
  NavigationView.OnNavigationItemSelectedListener,
  OnGlobalLayoutListener {

  // TODO: It's not obvious which locations of interest are in HomeScreen vs MapContainer;
  //  make this more intuitive.

  @Inject lateinit var authenticationManager: AuthenticationManager
  @Inject lateinit var locationOfInterestHelper: LocationOfInterestHelper
  @Inject lateinit var locationOfInterestRepository: LocationOfInterestRepository
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var popups: EphemeralPopups
  @Inject lateinit var schedulers: Schedulers

  private lateinit var binding: HomeScreenFragBinding
  private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var locationOfInterestSelectorViewModel: LocationOfInterestSelectorViewModel
  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var surveySelectorViewModel: SurveySelectorViewModel

  private var progressDialog: ProgressDialog? = null
  private var surveys = emptyList<Survey>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    getViewModel(MainViewModel::class.java).windowInsets.observe(this) { insets: WindowInsetsCompat
      ->
      onApplyWindowInsets(insets)
    }
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    surveySelectorViewModel = getViewModel(SurveySelectorViewModel::class.java)
    locationOfInterestSelectorViewModel =
      getViewModel(LocationOfInterestSelectorViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    homeScreenViewModel.surveyLoadingState.observe(this) { onActiveSurveyChange(it) }
    homeScreenViewModel.bottomSheetState.observe(this) { onBottomSheetStateChange(it) }
    homeScreenViewModel.showLocationOfInterestSelectorRequests
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { showLocationOfInterestSelector(it) }
    homeScreenViewModel.openDrawerRequests.`as`(RxAutoDispose.autoDisposable(this)).subscribe {
      openDrawer()
    }
    locationOfInterestSelectorViewModel.locationOfInterestClicks
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { homeScreenViewModel.onLocationOfInterestSelected(it) }
  }

  private fun showLocationOfInterestSelector(
    locationsOfInterest: ImmutableList<LocationOfInterest>
  ) {
    locationOfInterestSelectorViewModel.locationsOfInterest = locationsOfInterest
    navigator.navigate(
      HomeScreenFragmentDirections.actionHomeScreenFragmentToLocationOfInterestSelectorFragment()
    )
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = HomeScreenFragBinding.inflate(inflater, container, false)
    binding.locationOfInterestDetailsChrome.viewModel = homeScreenViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding.versionText.text = String.format(getString(R.string.build), BuildConfig.VERSION_NAME)
    // Ensure nav drawer cannot be swiped out, which would conflict with map pan gestures.
    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    binding.navView.setNavigationItemSelectedListener(this)
    requireView().viewTreeObserver.addOnGlobalLayoutListener(this)
    updateNavHeader()
    setUpBottomSheetBehavior()
  }

  private fun updateNavHeader() {
    val navHeader = binding.navView.getHeaderView(0)
    val headerBinding = NavDrawerHeaderBinding.bind(navHeader)
    headerBinding.user = authenticationManager.currentUser
  }

  /** Fetches offline saved surveys and adds them to navigation drawer. */
  private fun updateNavDrawer(activeSurvey: Survey) {
    surveySelectorViewModel.offlineSurveys
      .subscribeOn(schedulers.io())
      .observeOn(schedulers.ui())
      .`as`(RxAutoDispose.autoDisposable(this))
      .subscribe { surveys: ImmutableList<Survey> -> addSurveyToNavDrawer(surveys, activeSurvey) }
  }

  // Below index is the order of the surveys item in nav_drawer_menu.xml
  private val surveysNavItem: MenuItem
    get() = // Below index is the order of the surveys item in nav_drawer_menu.xml
    binding.navView.menu.getItem(1)

  private fun addSurveyToNavDrawer(surveys: List<Survey>, activeSurvey: Survey) {
    this.surveys = surveys

    // clear last saved surveys list
    surveysNavItem.subMenu?.removeGroup(R.id.group_join_survey)
    for (index in surveys.indices) {
      surveysNavItem.subMenu
        ?.add(R.id.group_join_survey, Menu.NONE, index, surveys[index].title)
        ?.setIcon(R.drawable.ic_menu_survey)
    }

    // Highlight active survey
    updateSelectedSurveyUI(getSelectedSurveyIndex(activeSurvey))
  }

  override fun onGlobalLayout() {
    if (binding.root.findViewById<FrameLayout>(R.id.bottom_sheet_header) == null) return

    bottomSheetBehavior.isFitToContents = false

    // When the bottom sheet is expanded, the bottom edge of the header needs to be aligned with
    // the bottom edge of the toolbar (the header slides up under it).
    val metrics = BottomSheetMetrics(binding.bottomSheetLayout)
    bottomSheetBehavior.expandedOffset = metrics.expandedOffset
    requireView().viewTreeObserver.removeOnGlobalLayoutListener(this)
  }

  private fun setUpBottomSheetBehavior() {
    bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheetLayout)
    bottomSheetBehavior.isHideable = true
    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
    bottomSheetBehavior.setBottomSheetCallback(BottomSheetCallback())
  }

  @Deprecated("Deprecated in Java")
  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setHasOptionsMenu(true)
    getMainActivity().setActionBar(binding.locationOfInterestDetailsChrome.toolbar, false)
  }

  private fun openDrawer() {
    binding.drawerLayout.openDrawer(GravityCompat.START)
  }

  private fun closeDrawer() {
    binding.drawerLayout.closeDrawer(GravityCompat.START)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.loi_properties_menu_item -> {
        showLocationOfInterestProperties()
      }
      else -> return false
    }
    return true
  }

  override fun onStart() {
    super.onStart()
    homeScreenViewModel.init()
  }

  private val currentDestinationId: Int
    get() {
      val currentDestination = NavHostFragment.findNavController(this).currentDestination
      return currentDestination?.id ?: -1
    }

  private fun showSurveySelector() {
    if (currentDestinationId != R.id.surveySelectorDialogFragment) {
      navigator.navigate(
        HomeScreenFragmentDirections.actionHomeScreenFragmentToProjectSelectorDialogFragment()
      )
    }
  }

  private fun showDataCollection() {
    // TODO(#1146): Replace with actual values based on the clicked Task card
    val dummySurveyId = "123"
    val dummyLocationOfInterestId = "456"
    val dummySubmissionId = "789"
    navigator.navigate(
      HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
        dummySurveyId,
        dummyLocationOfInterestId,
        dummySubmissionId
      )
    )
  }

  private fun showOfflineAreas() {
    homeScreenViewModel.showOfflineAreas()
  }

  private fun onApplyWindowInsets(insets: WindowInsetsCompat) {
    binding.locationOfInterestDetailsChrome.toolbarWrapper.setPadding(
      0,
      insets.systemWindowInsetTop,
      0,
      0
    )
    binding.locationOfInterestDetailsChrome.bottomSheetBottomInsetScrim.minimumHeight =
      insets.systemWindowInsetBottom
    updateNavViewInsets(insets)
    updateBottomSheetPeekHeight(insets)
  }

  private fun updateNavViewInsets(insets: WindowInsetsCompat) {
    val headerView = binding.navView.getHeaderView(0)
    headerView.setPadding(0, insets.systemWindowInsetTop, 0, 0)
  }

  private fun updateBottomSheetPeekHeight(insets: WindowInsetsCompat) {
    val width =
      (ViewUtil.getScreenWidth(requireActivity()) +
          insets.systemWindowInsetLeft +
          insets.systemWindowInsetRight)
        .toDouble()
    val height =
      (ViewUtil.getScreenHeight(requireActivity()) +
          insets.systemWindowInsetTop +
          insets.systemWindowInsetBottom)
        .toDouble()
    var mapHeight = 0.0
    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      mapHeight = width / COLLAPSED_MAP_ASPECT_RATIO
    } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      mapHeight = height / COLLAPSED_MAP_ASPECT_RATIO
    }
    val peekHeight = height - mapHeight
    bottomSheetBehavior.peekHeight = peekHeight.toInt()
  }

  private fun onActiveSurveyChange(loadable: Loadable<Survey>) {
    when (loadable.state) {
      LoadState.LOADED -> {
        dismissSurveyLoadingDialog()
        updateNavDrawer(loadable.value().get())
      }
      LoadState.LOADING -> showSurveyLoadingDialog()
      LoadState.ERROR -> loadable.error().ifPresent { onActivateSurveyFailure(it!!) }
    }
  }

  private fun updateSelectedSurveyUI(selectedIndex: Int) {
    val subMenu = surveysNavItem.subMenu
    for (i in surveys.indices) {
      val menuItem = subMenu?.getItem(i)
      menuItem?.isChecked = i == selectedIndex
    }
  }

  private fun getSelectedSurveyIndex(activeSurvey: Survey): Int {
    for (survey in surveys) {
      if (survey.id == activeSurvey.id) {
        return surveys.indexOf(survey)
      }
    }
    Timber.e("Selected survey not found.")
    return -1
  }

  private fun onBottomSheetStateChange(state: BottomSheetState) {
    when (state.visibility) {
      BottomSheetState.Visibility.VISIBLE -> showBottomSheet()
      BottomSheetState.Visibility.HIDDEN -> hideBottomSheet()
    }
  }

  private fun showBottomSheet() {
    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
  }

  private fun hideBottomSheet() {
    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
  }

  private fun showSurveyLoadingDialog() {
    if (progressDialog == null) {
      progressDialog = ProgressDialogs.modalSpinner(context, R.string.survey_loading_please_wait)
      progressDialog?.show()
    }
  }

  private fun dismissSurveyLoadingDialog() {
    if (progressDialog != null) {
      progressDialog!!.dismiss()
      progressDialog = null
    }
  }

  override fun onBack(): Boolean {
    return if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
      false
    } else {
      hideBottomSheet()
      true
    }
  }

  override fun onNavigationItemSelected(item: MenuItem): Boolean {
    if (item.groupId == R.id.group_join_survey) {
      val (id) = surveys[item.order]
      surveySelectorViewModel.activateOfflineSurvey(id)
    } else if (item.itemId == R.id.nav_join_survey) {
      showSurveySelector()
    } else if (item.itemId == R.id.tmp_collect_data) {
      showDataCollection()
    } else if (item.itemId == R.id.sync_status) {
      homeScreenViewModel.showSyncStatus()
    } else if (item.itemId == R.id.nav_offline_areas) {
      showOfflineAreas()
    } else if (item.itemId == R.id.nav_settings) {
      homeScreenViewModel.showSettings()
    } else if (item.itemId == R.id.nav_sign_out) {
      authenticationManager.signOut()
    }
    closeDrawer()
    return true
  }

  private fun onActivateSurveyFailure(throwable: Throwable) {
    Timber.e(RxJava2Debug.getEnhancedStackTrace(throwable), "Error activating survey")
    dismissSurveyLoadingDialog()
    popups.showError(R.string.survey_load_error)
    showSurveySelector()
  }

  private fun showLocationOfInterestProperties() {
    // TODO(#841): Move business logic into view model.
    val state = homeScreenViewModel.bottomSheetState.value
    if (state == null) {
      Timber.e("BottomSheetState is null")
      return
    }
    if (state.locationOfInterest == null) {
      Timber.e("No locationOfInterest selected")
      return
    }
    val items: MutableList<String> = ArrayList()
    // TODO(#843): Let properties apply to other locationOfInterest types as well.
    if (items.isEmpty()) {
      items.add("No properties defined for this locationOfInterest")
    }
    AlertDialog.Builder(requireContext())
      .setCancelable(true)
      .setTitle(
        R.string.loi_properties
      ) // TODO(#842): Use custom view to format locationOfInterest properties as table.
      .setItems(arrayOf<String>()) { _: DialogInterface?, _: Int -> }
      .setPositiveButton(R.string.close_loi_properties) { _: DialogInterface?, _: Int -> }
      .create()
      .show()
  }

  private inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      if (newState == BottomSheetBehavior.STATE_HIDDEN) {
        homeScreenViewModel.onBottomSheetHidden()
      }
    }

    override fun onSlide(bottomSheet: View, slideOffset: Float) {
      // no-op.
    }
  }

  companion object {
    private const val COLLAPSED_MAP_ASPECT_RATIO = 3.0f / 2.0f
  }
}
