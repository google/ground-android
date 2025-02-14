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

import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import com.google.android.ground.Config.CLUSTERING_ZOOM_THRESHOLD
import com.google.android.ground.R
import com.google.android.ground.domain.usecases.datasharingterms.GetDataSharingTermsUseCase
import com.google.android.ground.model.Survey
import com.google.android.ground.model.job.getDefaultColor
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.proto.Survey.DataSharingTerms
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointEvent
import com.google.android.ground.ui.home.mapcontainer.jobs.DataCollectionEntryPointState
import com.google.android.ground.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.ui.map.FeatureType
import com.google.android.ground.ui.map.isLocationOfInterest
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val getDataSharingTermsUseCase: GetDataSharingTermsUseCase,
  private val loiRepository: LocationOfInterestRepository,
  mapStateRepository: MapStateRepository,
  private val submissionRepository: SubmissionRepository,
  locationManager: LocationManager,
  settingsManager: SettingsManager,
  offlineAreaRepository: OfflineAreaRepository,
  permissionsManager: PermissionsManager,
  private val surveyRepository: SurveyRepository,
  private val userRepository: UserRepository,
  private val localValueStore: LocalValueStore,
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    surveyRepository,
    loiRepository,
  ) {

  private val selectedLoiIdFlow = MutableStateFlow<String?>(null)

  private val activeSurvey: StateFlow<Survey?> = surveyRepository.activeSurveyFlow

  /** Captures essential, high-level derived properties for a given survey. */
  data class SurveyProperties(val addLoiPermitted: Boolean, val noLois: Boolean)

  /**
   * This flow emits [SurveyProperties] when the active survey changes. Callers can use this data to
   * determine if and how behavior should change based on differing survey properties.
   */
  val surveyUpdateFlow: Flow<SurveyProperties> =
    activeSurvey.filterNotNull().map { survey ->
      val lois = loiRepository.getValidLois(survey).first()
      val addLoiPermitted = survey.jobs.any { job -> job.canDataCollectorsAddLois }
      SurveyProperties(addLoiPermitted = addLoiPermitted, noLois = lois.isEmpty())
    }

  /** Set of [Feature] to render on the map. */
  val mapLoiFeatures: Flow<Set<Feature>>

  /**
   * List of [LocationOfInterest] for the active survey that are present within the viewport and
   * zoom level is clustering threshold or higher.
   */
  private val loisInViewport: StateFlow<List<LocationOfInterest>>

  /** [Feature] clicked by the user. */
  private val featureClicked: MutableStateFlow<Feature?> = MutableStateFlow(null)

  /** Emits whether the current zoom has crossed the zoomed-in threshold or not to cluster LOIs. */
  val isZoomedInFlow: Flow<Boolean>

  private val _dataCollectionEntryPointState = mutableStateOf(DataCollectionEntryPointState())
  val dataCollectionEntryPointState: State<DataCollectionEntryPointState>
    get() = _dataCollectionEntryPointState

  private val _uiEvents: MutableSharedFlow<HomeScreenMapContainerEvent> = MutableSharedFlow()
  val uiEventsFlow: SharedFlow<HomeScreenMapContainerEvent> = _uiEvents.asSharedFlow()

  init {
    // THIS SHOULD NOT BE CALLED ON CONFIG CHANGE

    @OptIn(FlowPreview::class)
    mapLoiFeatures =
      activeSurvey.flatMapLatest {
        if (it == null) flowOf(setOf())
        else
          getLocationOfInterestFeatures(it)
            .debounce(1000.milliseconds)
            .distinctUntilChanged()
            .combine(selectedLoiIdFlow) { loiFeatures, selectedLoiId ->
              updatedLoiSelectedStates(loiFeatures, selectedLoiId)
            }
      }

    isZoomedInFlow =
      getCurrentCameraPosition().mapNotNull { it.zoomLevel }.map { it >= CLUSTERING_ZOOM_THRESHOLD }

    loisInViewport =
      activeSurvey
        .combine(isZoomedInFlow) { survey, isZoomedIn -> Pair(survey, isZoomedIn) }
        .flatMapLatest { (survey, isZoomedIn) ->
          val bounds = currentCameraPosition.value?.bounds
          if (bounds == null || survey == null || !isZoomedIn) flowOf(listOf())
          else loiRepository.getWithinBounds(survey, bounds)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, listOf())

    /**
     * List of [AdHocDataCollectionButtonData]s which allow LOIs to be added during field
     * collection, populated only when zoomed in far enough.
     */
    val adHocDataCollectionButtonData: Flow<List<AdHocDataCollectionButtonData>> =
      activeSurvey.combine(isZoomedInFlow) { survey, isZoomedIn ->
        if (survey == null || !isZoomedIn) listOf()
        else {
          val canCollectData = userRepository.canUserSubmitData(survey)
          survey.jobs
            .filter { it.canDataCollectorsAddLois && it.getAddLoiTask() != null }
            .map { AdHocDataCollectionButtonData(canCollectData = canCollectData, job = it) }
        }
      }

    /**
     * Returns a flow of [DataCollectionEntryPointData] associated with the active survey's LOIs and
     * adhoc jobs for displaying the cards.
     */
    val selectedLoiSheetData: Flow<SelectedLoiSheetData?> =
      combine(activeSurvey, loisInViewport, featureClicked) { survey, loisInView, feature ->
        val canUserSubmitData = userRepository.canUserSubmitData(survey)
        val loi: LocationOfInterest? = loisInView.firstOrNull { it.geometry == feature?.geometry }
        if (loi == null && feature != null) {
          // The feature is not in view anymore.
          featureClicked.value = null
        }
        loi?.let {
          SelectedLoiSheetData(
            canCollectData = canUserSubmitData,
            loi = it,
            submissionCount = submissionRepository.getTotalSubmissionCount(it),
          )
        }
      }

    viewModelScope.launch {
      adHocDataCollectionButtonData.collectLatest {
        handleEvent(
          DataCollectionEntryPointEvent.UpdateNewLoiJobCardDataList(newLoiJobCardDataList = it)
        )
      }
    }

    viewModelScope.launch {
      selectedLoiSheetData.collectLatest {
        handleEvent(
          DataCollectionEntryPointEvent.UpdateSelectedLoiSheetData(selectedLoiSheetData = it)
        )
      }
    }
  }

  fun handleEvent(event: DataCollectionEntryPointEvent) {
    with(_dataCollectionEntryPointState) {
      when (event) {
        is DataCollectionEntryPointEvent.StartDataCollection -> {
          onCollectData(event.data)
          value.copy(
            showNewLoiJobSelectionModal = false,
            showLoiSheet = false,
            selectedLoiSheetData = null,
          )
        }

        is DataCollectionEntryPointEvent.ShowNewLoiJobSelectionModal -> {
          value.copy(showNewLoiJobSelectionModal = true, showLoiSheet = false)
        }

        is DataCollectionEntryPointEvent.DismissNewLoiJobSelectionModal -> {
          value.copy(showNewLoiJobSelectionModal = false)
        }

        is DataCollectionEntryPointEvent.DismissSelectedLoiJobSheet -> {
          selectLocationOfInterest(null)
          value.copy(showLoiSheet = false, selectedLoiSheetData = null)
        }

        is DataCollectionEntryPointEvent.UpdateNewLoiJobCardDataList -> {
          value.copy(newLoiJobCardDataList = event.newLoiJobCardDataList)
        }

        is DataCollectionEntryPointEvent.UpdateSelectedLoiSheetData -> {
          selectLocationOfInterest(event.selectedLoiSheetData?.loi?.id)
          value.copy(
            selectedLoiSheetData = event.selectedLoiSheetData,
            showLoiSheet = event.selectedLoiSheetData != null,
          )
        }
      }.also { value = it }
    }
  }

  private fun showError(@StringRes messageResId: Int) {
    viewModelScope.launch {
      _uiEvents.emit(HomeScreenMapContainerEvent.ShowErrorToast(messageResId))
    }
  }

  private fun navigateToDataCollectionFragment(cardUiData: DataCollectionEntryPointData) {
    viewModelScope.launch {
      _uiEvents.emit(HomeScreenMapContainerEvent.NavigateToDataCollectionFragment(cardUiData))
    }
  }

  private fun showDataSharingTermsDialog(
    cardUiData: DataCollectionEntryPointData,
    terms: DataSharingTerms,
  ) {
    viewModelScope.launch {
      _uiEvents.emit(HomeScreenMapContainerEvent.ShowDataSharingTermsDialog(cardUiData, terms))
    }
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
      showError(R.string.collect_data_viewer_error)
      return
    }

    if (!hasValidTasks(cardUiData)) {
      // NOTE(#2539): The DataCollectionFragment will crash if there are no tasks.
      showError(R.string.no_tasks_error)
      return
    }

    getDataSharingTermsUseCase()
      .onSuccess { terms ->
        if (terms == null) {
          // Data sharing terms already accepted or missing.
          navigateToDataCollectionFragment(cardUiData)
        } else {
          showDataSharingTermsDialog(cardUiData, terms)
        }
      }
      .onFailure {
        Timber.e(it, "Failed to get data sharing terms")
        showError(
          if (it is GetDataSharingTermsUseCase.InvalidCustomSharingTermsException) {
            R.string.invalid_data_sharing_terms
          } else {
            R.string.something_went_wrong
          }
        )
      }
  }

  private fun updatedLoiSelectedStates(
    features: Set<Feature>,
    selectedLoiId: String?,
  ): Set<Feature> =
    features
      .map { it.withSelected(it.isLocationOfInterest() && it.tag.id == selectedLoiId) }
      .toSet()

  /**
   * Intended as a callback for when a specific map [Feature] is clicked. If the click is ambiguous,
   * (list of features > 1), it chooses the [Feature] with the smallest area. If multiple features
   * have the same area, or in the case of points, no area, the first is chosen. Does nothing if the
   * list of provided features is empty.
   */
  fun onFeatureClicked(features: Set<Feature>) {
    featureClicked.value = features.minByOrNull { it.geometry.area }
  }

  fun grantDataSharingConsent() {
    val survey = requireNotNull(surveyRepository.activeSurvey)
    localValueStore.setDataSharingConsent(survey.id, true)
  }

  private fun getLocationOfInterestFeatures(survey: Survey): Flow<Set<Feature>> =
    loiRepository.getValidLois(survey).map { it.map { loi -> loi.toFeature() }.toPersistentSet() }

  private suspend fun LocationOfInterest.toFeature() =
    Feature(
      id = id,
      type = FeatureType.LOCATION_OF_INTEREST.ordinal,
      flag = submissionRepository.getTotalSubmissionCount(this) > 0,
      geometry = geometry,
      style = Feature.Style(job.getDefaultColor()),
      clusterable = true,
      selected = true,
    )

  private fun selectLocationOfInterest(id: String?) {
    selectedLoiIdFlow.value = id
    if (id == null) {
      featureClicked.value = null
    }
  }
}
