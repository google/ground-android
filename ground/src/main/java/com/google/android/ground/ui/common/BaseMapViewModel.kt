/*
 * Copyright 2022 Google LLC
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
package com.google.android.ground.ui.common

import android.Manifest
import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.Nil
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.*
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapController
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import timber.log.Timber

open class BaseMapViewModel
@Inject
constructor(
  private val locationManager: LocationManager,
  private val mapStateRepository: MapStateRepository,
  private val settingsManager: SettingsManager,
  private val permissionsManager: PermissionsManager,
  mapController: MapController
) : AbstractViewModel() {

  val locationLock: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.success(mapStateRepository.isLocationLockEnabled))
  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()
  private val selectMapTypeClicks: @Hot Subject<Nil> = PublishSubject.create()

  val locationLockIconTint =
    locationLock
      .map { lockState ->
        if (lockState.getOrDefault(false)) LOCATION_LOCK_ICON_TINT_ENABLED
        else LOCATION_LOCK_ICON_TINT_DISABLED
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, LOCATION_LOCK_ICON_TINT_DISABLED)
  val locationLockIcon =
    locationLock
      .map { lockState ->
        if (lockState.getOrDefault(false)) LOCATION_LOCK_ICON_ENABLED
        else LOCATION_LOCK_ICON_DISABLED
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, LOCATION_LOCK_ICON_DISABLED)
  val cameraUpdateRequests: LiveData<Event<CameraPosition>>

  init {
    cameraUpdateRequests =
      LiveDataReactiveStreams.fromPublisher(
        mapController.getCameraUpdates().map { Event.create(it) }
      )
  }

  private suspend fun toggleLocationLock() {
    if (locationLock.value.getOrDefault(false)) {
      disableLocationLock()

      locationManager.disableLocationUpdates()
    } else {
      try {
        permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION).await()

        settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST).await()

        enableLocationLock()

        locationManager.requestLocationUpdates().await()
      } catch (e: PermissionDeniedException) {
        locationLock.value = Result.failure(e)
        locationManager.disableLocationUpdates()
      }
    }
  }

  private fun enableLocationLock() = onLockStateChanged(true)

  /** Releases location enableLocationLock by disabling location updates. */
  private fun disableLocationLock() {
    locationManager.disableLocationUpdates()
    onLockStateChanged(false)
  }

  private fun onLockStateChanged(isLocked: Boolean) {
    locationLock.value = Result.success(isLocked)
    mapStateRepository.isLocationLockEnabled = isLocked
  }

  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
  }

  /** Called when map type button is clicked by the user. */
  fun onMapTypeButtonClicked() {
    selectMapTypeClicks.onNext(Nil.NIL)
  }

  fun getSelectMapTypeClicks(): Observable<Nil> {
    return selectMapTypeClicks
  }

  /** Called when location lock button is clicked by the user. */
  fun onLocationLockClick() {
    viewModelScope.launch { toggleLocationLock() }
  }

  /** Called when the map starts to move by the user. */
  fun onMapDragged() {
    if (locationLock.value.getOrDefault(false)) {
      Timber.d("User dragged map. Disabling location lock")
      disableLocationLock()
    }
  }

  /** Called when the map camera is moved by the user. */
  open fun onMapCameraMoved(newCameraPosition: CameraPosition) {}

  companion object {
    private const val LOCATION_LOCK_ICON_TINT_ENABLED = R.color.colorMapBlue
    private const val LOCATION_LOCK_ICON_TINT_DISABLED = R.color.colorGrey800

    // TODO(Shobhit): Consider adding another icon for representing "GPS disabled" state.
    private const val LOCATION_LOCK_ICON_ENABLED = R.drawable.ic_gps_lock
    private const val LOCATION_LOCK_ICON_DISABLED = R.drawable.ic_gps_lock_not_fixed
  }
}
