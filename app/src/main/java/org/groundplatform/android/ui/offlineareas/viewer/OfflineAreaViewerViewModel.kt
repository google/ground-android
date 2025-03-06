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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.groundplatform.android.coroutines.IoDispatcher
import org.groundplatform.android.model.imagery.OfflineArea
import org.groundplatform.android.repository.LocationOfInterestRepository
import org.groundplatform.android.repository.MapStateRepository
import org.groundplatform.android.repository.OfflineAreaRepository
import org.groundplatform.android.repository.SurveyRepository
import org.groundplatform.android.system.LocationManager
import org.groundplatform.android.system.PermissionsManager
import org.groundplatform.android.system.SettingsManager
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.util.toMb
import org.groundplatform.android.util.toMbString
import timber.log.Timber

/**
 * View model for the OfflineAreaViewerFragment. Manges offline area deletions and calculates the
 * storage size of an area on the user's device.
 */
class OfflineAreaViewerViewModel
@Inject
constructor(
  private val offlineAreaRepository: OfflineAreaRepository,
  locationManager: LocationManager,
  mapStateRepository: MapStateRepository,
  settingsManager: SettingsManager,
  permissionsManager: PermissionsManager,
  surveyRepository: SurveyRepository,
  locationOfInterestRepository: LocationOfInterestRepository,
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

  /** Returns the offline area associated with this view model. */
  val area = MutableLiveData<OfflineArea>()
  val areaName = MutableLiveData<String>()
  val areaSize = MutableLiveData<String>()
  val progressOverlayVisible = MutableLiveData<Boolean>()

  private val _navigateUp = MutableSharedFlow<Unit>(replay = 0)
  val navigateUp = _navigateUp.asSharedFlow()

  /** Initialize the view model with the given arguments. */
  fun initialize(offlineAreaId: String) {
    viewModelScope.launch(ioDispatcher) {
      val thisArea = offlineAreaRepository.getOfflineArea(offlineAreaId)
      thisArea?.let {
        area.postValue(it)
        areaSize.postValue(offlineAreaRepository.sizeOnDevice(it).toMb().toMbString())
        areaName.postValue(it.name)
      } ?: run { _navigateUp.emit(Unit) }
    }
  }

  /** Deletes the area associated with this view model. */
  fun onRemoveButtonClick() {
    progressOverlayVisible.value = true
    viewModelScope.launch(ioDispatcher) { removeOfflineArea(area.value) }
  }

  private suspend fun removeOfflineArea(deletedArea: OfflineArea?) {
    if (deletedArea == null) return
    Timber.d("Removing offline area ${deletedArea.name}")
    offlineAreaRepository.removeFromDevice(deletedArea)
    _navigateUp.emit(Unit)
  }
}
