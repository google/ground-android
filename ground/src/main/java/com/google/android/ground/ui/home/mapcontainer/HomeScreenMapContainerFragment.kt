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
import android.widget.TextView
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.google.android.ground.R
import com.google.android.ground.coroutines.ApplicationScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.coroutines.MainDispatcher
import com.google.android.ground.databinding.BasemapLayoutBinding
import com.google.android.ground.databinding.LoiCardsRecyclerViewBinding
import com.google.android.ground.databinding.MenuButtonBinding
import com.google.android.ground.model.locationofinterest.LOI_NAME_PROPERTY
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.home.DataConsentDialog
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardAdapter
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardUiData
import com.google.android.ground.ui.map.MapFragment
import com.google.android.ground.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
  private lateinit var adapter: MapCardAdapter
  private lateinit var infoPopup: EphemeralPopups.InfoPopup

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mapContainerViewModel = getViewModel(HomeScreenMapContainerViewModel::class.java)
    homeScreenViewModel = getViewModel(HomeScreenViewModel::class.java)
    adapter = MapCardAdapter { loi, view -> updateSubmissionCount(loi, view) }

    launchWhenStarted {
      val canUserSubmitData = userRepository.canUserSubmitData()

      // Handle collect button clicks
      adapter.setCollectDataListener {
        onCollectData(canUserSubmitData, hasValidTasks(it), it)
      }

      // Bind data for cards
      mapContainerViewModel.getMapCardUiData().launchWhenStartedAndCollect { (mapCards, loiCount) ->
        adapter.updateData(canUserSubmitData, mapCards, loiCount - 1)
      }
    }

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
  }

  private fun hasValidTasks(cardUiData: MapCardUiData) =
    when (cardUiData) {
      // LOI tasks are filtered out of the tasks list for pre-defined tasks.
      is MapCardUiData.LoiCardUiData ->
        cardUiData.loi.job.tasks.values.count { !it.isAddLoiTask } > 0
      is MapCardUiData.AddLoiCardUiData -> cardUiData.job.tasks.values.isNotEmpty()
    }

  /** Invoked when user clicks on the map cards to collect data. */
  private fun onCollectData(
    canUserSubmitData: Boolean,
    hasTasks: Boolean,
    cardUiData: MapCardUiData,
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
    mapContainerViewModel.activeSurveyDataConsentFlow.launchWhenStartedAndCollectFirst { hasDataConsent ->
      if (!hasDataConsent) {
        (view as ViewGroup).addView(
          ComposeView(requireContext()).apply {
            setContent {
              val showDataConsentDialog = remember { mutableStateOf(true) }
              when {
                showDataConsentDialog.value -> {
                  AppTheme {
                    DataConsentDialog(showDataConsentDialog, EXAMPLE_TEXT) {
                      navigateToDataCollectionFragment(cardUiData)
                    }
                  }
                }
              }
            }
          }
        )
      } else {
        navigateToDataCollectionFragment(cardUiData)
      }
    }
  }

  /** Updates the given [TextView] with the submission count for the given [LocationOfInterest]. */
  private fun updateSubmissionCount(loi: LocationOfInterest, view: TextView) {
    externalScope.launch {
      val count = submissionRepository.getTotalSubmissionCount(loi)
      val submissionText =
        if (count == 0) resources.getString(R.string.no_submissions)
        else resources.getQuantityString(R.plurals.submission_count, count, count)
      withContext(mainDispatcher) { view.text = submissionText }
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
    setupBottomLoiCards()
    showDataCollectionHint()
    setupDataConsentFlows()
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
    val menuBinding = MenuButtonBinding.inflate(layoutInflater, mapOverlay, true)
    menuBinding.homeScreenViewModel = homeScreenViewModel
    menuBinding.lifecycleOwner = this
  }

  private fun setupBottomLoiCards() {
    val container = binding.bottomContainer
    val recyclerViewBinding = LoiCardsRecyclerViewBinding.inflate(layoutInflater, container, true)
    val recyclerView = recyclerViewBinding.recyclerView
    recyclerView.adapter = adapter
    recyclerView.addOnScrollListener(
      object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
          super.onScrollStateChanged(recyclerView, newState)
          val layoutManager = recyclerView.layoutManager as LinearLayoutManager
          val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
          val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()
          val firstCompletelyVisiblePosition =
            layoutManager.findFirstCompletelyVisibleItemPosition()
          var midPosition = (firstVisiblePosition + lastVisiblePosition) / 2

          // Focus the last card
          if (firstCompletelyVisiblePosition > midPosition) {
            midPosition = firstCompletelyVisiblePosition
          }

          adapter.focusItemAtIndex(midPosition)
        }
      }
    )

    val helper: SnapHelper = PagerSnapHelper()
    helper.attachToRecyclerView(recyclerView)

    mapContainerViewModel.loiClicks.launchWhenStartedAndCollect {
      val index = it?.let { adapter.getIndex(it) } ?: -1
      if (index != -1) {
        recyclerView.scrollToPosition(index)
        adapter.focusItemAtIndex(index)
      }
    }
  }

  private fun setupDataConsentFlows() {
//    mapContainerViewModel.activeSurvey.combine(mapContainerViewModel.dataConsentUpdatedFlow) {
//
//    }
  }

  private fun navigateToDataCollectionFragment(cardUiData: MapCardUiData) {
    when (cardUiData) {
      is MapCardUiData.LoiCardUiData ->
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
            cardUiData.loi.id,
            cardUiData.loi.properties[LOI_NAME_PROPERTY] as? String?,
            cardUiData.loi.job.id,
            false,
            null,
          )
        )
      is MapCardUiData.AddLoiCardUiData ->
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
            null,
            null,
            cardUiData.job.id,
            false,
            null,
          )
        )
    }
  }

  override fun onMapReady(map: MapFragment) {
    mapContainerViewModel.mapLoiFeatures.launchWhenStartedAndCollect { map.setFeatures(it) }

    adapter.setLoiCardFocusedListener {
      when (it) {
        is MapCardUiData.LoiCardUiData -> mapContainerViewModel.selectLocationOfInterest(it.loi.id)
        is MapCardUiData.AddLoiCardUiData,
        null -> mapContainerViewModel.selectLocationOfInterest(null)
      }
    }
  }

  override fun getMapViewModel(): BaseMapViewModel = mapContainerViewModel

  companion object {
    const val EXAMPLE_TEXT = """
# Introduction

Ground values your privacy and is committed to protecting your personal information. This form explains how we may collect, use, and share your data for research or other purposes. By signing this form, you consent to the practices described below.

## What Data We Collect

We may collect the following types of data:

*   Personal Information: Name, contact details, demographic information (if applicable).
*   Research Data: Responses to surveys, interviews, or other study-related data.
*   Usage Data: Information about how you interact with our services or website (if applicable).

## How We Use Your Data

We may use your data for the following purposes:

*   Research: To analyze and publish findings, contribute to scientific knowledge, and improve our services.
*   Internal Analysis: To understand how our services are used and to make improvements.
*   Communication: To contact you with updates, information about research results, or opportunities to participate in future studies.

## How We Share Your Data

We may share your data with:

*   Researchers: We may share de-identified data with qualified researchers for approved studies.
*   Partners: We may share de-identified data with partner organizations for research or analysis.
*   Service Providers: We may share your data with trusted third-party service providers who help us deliver our services (e.g., data storage, analysis).

## Your Rights

You have the right to:

*   Access Your Data: Request a copy of the personal data we hold about you.
*   Correct Your Data: Ask us to correct any inaccurate or incomplete data.
*   Withdraw Consent: You may withdraw your consent to data sharing at any time.
*   Object to Processing: You can object to certain types of processing (e.g., direct marketing).

## Data Security

We take appropriate technical and organizational measures to protect your data from unauthorized access, disclosure, alteration, or destruction.

## Data Retention

We will retain your data for as long as necessary to fulfill the purposes outlined in this form or as required by law.

## Changes to this Form

We may update this form from time to time. We will notify you of any material changes.

## Contact Us

If you have any questions or concerns about our data practices, please contact us at [email address removed].

## Consent

By agreeing below, I acknowledge that I have read and understood this data sharing consent form. I freely give my consent for Ground to collect, use, and share my data as described above.
"""
  }
}
