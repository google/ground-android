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
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.ui.common.AbstractViewModel
import com.google.android.ground.ui.common.Navigator
import com.google.android.ground.ui.common.SharedViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@SharedViewModel
class HomeScreenViewModel
@Inject
internal constructor(
  private val navigator: Navigator,
  private val surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val _openDrawerRequests: MutableSharedFlow<Unit> = MutableSharedFlow()
  val openDrawerRequestsFlow: SharedFlow<Unit> = _openDrawerRequests.asSharedFlow()

  // TODO(#1730): Allow tile source configuration from a non-survey accessible source.
  val showOfflineAreaMenuItem: LiveData<Boolean> =
    surveyRepository.activeSurveyFlow.map { true }.asLiveData()

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
