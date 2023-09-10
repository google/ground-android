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
package com.google.android.ground.ui.datacollection.tasks.gps

import android.Manifest
import android.content.res.Resources
import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

class GpsTaskViewModel
@Inject
constructor(
  private val locationManager: LocationManager,
  private val permissionsManager: PermissionsManager,
  private val settingsManager: SettingsManager,
  resources: Resources
) : AbstractTaskViewModel(resources) {

  val locationUpdates: MutableLiveData<String?> = MutableLiveData(null)

  init {
    viewModelScope.launch {
      locationManager.locationUpdates.collect {
        val newDisplayText = displayText(it)
        if (locationUpdates.value != newDisplayText) {
          locationUpdates.value = newDisplayText
        }
      }
    }
  }

  suspend fun enableLocationUpdates() {
    permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
    locationManager.requestLocationUpdates()
  }

  suspend fun disableLocationUpdates() {
    locationManager.disableLocationUpdates()
  }

  private fun displayText(location: Location?): String =
    if (location == null) ""
    else
      location.accuracy.toString() +
        "\n" +
        location.altitude +
        "\n" +
        location.bearing +
        "\n" +
        location.latitude +
        "\n" +
        location.longitude
}
