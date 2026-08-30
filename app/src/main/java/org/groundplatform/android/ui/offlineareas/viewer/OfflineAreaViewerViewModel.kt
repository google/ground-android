/*
 * Copyright 2020 Google LLC
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
package org.groundplatform.android.ui.offlineareas.viewer

import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.groundplatform.android.di.coroutines.IoDispatcher
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.domain.model.imagery.OfflineArea
import org.groundplatform.domain.model.util.toMb
import org.groundplatform.domain.repository.LocationOfInterestRepositoryInterface
import org.groundplatform.domain.repository.MapStateRepositoryInterface
import org.groundplatform.domain.repository.OfflineAreaRepositoryInterface
import org.groundplatform.domain.repository.SurveyRepositoryInterface
import org.groundplatform.ui.util.toMbString
import timber.log.Timber

/**
 * View model for the OfflineAreaViewerFragment. Manages offline area deletions and calculates the
 * storage size of an area on the user's device.
 */
class OfflineAreaViewerViewModel
@Inject
constructor(
  private val offlineAreaRepository: OfflineAreaRepositoryInterface,
  locationManager: LocationManager,
  mapStateRepository: MapStateRepositoryInterface,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  surveyRepository: SurveyRepositoryInterface,
  locationOfInterestRepository: LocationOfInterestRepositoryInterface,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) :
  BaseMapViewModel(
    locationManager,
    mapStateRepository,
    settingsManager,
    offlineAreaRepository,
    permissionsManager,
    surveyRepository,
    locationOfInterestRepository,
  ) {

  private val _uiState = MutableStateFlow(OfflineAreaViewerState())
  val uiState: StateFlow<OfflineAreaViewerState> = _uiState.asStateFlow()

  private val navigateUpChannel = Channel<Unit>(Channel.BUFFERED)
  val navigateUp = navigateUpChannel.receiveAsFlow()

  /** Initialize the view model with the given arguments. */
  fun initialize(offlineAreaId: String) {
    viewModelScope.launch(ioDispatcher) {
      val thisArea = offlineAreaRepository.getOfflineArea(offlineAreaId)
      thisArea?.let {
        val size = offlineAreaRepository.sizeOnDevice(it).toMb().toMbString()
        _uiState.update { state ->
          state.copy(
            area = it,
            areaName = it.name,
            areaSize = size,
          )
        }
      } ?: run { navigateUpChannel.send(Unit) }
    }
  }

  /** Deletes the area associated with this view model. */
  fun onRemoveButtonClick() {
    if (_uiState.value.isProgressOverlayVisible || _uiState.value.area == null) return
    _uiState.update { it.copy(isProgressOverlayVisible = true) }
    viewModelScope.launch(ioDispatcher) { removeOfflineArea(_uiState.value.area) }
  }

  private suspend fun removeOfflineArea(deletedArea: OfflineArea?) {
    if (deletedArea == null) {
      _uiState.update { it.copy(isProgressOverlayVisible = false) }
      return
    }
    try {
      Timber.d("Removing offline area ${deletedArea.name}")
      offlineAreaRepository.removeFromDevice(deletedArea)
      _uiState.update { it.copy(isProgressOverlayVisible = false, area = null) }
      navigateUpChannel.send(Unit)
    } catch (e: kotlinx.coroutines.CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.e(e, "Failed to remove offline area")
      _uiState.update { it.copy(isProgressOverlayVisible = false) }
    }
  }
}
