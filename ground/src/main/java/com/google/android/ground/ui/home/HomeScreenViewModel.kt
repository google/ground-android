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
import com.google.android.ground.model.locationofinterest.LocationOfInterest
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
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
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
  val showLocationOfInterestSelectorRequests: @Hot Subject<ImmutableList<LocationOfInterest>> =
    PublishSubject.create()

  /**
   * Live cache of locations of interest. Updated every time the underlying local storage data
   * changes.
   */
  private var locationOfInterestCache: ImmutableSet<LocationOfInterest> = ImmutableSet.of()

  fun openNavDrawer() {
    openDrawerRequests.onNext(Nil.NIL)
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

  fun init() {
    surveyRepository.loadLastActiveSurvey()
  }

  fun showOfflineAreas() {
    navigator.navigate(HomeScreenFragmentDirections.showOfflineAreas())
  }

  fun showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity())
  }

  /** Intended for use as a callback for handling user clicks on rendered map features. */
  fun onFeatureClick(features: ImmutableList<Feature>) {
    val loiFeatureIds =
      features.filter { it.tag == Feature.Type.LOCATION_OF_INTEREST }.map { it.id }
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

  fun showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus())
  }

  init {
    val locationsOfInterestSubscription =
      surveyRepository.activeSurvey
        .switchMap {
          if (it.isPresent) {
            locationOfInterestRepository.getLocationsOfInterestOnceAndStream(it.get())
          } else {
            Flowable.empty()
          }
        }
        .subscribe { locationOfInterestCache = it }

    disposeOnClear(locationsOfInterestSubscription)
  }
}
