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
package com.google.android.ground.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.fragment.findNavController
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.coroutines.MainDispatcher
import com.google.android.ground.databinding.BasemapLayoutBinding
import com.google.android.ground.databinding.MenuButtonBinding
import com.google.android.ground.model.locationofinterest.LOI_NAME_PROPERTY
import com.google.android.ground.proto.Survey.DataSharingTerms
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.home.DataSharingTermsDialog
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointRender
import com.google.android.ground.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.util.createComposeView
import com.google.android.ground.util.renderComposableDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
class HomeScreenMapContainerFragment : AbstractMapContainerFragment() {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  @Inject lateinit var submissionRepository: SubmissionRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope

  private lateinit var binding: BasemapLayoutBinding
  private lateinit var menuBinding: MenuButtonBinding

  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var infoPopup: EphemeralPopups.InfoPopup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
    mapContainerViewModel.uiEventsFlow.launchWhenStartedAndCollect { handleUiEvent(it) }
  }

  private fun handleUiEvent(uiEvent: HomeScreenMapContainerEvent) {
    when (uiEvent) {
      is HomeScreenMapContainerEvent.ShowDataSharingTermsDialog -> {
        showDataSharingTermsDialog(uiEvent.data, uiEvent.dataSharingTerms)
      }
      is HomeScreenMapContainerEvent.NavigateToDataCollectionFragment -> {
        navigateToDataCollectionFragment(uiEvent.data)
      }
      is HomeScreenMapContainerEvent.ShowErrorToast -> {
        ephemeralPopups.ErrorPopup().show(getString(uiEvent.messageResId))
      }
    }
  }

  private fun showDataSharingTermsDialog(
    cardUiData: DataCollectionEntryPointData,
    dataSharingTerms: DataSharingTerms,
  ) {
    renderComposableDialog {
      DataSharingTermsDialog(dataSharingTerms) {
        mapContainerViewModel.grantDataSharingConsent()
        navigateToDataCollectionFragment(cardUiData)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    binding = BasemapLayoutBinding.inflate(inflater, container, false)
    binding.fragment = this
    binding.viewModel = mapContainerViewModel
    binding.lifecycleOwner = this
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupMenuFab()
    binding.bottomContainer.addView(createComposeView { JobMapScreen() })
    binding.bottomContainer.bringToFront()
    showDataCollectionHint()
  }

  @Composable
  fun JobMapScreen() {
    val state by mapContainerViewModel.dataCollectionEntryPointState

    DataCollectionEntryPointRender(
      state = state,
      onEvent = { mapContainerViewModel.handleEvent(it) },
      onJobSelectionModalShown = { showMapOverlayButtons(false) },
      onJobSelectionModalDismissed = { showMapOverlayButtons(true) },
    )
  }

  private fun showMapOverlayButtons(show: Boolean) {
    if (show) {
      binding.mapTypeBtn.show()
      binding.locationLockBtn.show()
      menuBinding.hamburgerBtn.show()
    } else {
      binding.mapTypeBtn.hide()
      binding.locationLockBtn.hide()
      menuBinding.hamburgerBtn.hide()
    }
  }

  /**
   * Displays a popup hint informing users how to begin collecting data, based on the properties of
   * the active survey and zoomed in state.
   *
   * This method should only be called after view creation and should only trigger once per view
   * create.
   */
  private fun showDataCollectionHint() {
    if (!this::mapContainerViewModel.isInitialized) {
      return Timber.w("showDataCollectionHint() called before mapContainerViewModel initialized")
    }
    if (!this::binding.isInitialized) {
      return Timber.w("showDataCollectionHint() called before binding initialized")
    }
    // Combine the survey properties and the current zoomed-out state to determine which popup to
    // show, or not.
    mapContainerViewModel.surveyUpdateFlow
      .combine(mapContainerViewModel.isZoomedInFlow) { surveyProperties, isZoomedIn ->
        // Negated since we only want to show certain popups when the user is zoomed out.
        Pair(surveyProperties, !isZoomedIn)
      }
      .launchWhenStartedAndCollectFirst {
        val (surveyProperties, isZoomedOut) = it
        when {
          surveyProperties.noLois && !surveyProperties.addLoiPermitted ->
            R.string.read_only_data_collection_hint
          isZoomedOut && surveyProperties.addLoiPermitted -> R.string.suggest_data_collection_hint
          isZoomedOut -> R.string.predefined_data_collection_hint
          else -> null
        }?.let { message ->
          infoPopup =
            ephemeralPopups.InfoPopup(
              binding.bottomContainer,
              message,
              EphemeralPopups.PopupDuration.LONG,
            )
          infoPopup.show()
        }
      }
  }

  private fun setupMenuFab() {
    val mapOverlay = binding.overlay
    menuBinding = MenuButtonBinding.inflate(layoutInflater, mapOverlay, true)
    menuBinding.homeScreenViewModel = homeScreenViewModel
    menuBinding.lifecycleOwner = this
  }

  private fun navigateToDataCollectionFragment(cardUiData: DataCollectionEntryPointData) {
    when (cardUiData) {
      is SelectedLoiSheetData ->
        findNavController()
          .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
              cardUiData.loi.id,
              cardUiData.loi.properties[LOI_NAME_PROPERTY] as? String?,
              cardUiData.loi.job.id,
              false,
              null,
              "",
            )
          )
      is AdHocDataCollectionButtonData ->
        findNavController()
          .navigate(
            HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
              null,
              null,
              cardUiData.job.id,
              false,
              null,
              "",
            )
          )
    }
  }

  override fun onMapReady(map: MapFragment) {
    mapContainerViewModel.mapLoiFeatures.launchWhenStartedAndCollect { map.setFeatures(it) }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapContainerViewModel
}
