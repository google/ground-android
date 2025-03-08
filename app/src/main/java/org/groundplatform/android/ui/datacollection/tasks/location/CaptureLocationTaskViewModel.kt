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
package org.groundplatform.android.ui.datacollection.tasks.location

import android.location.Location
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.groundplatform.android.model.geometry.Point
import org.groundplatform.android.model.submission.CaptureLocationTaskData
import org.groundplatform.android.ui.common.BaseMapViewModel
import org.groundplatform.android.ui.datacollection.tasks.AbstractTaskViewModel
import org.groundplatform.android.ui.map.gms.getAccuracyOrNull
import org.groundplatform.android.ui.map.gms.getAltitudeOrNull
import org.groundplatform.android.ui.map.gms.toCoordinates

/** Location lock states relevant for attempting to enable it or not. */
enum class LocationLockEnabledState {
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

  private val _lastLocation = MutableStateFlow<Location?>(null)
  /** Allows control for triggering the location lock programmatically. */
  private val _enableLocationLockFlow = MutableStateFlow(LocationLockEnabledState.UNKNOWN)
  val enableLocationLockFlow = _enableLocationLockFlow.asStateFlow()

  fun updateLocation(location: Location) {
    _lastLocation.update { location }
  }

  private fun updateLocationLock(newState: LocationLockEnabledState) =
    _enableLocationLockFlow.update { newState }

  fun updateResponse() {
    val location = _lastLocation.value
    if (location == null) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    } else {
      setValue(
        CaptureLocationTaskData(
          location = Point(location.toCoordinates()),
          altitude = location.getAltitudeOrNull(),
          accuracy = location.getAccuracyOrNull(),
        )
      )
    }
  }

  fun enableLocationLock() {
    if (_enableLocationLockFlow.value == LocationLockEnabledState.NEEDS_ENABLE) {
      updateLocationLock(LocationLockEnabledState.ENABLE)
    }
  }

  // TODO: Investigate if this method be pulled to BasemapViewModel since location lock is available
  // Issue URL: https://github.com/google/ground-android/issues/2985
  //  for all map tasks.
  suspend fun initLocationUpdates(mapViewModel: BaseMapViewModel) {
    val locationLockEnabledState =
      if (mapViewModel.hasLocationPermission()) {
        // User has permission to enable location updates, enable it now.
        mapViewModel.enableLocationLockAndGetUpdates()
        LocationLockEnabledState.ALREADY_ENABLED
      } else {
        // Otherwise, wait to enable location lock until later.
        LocationLockEnabledState.NEEDS_ENABLE
      }
    updateLocationLock(locationLockEnabledState)
    _enableLocationLockFlow.collect {
      if (it == LocationLockEnabledState.ENABLE) {
        // No-op if permission is already granted and location updates are enabled.
        mapViewModel.enableLocationLockAndGetUpdates()
        updateLocationLock(LocationLockEnabledState.ALREADY_ENABLED)
      }
    }
  }
}
