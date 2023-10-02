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
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class CaptureLocationTaskViewModel
@Inject
constructor(
  private val locationManager: LocationManager,
  private val permissionsManager: PermissionsManager,
  private val settingsManager: SettingsManager,
  resources: Resources
) : AbstractTaskViewModel(resources) {

  val locationUpdates: LiveData<String> =
    locationManager.locationUpdates
      .map { it.toTaskData().displayText() }
      .distinctUntilChanged()
      .asLiveData()

  suspend fun enableLocationUpdates() {
    permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
    locationManager.requestLocationUpdates()
  }

  suspend fun disableLocationUpdates() {
    locationManager.disableLocationUpdates()
  }

  data class CapturedLocation(
    val coordinates: Coordinates,
    val altitude: Double?, // in metres
    val accuracy: Float? // in metres
  )

  companion object {
    private fun Location.toTaskData(): CapturedLocation {
      val altitude = if (hasAltitude()) altitude else null
      val accuracy = if (hasAccuracy()) accuracy else null
      return CapturedLocation(Coordinates(latitude, longitude), altitude, accuracy)
    }

    private fun CapturedLocation.displayText(): String {
      val df = DecimalFormat("#.##")
      df.roundingMode = RoundingMode.DOWN
      return "Location: ${processCoordinates(coordinates)}\n" +
        "Altitude: ${df.format(altitude)}m\n" +
        "Accuracy: ${df.format(accuracy)}m"
    }
  }
}
