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

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Point
import com.google.android.ground.model.job.Job
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.model.mutation.LocationOfInterestMutation
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Loadable
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.BottomSheetState.Companion.hidden
import com.google.android.ground.ui.home.BottomSheetState.Companion.visible
import com.google.android.ground.ui.map.Feature
import com.google.android.ground.util.toImmutableList
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import java.util.*
import java8.util.Optional
import javax.inject.Inject
import timber.log.Timber

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
  private val navigator: Navigator
) : AbstractViewModel() {

  @JvmField
  val isSubmissionButtonVisible: @Hot(replays = true) MutableLiveData<Boolean> =
    MutableLiveData(false)

  /** The state and value of the currently active survey (loading, loaded, etc.). */
  val surveyLoadingState: LiveData<Loadable<Survey>> =
    LiveDataReactiveStreams.fromPublisher(surveyRepository.surveyLoadingState)

  // TODO(#719): Move into LocationOfInterestDetailsViewModel.
  val openDrawerRequests: @Hot FlowableProcessor<Nil> = PublishProcessor.create()
  val bottomSheetState: @Hot(replays = true) MutableLiveData<BottomSheetState> = MutableLiveData()
  private val addLocationOfInterestRequests: @Hot FlowableProcessor<LocationOfInterestMutation> =
    PublishProcessor.create()
  val addLocationOfInterestResults: @Hot Flowable<LocationOfInterest>
  val errors: @Hot FlowableProcessor<Throwable> = PublishProcessor.create()
  val showLocationOfInterestSelectorRequests: @Hot Subject<ImmutableList<LocationOfInterest>> =
    PublishSubject.create()
  /**
   * Live cache of locations of interest. Updated every time the underlying local storage data
   * changes.
   */
  private var locationOfInterestCache: ImmutableSet<LocationOfInterest> = ImmutableSet.of()

  // TODO: Cleanup this method
  fun addLoi(job: Job, point: Point) {
    activeSurvey.ifPresentOrElse({ survey: Survey ->
      addLocationOfInterestRequests.onNext(
        locationOfInterestRepository.newMutation(survey.id, job.id, point, Date())
      )
    }) {
      throw IllegalStateException("Empty survey")
    }
  }

  // TODO: Cleanup this method
  fun addPolygonOfInterest(areaOfInterest: LocationOfInterest) {
    activeSurvey.ifPresentOrElse({ survey: Survey ->
      addLocationOfInterestRequests.onNext(
        locationOfInterestRepository.newPolygonOfInterestMutation(
          survey.id,
          areaOfInterest.job.id,
          areaOfInterest.geometry.vertices,
          Date()
        )
      )
    }) {
      throw IllegalStateException("Empty survey")
    }
  }

  fun openNavDrawer() {
    openDrawerRequests.onNext(Nil.NIL)
  }

  /** Intended for use as a callback for handling user clicks on rendered map features. */
  fun onMarkerClick(feature: Feature) =
    when (val tag = feature.tag) {
      is Feature.LocationOfInterestTag -> {
        val loi = locationOfInterestCache.find { it.id == tag.id }

        if (loi != null) {
          showBottomSheet(loi)
        } else {
          Timber.e("user selected LOI feature does not exist in the active survey")
        }
      }
      else -> {} // only LocationOfInterest features are currently handled.
    }

  fun onLocationOfInterestSelected(locationOfInterest: LocationOfInterest?) {
    showBottomSheet(locationOfInterest)
  }

  private fun showBottomSheet(loi: LocationOfInterest?) {
    Timber.d("showing bottom sheet")
    isSubmissionButtonVisible.value = true
    bottomSheetState.value = visible(loi!!)
  }

  fun onBottomSheetHidden() {
    bottomSheetState.value = hidden()
    isSubmissionButtonVisible.value = false
  }

  fun addSubmission() {
    val state = bottomSheetState.value
    if (state == null) {
      Timber.e("Missing bottomSheetState")
      return
    }
    val loi = state.locationOfInterest
    if (loi == null) {
      Timber.e("Missing loi")
      return
    }
    navigator.navigate(HomeScreenFragmentDirections.addSubmission(loi.surveyId, loi.id, loi.job.id))
  }

  fun init() {
    // Last active survey will be loaded once view subscribes to activeProject.
    surveyRepository.loadLastActiveSurvey()
  }

  fun showOfflineAreas() {
    navigator.navigate(HomeScreenFragmentDirections.showOfflineAreas())
  }

  fun showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity())
  }

  fun onLocationOfInterestClick(features: ImmutableList<Feature>) {
    val loiFeatureIds =
      features.filter { it.tag is Feature.LocationOfInterestTag }.map { it.tag.id }
    val locationsOfInterest: ImmutableList<LocationOfInterest> =
      locationOfInterestCache.filter { loiFeatureIds.contains(it.id) }.toImmutableList()

    if (locationsOfInterest.isEmpty()) {
      Timber.e("onLocationOfInterestClick called with empty or null map locationsOfInterest")
      return
    }

    if (locationsOfInterest.size == 1) {
      onLocationOfInterestSelected(locationsOfInterest[0])
      return
    }

    showLocationOfInterestSelectorRequests.onNext(locationsOfInterest)
  }

  private val activeSurvey: Optional<Survey>
    get() = surveyLoadingState.value?.value() ?: Optional.empty()

  fun showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus())
  }

  init {
    addLocationOfInterestResults =
      addLocationOfInterestRequests.switchMapSingle { mutation: LocationOfInterestMutation ->
        locationOfInterestRepository
          .applyAndEnqueue(mutation)
          .andThen(locationOfInterestRepository.getLocationOfInterest(mutation))
          .doOnError { t: Throwable -> errors.onNext(t) }
          .onErrorResumeNext(Single.never())
      } // Prevent from breaking upstream.

    activeSurvey.ifPresent {
      val locationsOfInterestSubscription =
        locationOfInterestRepository.getLocationsOfInterestOnceAndStream(it).subscribe {
          locationOfInterestCache = it
        }

      disposeOnClear(locationsOfInterestSubscription)
    }
  }
}
