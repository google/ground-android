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
package org.groundplatform.android.ui.home.mapcontainer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.groundplatform.android.R
import org.groundplatform.android.model.locationofinterest.LOI_NAME_PROPERTY
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.ui.common.AbstractMapContainerFragment
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.EphemeralPopups
import org.groundplatform.android.ui.home.DataSharingTermsDialog
import org.groundplatform.android.ui.home.HomeScreenFragmentDirections
import org.groundplatform.android.ui.home.HomeScreenViewModel
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentAction
import org.groundplatform.android.ui.home.mapcontainer.jobs.JobMapComponentState
import org.groundplatform.android.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import org.groundplatform.android.ui.map.MapFragment
import org.groundplatform.android.ui.theme.AppTheme
import org.groundplatform.android.usecases.datasharingterms.GetDataSharingTermsUseCase
import org.groundplatform.android.util.renderComposableDialog
import timber.log.Timber

/** Main app view, displaying the map and related controls (center cross-hairs, add button, etc). */
@AndroidEntryPoint
class HomeScreenMapContainerFragment : AbstractMapContainerFragment() {

  @Inject lateinit var ephemeralPopups: EphemeralPopups

  private lateinit var mapContainerViewModel: HomeScreenMapContainerViewModel
  private lateinit var homeScreenViewModel: HomeScreenViewModel
  private lateinit var bottomContainer: ViewGroup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
  }

  private fun hasValidTasks(cardUiData: DataCollectionEntryPointData) =
    when (cardUiData) {
      // LOI tasks are filtered out of the tasks list for pre-defined tasks.
      is SelectedLoiSheetData -> cardUiData.loi.job.tasks.values.count { !it.isAddLoiTask } > 0
      is AdHocDataCollectionButtonData -> cardUiData.job.tasks.values.isNotEmpty()
    }



  /** Invoked when user clicks on the map cards to collect data. */
  private fun onCollectData(cardUiData: DataCollectionEntryPointData) {
    if (!cardUiData.canCollectData) {
      // Skip data collection screen if the user can't submit any data
      // TODO: Revisit UX for displaying view only mode
      // Issue URL: https://github.com/google/ground-android/issues/1667
      ephemeralPopups.ErrorPopup().show(getString(R.string.collect_data_viewer_error))
      return
    }
    if (!hasValidTasks(cardUiData)) {
      // NOTE(#2539): The DataCollectionFragment will crash if there are no tasks.
      ephemeralPopups.ErrorPopup().show(getString(R.string.no_tasks_error))
      return
    }

    mapContainerViewModel.queueDataCollection(cardUiData)
  }

  /** Invoked when user clicks delete on a site. */
  private fun onDeleteSite(loiData: SelectedLoiSheetData) {
    launchWhenStarted { mapContainerViewModel.deleteLoi(loiData.loi) }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    super.onCreateView(inflater, container, savedInstanceState)
    bottomContainer =
      androidx.coordinatorlayout.widget.CoordinatorLayout(requireContext()).apply {
        id = R.id.bottom_container
      }

    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        AppTheme {
          Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
              factory = { context -> android.widget.FrameLayout(context).apply { id = R.id.map } },
              modifier = Modifier.fillMaxSize(),
              update = {
                val fragment = childFragmentManager.findFragmentById(R.id.map)
                if (fragment == null) {
                  map.attachToParent(this@HomeScreenMapContainerFragment, R.id.map) {
                    onMapAttached(it)
                  }
                }
              },
            )

            val locationLockButton by
              mapContainerViewModel.locationLockIconType.collectAsStateWithLifecycle()
            val jobMapComponentState by
              mapContainerViewModel.jobMapComponentState.collectAsStateWithLifecycle()
            val shouldShowMapActions by
              mapContainerViewModel.shouldShowMapActions.collectAsStateWithLifecycle()
            val shouldShowRecenter by
              mapContainerViewModel.shouldShowRecenterButton.collectAsStateWithLifecycle()
            val dataSharingTerms by
              mapContainerViewModel.dataSharingTerms.collectAsStateWithLifecycle()

            mapContainerViewModel.navigateToDataCollectionFragment.launchWhenStartedAndCollect {
              navigateToDataCollectionFragment(it)
            }

            HomeScreenMapContainerScreen(
              locationLockButtonType = locationLockButton,
              shouldShowMapActions = shouldShowMapActions,
              shouldShowRecenter = shouldShowRecenter,
              jobComponentState = jobMapComponentState,
              dataSharingTerms = dataSharingTerms,
              onBaseMapAction = { handleMapAction(it) },
              onJobComponentAction = {
                handleJobMapComponentAction(
                  jobMapComponentState = jobMapComponentState,
                  action = it,
                )
              },
              onTermsConsentGiven = { mapContainerViewModel.onTermsConsentGiven() },
              onTermsConsentDismissed = { mapContainerViewModel.onTermsConsentDismissed() },
            )

            AndroidView(factory = { bottomContainer }, modifier = Modifier.fillMaxSize())
          }
        }
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    // AbstractMapContainerFragment.onViewCreated calls map.attachToParent, which fails here
    // because the R.id.map view hasn't been added by AndroidView yet (it happens in composition).
    // So we skip super.onViewCreated and handle map attachment in the AndroidView update block.
    // super.onViewCreated(view, savedInstanceState)

    bottomContainer.bringToFront()
    showDataCollectionHint()

    // LOIs associated with the survey have been synced to the local db by this point. We can
    // enable location lock if no LOIs exist or a previous camera position doesn't exist.
    launchWhenStarted { mapContainerViewModel.maybeEnableLocationLock() }
  }

  private fun handleMapAction(action: BaseMapAction) {
    when (action) {
      BaseMapAction.OnLocationLockClicked -> mapContainerViewModel.onLocationLockClick()
      BaseMapAction.OnMapTypeClicked -> showMapTypeSelectorDialog()
      BaseMapAction.OnOpenNavDrawerClicked -> homeScreenViewModel.openNavDrawer()
    }
  }

  private fun handleJobMapComponentAction(
    jobMapComponentState: JobMapComponentState,
    action: JobMapComponentAction,
  ) {
    when (action) {
      is JobMapComponentAction.OnAddDataClicked -> onCollectData(action.selectedLoi)
      is JobMapComponentAction.OnDeleteSiteClicked -> onDeleteSite(action.selectedLoi)
      JobMapComponentAction.OnJobCardDismissed ->
        mapContainerViewModel.selectLocationOfInterest(null)
      is JobMapComponentAction.OnJobSelected ->
        jobMapComponentState.adHocDataCollectionButtonData
          .firstOrNull { it.job == action.job }
          ?.let { onCollectData(it) }
      is JobMapComponentAction.OnJobSelectionModalVisibilityChanged ->
        mapContainerViewModel.onJobSelectionModalVisibilityChanged(action.isShown)
    }
  }

  /**
   * Displays a popup hint informing users how to begin collecting data.
   *
   * This method should only be called after view creation and should only trigger once per view
   * create.
   */
  private fun showDataCollectionHint() {
    if (!this::mapContainerViewModel.isInitialized) {
      return Timber.w("showDataCollectionHint() called before mapContainerViewModel initialized")
    }

    // binding check no longer valid.
    // composeView and bottomContainer are initialized in onCreateView.

    // Decides which survey-related popup to show based on the current survey.
    mapContainerViewModel.surveyUpdateFlow.launchWhenStartedAndCollectFirst { surveyProperties ->
      surveyProperties.getInfoPopupMessageId()?.let { showInfoPopup(it) }
    }
  }

  private fun HomeScreenMapContainerViewModel.SurveyProperties.getInfoPopupMessageId(): Int? =
    if (noLois && !addLoiPermitted) {
      R.string.read_only_data_collection_hint
    } else {
      null
    }

  // ... showInfoPopup ...
  private fun showInfoPopup(messageId: Int) {
    ephemeralPopups.InfoPopup(bottomContainer, messageId, EphemeralPopups.PopupDuration.LONG).show()
  }

  private fun navigateToDataCollectionFragment(cardUiData: DataCollectionEntryPointData) {
    if (findNavController().currentDestination?.id != R.id.home_screen_fragment) {
      Timber.w("Refusing to navigate to data collection from ${findNavController().currentDestination?.label}")
      return
    }
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
