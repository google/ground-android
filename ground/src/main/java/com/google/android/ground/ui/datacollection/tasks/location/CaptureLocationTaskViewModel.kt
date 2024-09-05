/*
 * Copyright 2023 Google LLC
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
package com.google.android.ground.ui.datacollection.tasks.location

import android.location.Location
import androidx.lifecycle.viewModelScope
import com.google.android.ground.model.submission.CaptureLocationTaskData
import com.google.android.ground.model.submission.CaptureLocationTaskData.Companion.toCaptureLocationResult
import com.google.android.ground.ui.common.BaseMapViewModel
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Location lock states relevant for attempting to enable it or not. */
private enum class LocationLockEnabledState {
  /** The default, unknown state. */
  UNKNOWN,

  /** The location lock was already enabled, or an attempt was made. */
  ALREADY_ENABLED,

  /** The location lock was not already enabled. */
  NEEDS_ENABLE,

  /** Trigger to enable the location lock. */
  ENABLE,
}

class CaptureLocationTaskViewModel @Inject constructor() : AbstractTaskViewModel() {

  private val lastLocation = MutableStateFlow<CaptureLocationTaskData?>(null)
  /** Allows control for triggering the location lock programmatically. */
  private val enableLocationLockFlow = MutableStateFlow(LocationLockEnabledState.UNKNOWN)

  suspend fun updateLocation(location: Location) {
    lastLocation.emit(location.toCaptureLocationResult())
  }

  fun updateResponse() {
    if (lastLocation.value == null) {
      viewModelScope.launch { enableLocationLockFlow.emit(LocationLockEnabledState.ENABLE) }
    } else {
      setValue(lastLocation.value)
    }
  }

  fun enableLocationLock() {
    if (enableLocationLockFlow.value == LocationLockEnabledState.NEEDS_ENABLE) {
      viewModelScope.launch { enableLocationLockFlow.emit(LocationLockEnabledState.ENABLE) }
    }
  }

  suspend fun onMapReady(mapViewModel: BaseMapViewModel) {
    val locationLockEnabledState =
      if (mapViewModel.hasLocationPermission()) {
        // User has permission to enable location updates, enable it now.
        mapViewModel.enableLocationLockAndGetUpdates()
        LocationLockEnabledState.ALREADY_ENABLED
      } else {
        // Otherwise, wait to enable location lock until later.
        LocationLockEnabledState.NEEDS_ENABLE
      }
    enableLocationLockFlow.value = locationLockEnabledState
    enableLocationLockFlow.collect {
      if (it == LocationLockEnabledState.ENABLE) {
        // No-op if permission is already granted and location updates are enabled.
        mapViewModel.enableLocationLockAndGetUpdates()
        enableLocationLockFlow.value = LocationLockEnabledState.ALREADY_ENABLED
      }
    }
  }
}
