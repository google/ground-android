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
package com.google.android.ground.ui.offlineareas.viewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.imagery.OfflineArea
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.common.MapConfig
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.util.toMb
import com.google.android.ground.util.toMbString
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
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

  override val mapConfig: MapConfig
    get() =
      super.mapConfig.copy(
        showOfflineImagery = true,
        overrideMapType = MapType.TERRAIN,
        allowGestures = false,
      )

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
