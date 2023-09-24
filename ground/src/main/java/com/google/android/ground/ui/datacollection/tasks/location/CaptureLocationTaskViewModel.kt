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

import android.Manifest
import android.content.res.Resources
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.submission.LocationTaskData
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CaptureLocationTaskViewModel
@Inject
constructor(
  private val locationManager: LocationManager,
  private val permissionsManager: PermissionsManager,
  private val settingsManager: SettingsManager,
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
  resources: Resources
) : AbstractTaskViewModel(resources) {

  private val lastLocation = MutableStateFlow<LocationTaskData?>(null)

  val locationDetailsText: LiveData<String?> =
    lastLocation.map { it?.getDetailsText() }.distinctUntilChanged().asLiveData()

  init {
    viewModelScope.launch(ioDispatcher) { listenToLocationUpdates() }
  }

  private suspend fun listenToLocationUpdates() {
    locationManager.locationUpdates
      .map { LocationTaskData.fromLocation(it) }
      .collect { lastLocation.emit(it) }
  }

  suspend fun enableLocationUpdates() {
    permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
    locationManager.requestLocationUpdates()
  }

  suspend fun disableLocationUpdates() {
    locationManager.disableLocationUpdates()
  }

  fun updateResponse() {
    setResponse(lastLocation.value)
  }
}
