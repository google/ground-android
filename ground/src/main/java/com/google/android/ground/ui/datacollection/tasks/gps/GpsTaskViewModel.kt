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
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.datacollection.tasks.AbstractTaskViewModel
import com.google.android.ground.ui.datacollection.tasks.point.LatLngConverter.processCoordinates
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject
import kotlinx.coroutines.flow.map
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
      locationManager.locationUpdates
        .map { it.toGpsLocation().displayText() }
        .collect {
          val newDisplayText = it
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

  data class GpsLocation(
    val coordinates: Coordinates,
    val altitude: Double?, // in metres
    val accuracy: Float? // in metres
  )

  companion object {
    private fun Location.toGpsLocation(): GpsLocation {
      val altitude = if (hasAltitude()) altitude else null
      val accuracy = if (hasAccuracy()) accuracy else null
      return GpsLocation(Coordinates(latitude, longitude), altitude, accuracy)
    }

    private fun GpsLocation.displayText(): String {
      val df = DecimalFormat("#.##")
      df.roundingMode = RoundingMode.DOWN
      return "Location: ${processCoordinates(coordinates)}\n" +
        "Altitude: ${df.format(altitude)}m\n" +
        "Accuracy: ${df.format(accuracy)}m"
    }
  }
}
