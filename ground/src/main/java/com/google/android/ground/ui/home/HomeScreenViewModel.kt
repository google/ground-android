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
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SubmissionRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.auth.AuthenticationManager
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val localValueStore: LocalValueStore,
  private val navigator: Navigator,
  private val offlineAreaRepository: OfflineAreaRepository,
  private val submissionRepository: SubmissionRepository,
  private val surveyRepository: SurveyRepository,
  private val authenticationManager: AuthenticationManager,
) : AbstractViewModel() {

  private val _openDrawerRequests: MutableSharedFlow<Unit> = MutableSharedFlow()
  val openDrawerRequestsFlow: SharedFlow<Unit> = _openDrawerRequests.asSharedFlow()

  private val _userDetails = MutableLiveData<User>()
  val userDetails: LiveData<User> = _userDetails

  private val _showUserDetailsDialog = MutableLiveData(true)
  val showUserDetailsDialog: LiveData<Boolean> = _showUserDetailsDialog

  private val _showSignOutDialog = MutableLiveData(false)
  val showSignOutDialog: LiveData<Boolean> = _showSignOutDialog

  init {
    viewModelScope.launch { _userDetails.value = authenticationManager.getAuthenticatedUser() }
  }

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

  private suspend fun getOfflineAreas() = offlineAreaRepository.offlineAreas().first()

  fun showOfflineAreas() {
    viewModelScope.launch {
      navigator.navigate(
        if (getOfflineAreas().isEmpty()) HomeScreenFragmentDirections.showOfflineAreaSelector()
        else HomeScreenFragmentDirections.showOfflineAreas()
      )
    }
  }

  fun showSettings() {
    navigator.navigate(HomeScreenFragmentDirections.actionHomeScreenFragmentToSettingsActivity())
  }

  fun showSyncStatus() {
    navigator.navigate(HomeScreenFragmentDirections.showSyncStatus())
  }

  fun reInitializeDialogFlags() {
    _showUserDetailsDialog.value = true
    _showSignOutDialog.value = false
  }

  fun showSignOutConfirmationDialog() {
    _showUserDetailsDialog.value = false
    _showSignOutDialog.value = true
  }

  fun dismissDialogs() {
    _showUserDetailsDialog.value = false
    _showSignOutDialog.value = false
  }
}
