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
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE
import com.google.android.ground.Config.DEFAULT_LOI_ZOOM_LEVEL
import com.google.android.ground.R
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.CameraUpdateRequest
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.ui.map.NewCameraPositionViaBounds
import com.google.android.ground.ui.map.NewCameraPositionViaCoordinates
import com.google.android.ground.ui.map.NewCameraPositionViaCoordinatesAndZoomLevel
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import com.google.android.ground.ui.map.gms.toCoordinates
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch
import timber.log.Timber

open class BaseMapViewModel
@Inject
constructor(
  private val locationManager: LocationManager,
  private val mapStateRepository: MapStateRepository,
  private val settingsManager: SettingsManager,
  private val offlineAreaRepository: OfflineAreaRepository,
  private val permissionsManager: PermissionsManager,
  private val surveyRepository: SurveyRepository,
  private val locationOfInterestRepository: LocationOfInterestRepository,
) : AbstractViewModel() {

  val locationLock: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.success(mapStateRepository.isLocationLockEnabled))
  val mapType: Flow<MapType> = mapStateRepository.mapTypeFlow

  val locationLockIconTint =
    locationLock
      .map { lockState ->
        if (lockState.getOrDefault(false)) R.color.md_theme_primary
        else R.color.md_theme_onSurfaceVariant
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, R.color.md_theme_onSurfaceVariant)

  // TODO(#1789): Consider adding another icon for representing "GPS disabled" state.
  val locationLockIcon =
    locationLock
      .map { lockState ->
        if (lockState.getOrDefault(false)) R.drawable.ic_gps_lock
        else R.drawable.ic_gps_lock_not_fixed
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, R.drawable.ic_gps_lock_not_fixed)

  val location: StateFlow<Location?> =
    locationLock
      .combine(getLocationUpdates()) { locationLock, latestLocation ->
        if (locationLock.getOrDefault(false)) {
          latestLocation
        } else {
          null
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  /** Configuration to enable/disable base map features. */
  open val mapConfig: MapConfig = DEFAULT_MAP_CONFIG

  /** Flow of current position of camera. */
  var currentCameraPosition = MutableStateFlow<CameraPosition?>(null)
    private set

  val offlineTileSources: LiveData<TileSource?> =
    offlineAreaRepository
      .getOfflineTileSourcesFlow()
      .combine(mapStateRepository.offlineImageryEnabledFlow) { offlineSources, enabled ->
        if (enabled) offlineSources else null
      }
      .asLiveData()

  /** Returns whether the user has granted fine location permission. */
  fun hasLocationPermission() =
    permissionsManager.isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

  private suspend fun toggleLocationLock() {
    if (locationLock.value.getOrDefault(false)) {
      disableLocationLock()
    } else {
      enableLocationLockAndGetUpdates()
    }
  }

  suspend fun enableLocationLockAndGetUpdates() {
    try {
      try {
        permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
      } catch (throwable: ApiException) {
        val statusCode = throwable.statusCode
        if (statusCode == SETTINGS_CHANGE_UNAVAILABLE) {
          Timber.e(
            throwable,
            "User is offline, so fallback to user's current permission, which may also fail.",
          )
        } else {
          throw throwable
        }
      }
      enableLocationLock()
      locationManager.requestLocationUpdates()
    } catch (e: Throwable) {
      handleRequestLocationUpdateFailed(e)
    }
  }

  private suspend fun handleRequestLocationUpdateFailed(e: Throwable) {
    Timber.e(e)
    locationLock.value = Result.failure(e)
    locationManager.disableLocationUpdates()
  }

  private fun enableLocationLock() = onLockStateChanged(true)

  /** Releases location enableLocationLock by disabling location updates. */
  private suspend fun disableLocationLock() {
    onLockStateChanged(false)
    locationManager.disableLocationUpdates()
  }

  private fun onLockStateChanged(isLocked: Boolean) {
    locationLock.value = Result.success(isLocked)
  }

  /** Called when location lock button is clicked by the user. */
  fun onLocationLockClick() {
    viewModelScope.launch { toggleLocationLock() }
  }

  /** Called when the map starts to move by the user. */
  open fun onMapDragged() {
    if (locationLock.value.getOrDefault(false)) {
      Timber.d("User dragged map. Disabling location lock")
      viewModelScope.launch { disableLocationLock() }
    }
  }

  /** Emits a stream of camera update requests. */
  fun getCameraUpdateRequests(): SharedFlow<CameraUpdateRequest> =
    merge(
        getCameraUpdateRequestsForSurveyActivations(),
        getCameraUpdateRequestsForDeviceLocationChanges(),
      )
      .shareIn(viewModelScope, SharingStarted.Eagerly, replay = 0)

  /** Emits a stream of current camera position. */
  fun getCurrentCameraPosition(): Flow<CameraPosition> = currentCameraPosition.filterNotNull()

  fun getLocationUpdates() = locationManager.locationUpdates.distinctUntilChanged()

  /**
   * Updates map camera when location changes. The first update pans and zooms the camera to the
   * appropriate zoom level and subsequent ones only pan the map.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private fun getCameraUpdateRequestsForDeviceLocationChanges(): Flow<CameraUpdateRequest> =
    locationLock
      .flatMapLatest { enabled ->
        getLocationUpdates()
          .map { it.toCoordinates() }
          .filter { enabled.getOrDefault(false) }
          .withIndex()
      }
      .map { (index, coordinates) -> onLocationUpdate(index, coordinates) }

  private suspend fun onLocationUpdate(index: Int, coordinates: Coordinates): CameraUpdateRequest =
    if (index == 0) {
      NewCameraPositionViaCoordinatesAndZoomLevel(
        coordinates,
        DEFAULT_LOI_ZOOM_LEVEL,
        isAllowZoomOut = false,
        true,
      )
    } else {
      // Set a small delay before emitting another value to allow previous zoom animation to
      // finish. Otherwise, the map camera stops at some other zoom level.
      if (index == 1) {
        delay(3000)
      }

      // TODO(#1889): Track the zoom level in a VM associated with the MapFragment and use here
      NewCameraPositionViaCoordinates(coordinates, shouldAnimate = true)
    }

  /** Emits a new camera update request when active survey changes. */
  private fun getCameraUpdateRequestsForSurveyActivations(): Flow<CameraUpdateRequest> =
    surveyRepository.activeSurveyFlow
      .filterNotNull()
      .map { getLastSavedPositionOrDefaultBounds(it) }
      .filterNotNull()

  private suspend fun getLastSavedPositionOrDefaultBounds(survey: Survey): CameraUpdateRequest? {
    // Attempt to fetch last saved position from local storage.
    val savedPosition = mapStateRepository.getCameraPosition(survey.id)
    if (savedPosition != null) {
      return if (savedPosition.zoomLevel == null) {
        NewCameraPositionViaCoordinates(savedPosition.coordinates)
      } else
        NewCameraPositionViaCoordinatesAndZoomLevel(
          savedPosition.coordinates,
          savedPosition.zoomLevel,
          isAllowZoomOut = true,
        )
    }

    // Compute the default viewport which includes all LOIs in the given survey.
    val geometries = locationOfInterestRepository.getValidLois(survey).first().map { it.geometry }
    return geometries.toBounds()?.let { NewCameraPositionViaBounds(bounds = it, padding = 100) }
  }

  /** Called when the map camera is moved. */
  open fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    Timber.v("Camera moved : ${newCameraPosition.coordinates}")
    currentCameraPosition.value = newCameraPosition
    mapStateRepository.setCameraPosition(newCameraPosition)
  }

  companion object {
    private val DEFAULT_MAP_CONFIG: MapConfig = MapConfig(showOfflineImagery = true)
  }
}
