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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.google.android.ground.model.locationofinterest.LocationOfInterest
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.ui.home.BottomSheetState.Companion.hidden
import com.google.android.ground.ui.home.BottomSheetState.Companion.visible
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import timber.log.Timber

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val navigator: Navigator,
  private val surveyRepository: SurveyRepository
) : AbstractViewModel() {

  @JvmField
  val isSubmissionButtonVisible: @Hot(replays = true) MutableLiveData<Boolean> =
    MutableLiveData(false)

  // TODO(#719): Move into LocationOfInterestDetailsViewModel.
  val openDrawerRequests: @Hot FlowableProcessor<Nil> = PublishProcessor.create()
  val bottomSheetState: @Hot(replays = true) MutableLiveData<BottomSheetState> = MutableLiveData()
  val showOfflineAreaMenuItem: LiveData<Boolean> =
    surveyRepository.activeSurveyFlow.map { it?.tileOverlaySources?.isNotEmpty() ?: false }.asLiveData()

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

  fun showSurveySelector() {
    navigator.navigate(
      HomeScreenFragmentDirections.actionHomeScreenFragmentToSurveySelectorFragment(false)
    )
  }

  fun showOfflineAreas() {
    navigator.navigate(HomeScreenFragmentDirections.showOfflineAreas())
  }

  fun showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity())
  }

  fun showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus())
  }
}
