package com.google.android.ground.system.stateflow

import android.location.Location
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.flow.MutableStateFlow

/** Implementation of [LocationCallback] linked to a [MutableStateFlow]. */
class StateFlowLocationCallback(private val locationStateFlow: MutableStateFlow<Location?>) :
  LocationCallback() {
  override fun onLocationResult(locationResult: LocationResult) {
    locationStateFlow.value = locationResult.lastLocation
  }

  override fun onLocationAvailability(p0: LocationAvailability) {
    // This happens sometimes when GPS signal is temporarily lost.
  }
}
