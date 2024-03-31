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
import androidx.lifecycle.viewModelScope
import com.google.android.ground.model.User
import com.google.android.ground.persistence.local.LocalValueStore
import com.google.android.ground.persistence.local.room.converter.SubmissionDeltasConverter
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.repository.UserRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
  private var userRepository: UserRepository
) : AbstractViewModel() {

  private val _openDrawerRequests: MutableSharedFlow<Unit> = MutableSharedFlow()
  val openDrawerRequestsFlow: SharedFlow<Unit> = _openDrawerRequests.asSharedFlow()

  private val _userData = MutableLiveData<User>()
  private val _userId = MutableLiveData<String>()
  val userData: LiveData<User> = _userData
  val userId: LiveData<String> = _userId

  // TODO(#1730): Allow tile source configuration from a non-survey accessible source.
  val showOfflineAreaMenuItem: LiveData<Boolean> = MutableLiveData(true)

  suspend fun maybeNavigateToDraftSubmission() {
    val draftId = localValueStore.draftSubmissionId
    val survey = surveyRepository.activeSurvey

    // Missing draft submission
    if (draftId.isNullOrEmpty() || survey == null) {
      return
    }

    val draft = submissionRepository.getDraftSubmission(draftId, survey)

    // TODO: Check whether the previous user id matches with current user or not.
    if (draft != null && draft.surveyId == survey.id) {
      navigator.navigate(
        HomeScreenFragmentDirections.actionHomeScreenFragmentToDataCollectionFragment(
          draft.loiId,
          draft.loiName ?: "",
          draft.jobId,
          true,
          SubmissionDeltasConverter.toString(draft.deltas),
        )
      )
    }

    if (draft != null && draft.surveyId != survey.id) {
      Timber.e("Skipping draft submission, survey id doesn't match")
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

  fun getUserData(): User? {
    viewModelScope.launch {
      _userId.value = userRepository.getUserId()
      _userData.value = userId.value?.let { userRepository.getUser(it) }
    }
    return userData.value
  }
}
