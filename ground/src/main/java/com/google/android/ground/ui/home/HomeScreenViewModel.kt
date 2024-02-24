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
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import com.google.android.ground.util.isNotNullOrEmpty
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val localValueStore: LocalValueStore,
  private val navigator: Navigator,
  private val submissionRepository: SubmissionRepository,
  private val surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val _openDrawerRequests: MutableSharedFlow<Unit> = MutableSharedFlow()
  val openDrawerRequestsFlow: SharedFlow<Unit> = _openDrawerRequests.asSharedFlow()

  val showOfflineAreaMenuItem: LiveData<Boolean> =
    surveyRepository.activeSurveyFlow.map { it?.tileSources?.isNotEmpty() ?: false }.asLiveData()

  fun hasDraftSubmission(): Boolean {
    return localValueStore.draftSubmissionId.isNotNullOrEmpty()
  }

  fun navigateToDraftSubmission() {
    viewModelScope.launch {
      surveyRepository.activeSurvey
        ?.let { survey ->
          submissionRepository.getDraftSubmission(survey)?.let { Pair(survey, it) }
        }
        ?.let { (survey, draftSubmission) ->
          if (draftSubmission.surveyId != survey.id) {
            Timber.e(
              "Can't load draft submission. Expected ${draftSubmission.surveyId} to be active, found ${survey.id}"
            )
          } else {
            navigator.navigate(
              HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
                draftSubmission.loiId,
                draftSubmission.jobId,
                true,
                SubmissionDeltasConverter.toString(draftSubmission.deltas),
              )
            )
          }
        }
    }
  }

  fun openNavDrawer() {
    viewModelScope.launch { _openDrawerRequests.emit(Unit) }
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

  fun showSignOutConfirmation() {
    navigator.navigate(HomeScreenFragmentDirections.showSignOutConfirmationDialogFragment())
  }
}
