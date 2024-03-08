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
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractMapContainerFragment
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.EphemeralPopups
import com.google.android.ground.ui.home.HomeScreenFragmentDirections
import com.google.android.ground.ui.home.HomeScreenViewModel
import com.google.android.ground.ui.home.mapcontainer.HomeScreenMapContainerViewModel.SurveyProperties
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardAdapter
import com.google.android.ground.ui.home.mapcontainer.cards.MapCardUiData
import com.google.android.ground.ui.map.MapFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
      adapter.setCollectDataListener { onCollectData(canUserSubmitData, it) }

      // Bind data for cards
      mapContainerViewModel.getMapCardUiData().launchWhenStartedAndCollect { (mapCards, loiCount) ->
        adapter.updateData(canUserSubmitData, mapCards, loiCount - 1)
      }
    }

    map.featureClicks.launchWhenStartedAndCollect { mapContainerViewModel.onFeatureClicked(it) }
  }

  /** Invoked when user clicks on the map cards to collect data. */
  private fun onCollectData(canUserSubmitData: Boolean, cardUiData: MapCardUiData) {
    if (canUserSubmitData) {
      navigateToDataCollectionFragment(cardUiData)
    } else {
      // Skip data collection screen if the user can't submit any data
      // TODO(#1667): Revisit UX for displaying view only mode
      ephemeralPopups.ErrorPopup().show(getString(R.string.collect_data_viewer_error))
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
  }

  /**
   * Displays a popup hint informing users how to begin collecting data, based on the properties of
   * the active survey.
   *
   * This method should only be called after view creation.
   */
  private fun showDataCollectionHint() {
    if (!this::mapContainerViewModel.isInitialized) {
      return Timber.w("showDataCollectionHint() called before mapContainerViewModel initialized")
    }
    mapContainerViewModel.surveyUpdateFlow.launchWhenStartedAndCollect(this::onSurveyUpdate)
  }

  private fun onSurveyUpdate(surveyProperties: SurveyProperties) {
    if (!this::binding.isInitialized) {
      return Timber.w("showDataCollectionHint() called before binding initialized")
    }
    val messageId =
      when {
        surveyProperties.addLoiPermitted -> R.string.suggest_data_collection_hint
        surveyProperties.readOnly -> R.string.read_only_data_collection_hint
        else -> R.string.predefined_data_collection_hint
      }
    infoPopup =
      ephemeralPopups.InfoPopup(
        binding.bottomContainer,
        messageId,
        EphemeralPopups.PopupDuration.LONG,
      )
    infoPopup.show()
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

  private fun navigateToDataCollectionFragment(cardUiData: MapCardUiData) {
    when (cardUiData) {
      is MapCardUiData.LoiCardUiData ->
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
            cardUiData.loi.id,
            cardUiData.loi.job.id,
            false,
            null,
          )
        )
      is MapCardUiData.AddLoiCardUiData ->
        navigator.navigate(
          HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
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
}
