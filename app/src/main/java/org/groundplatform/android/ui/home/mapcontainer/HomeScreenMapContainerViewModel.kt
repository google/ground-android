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

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
import org.groundplatform.android.Config.CLUSTERING_ZOOM_THRESHOLD
import org.groundplatform.android.model.Survey
import org.groundplatform.android.model.job.Job
import org.groundplatform.android.model.job.getDefaultColor
import org.groundplatform.android.model.locationofinterest.LocationOfInterest
import org.groundplatform.android.persistence.local.LocalValueStore
import org.groundplatform.android.proto.Survey.DataSharingTerms
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SubmissionRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.repository.UserRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.common.SharedViewModel
import org.groundplatform.android.ui.home.mapcontainer.jobs.AdHocDataCollectionButtonData
import org.groundplatform.android.ui.home.mapcontainer.jobs.DataCollectionEntryPointData
import org.groundplatform.android.ui.home.mapcontainer.jobs.SelectedLoiSheetData
import org.groundplatform.android.ui.map.Feature
import org.groundplatform.android.ui.map.FeatureType
import org.groundplatform.android.ui.map.isLocationOfInterest
import org.groundplatform.android.usecases.datasharingterms.GetDataSharingTermsUseCase

@OptIn(ExperimentalCoroutinesApi::class)
@SharedViewModel
class HomeScreenMapContainerViewModel
@Inject
internal constructor(
  private val getDataSharingTermsUseCase: GetDataSharingTermsUseCase,
  private val loiRepository: LocationOfInterestRepository,
  private val mapStateRepository: MapStateRepository,
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
  val featureClicked: MutableStateFlow<Feature?> = MutableStateFlow(null)

  /**
   * List of [Job]s which allow LOIs to be added during field collection, populated only when zoomed
   * in far enough.
   */
  private val adHocLoiJobs: Flow<List<Job>>

  /** Emits whether the current zoom has crossed the zoomed-in threshold or not to cluster LOIs. */
  val isZoomedInFlow: Flow<Boolean>

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

    adHocLoiJobs =
      activeSurvey.combine(isZoomedInFlow) { survey, isZoomedIn ->
        if (survey == null || !isZoomedIn) listOf()
        else survey.jobs.filter { it.canDataCollectorsAddLois && it.getAddLoiTask() != null }
      }
  }

  /** Enables the location lock if the active survey doesn't have a default camera position. */
  suspend fun maybeEnableLocationLock() {
    val surveyId = activeSurvey.filterNotNull().first().id

    // Note: This logic should be in sync with BaseMapViewModel.getLastSavedPositionOrDefaultBounds.
    if (loiRepository.hasValidLois(surveyId)) {
      // Skipping as there are valid LOIs. The camera position will be set to the LOI bounds.
    } else if (mapStateRepository.getCameraPosition(surveyId) != null) {
      // Skipping as the camera position is already set.
    } else {
      // Enabling location lock.
      enableLocationLockAndGetUpdates()
    }
  }

  fun getDataSharingTerms(): Result<DataSharingTerms?> = getDataSharingTermsUseCase()

  /**
   * Returns a flow of [DataCollectionEntryPointData] associated with the active survey's LOIs and
   * adhoc jobs for displaying the cards.
   */
  fun processDataCollectionEntryPoints():
    Flow<Pair<SelectedLoiSheetData?, List<AdHocDataCollectionButtonData>>> =
    combine(loisInViewport, featureClicked, adHocLoiJobs) { loisInView, feature, jobs ->
      val canUserSubmitData = userRepository.canUserSubmitData()
      val loiCard =
        loisInView
          .firstOrNull { it.geometry == feature?.geometry }
          ?.let {
            SelectedLoiSheetData(
              canCollectData = canUserSubmitData,
              loi = it,
              submissionCount = submissionRepository.getTotalSubmissionCount(it),
            )
          }
      if (loiCard == null && feature != null) {
        // The feature is not in view anymore.
        featureClicked.value = null
      }
      val jobCard =
        jobs.map { AdHocDataCollectionButtonData(canCollectData = canUserSubmitData, job = it) }
      Pair(loiCard, jobCard)
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

  fun selectLocationOfInterest(id: String?) {
    selectedLoiIdFlow.value = id
    if (id == null) {
      featureClicked.value = null
    }
  }
}
