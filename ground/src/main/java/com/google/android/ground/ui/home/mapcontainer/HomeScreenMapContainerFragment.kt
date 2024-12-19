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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
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
import com.google.android.ground.ui.home.mapcontainer.jobs.JobMapAdapter
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
class HomeScreenMapContainerFragment : AbstractMapContainerFragment() {

  @Inject lateinit var ephemeralPopups: EphemeralPopups
  @Inject lateinit var submissionRepository: SubmissionRepository
  @Inject lateinit var userRepository: UserRepository
  @Inject @IoDispatcher lateinit var ioDispatcher: CoroutineDispatcher
  @Inject @MainDispatcher lateinit var mainDispatcher: CoroutineDispatcher
  @Inject @ApplicationScope lateinit var externalScope: CoroutineScope

  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var binding: BasemapLayoutBinding
  private lateinit var adapter: JobMapAdapter
  private lateinit var infoPopup: EphemeralPopups.InfoPopup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    adapter = JobMapAdapter { loi -> submissionRepository.getTotalSubmissionCount(loi) }

    launchWhenStarted {
      val canUserSubmitData = userRepository.canUserSubmitData()

      // Handle collect button clicks
      adapter.setCollectDataListener { mapUiData ->
        val job =
          lifecycleScope.launch {
            mapContainerViewModel.activeSurveyDataSharingTermsFlow.cancellable().collectLatest {
              hasDataSharingTerms ->
              onCollectData(
                canUserSubmitData,
                hasValidTasks(mapUiData),
                hasDataSharingTerms,
                mapUiData,
              )
            }
          }
        job.cancel()
      }

      // Bind data for cards
      mapContainerViewModel.getMapCardUiData().launchWhenStartedAndCollect { (loiCard, jobCards) ->
        runBlocking { adapter.updateData(canUserSubmitData, loiCard, jobCards) }
      }
    }

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
  }

  private fun hasValidTasks(cardUiData: DataCollectionEntryPointData) =
    when (cardUiData) {
      // LOI tasks are filtered out of the tasks list for pre-defined tasks.
      is DataCollectionEntryPointData.SelectedLoiSheetData -> cardUiData.loi.job.tasks.values.count { !it.isAddLoiTask } > 0
      is DataCollectionEntryPointData.AdHocDataCollectionButtonData -> cardUiData.job.tasks.values.isNotEmpty()
    }

  private fun renderDataSharingTermsDialog(
    cardUiData: DataCollectionEntryPointData,
    dataSharingTerms: DataSharingTerms,
  ) =
    ComposeView(requireContext()).apply {
      setContent {
        val showDataSharingTermsDialog = remember { mutableStateOf(true) }
        when {
          showDataSharingTermsDialog.value -> {
            AppTheme {
              DataSharingTermsDialog(showDataSharingTermsDialog, dataSharingTerms) {
                val job =
                  lifecycleScope.launch { mapContainerViewModel.updateDataSharingConsent(true) }
                job.cancel()
                navigateToDataCollectionFragment(cardUiData)
              }
            }
          }
        }
      }
    }

  /** Invoked when user clicks on the map cards to collect data. */
  private fun onCollectData(
    canUserSubmitData: Boolean,
    hasTasks: Boolean,
    hasDataSharingTerms: DataSharingTerms?,
    cardUiData: DataCollectionEntryPointData,
  ) {
    if (!canUserSubmitData) {
      // Skip data collection screen if the user can't submit any data
      // TODO(#1667): Revisit UX for displaying view only mode
      ephemeralPopups.ErrorPopup().show(getString(R.string.collect_data_viewer_error))
      return
    }
    if (!hasTasks) {
      // NOTE(#2539): The DataCollectionFragment will crash if there are no tasks.
      ephemeralPopups.ErrorPopup().show(getString(R.string.no_tasks_error))
      return
    }
    if (hasDataSharingTerms != null) {
      if (
        hasDataSharingTerms.type == DataSharingTerms.Type.CUSTOM &&
          hasDataSharingTerms.customText.isBlank()
      ) {
        ephemeralPopups.ErrorPopup().show(getString(R.string.invalid_data_sharing_terms))
        return
      }
      (view as ViewGroup).addView(renderDataSharingTermsDialog(cardUiData, hasDataSharingTerms))
      return
    }
    navigateToDataCollectionFragment(cardUiData)
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
    val menuBinding = setupMenuFab()
    setupBottomLoiCards(menuBinding)
    showDataCollectionHint()
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

  private fun setupMenuFab(): MenuButtonBinding {
    val mapOverlay = binding.overlay
    val menuBinding = MenuButtonBinding.inflate(layoutInflater, mapOverlay, true)
    menuBinding.homeScreenViewModel = homeScreenViewModel
    menuBinding.lifecycleOwner = this
    return menuBinding
  }

  private fun setupBottomLoiCards(menuBinding: MenuButtonBinding) {
    adapter.basemapLayoutBinding = binding
    adapter.menuBinding = menuBinding
    adapter.render()
  }

  private fun navigateToDataCollectionFragment(cardUiData: DataCollectionEntryPointData) {
    when (cardUiData) {
      is DataCollectionEntryPointData.SelectedLoiSheetData ->
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
      is DataCollectionEntryPointData.AdHocDataCollectionButtonData ->
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

    adapter.setSelectedFeature { mapContainerViewModel.selectLocationOfInterest(it) }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapContainerViewModel
}
