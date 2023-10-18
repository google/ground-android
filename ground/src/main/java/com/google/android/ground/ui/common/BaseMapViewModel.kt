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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.ground.Config.DEFAULT_LOI_ZOOM_LEVEL
import com.google.android.ground.R
import com.google.android.ground.coroutines.IoDispatcher
import com.google.android.ground.model.Survey
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.repository.LocationOfInterestRepository
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.CameraUpdateRequest
import com.google.android.ground.ui.map.MapType
import com.google.android.ground.ui.map.gms.GmsExt.toBounds
import com.google.android.ground.ui.map.gms.toCoordinates
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
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
  @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AbstractViewModel() {

  private val _cameraUpdateRequests = MutableStateFlow<CameraUpdateRequest?>(null)

  val locationLock: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.success(mapStateRepository.isLocationLockEnabled))
  private val locationLockEnabled: @Hot(replays = true) MutableLiveData<Boolean> = MutableLiveData()
  val mapType: LiveData<MapType>

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

  val offlineTileSources: LiveData<List<TileSource>>

  /** Configuration to enable/disable base map features. */
  open val mapConfig: MapConfig = DEFAULT_MAP_CONFIG

  /** Flow of current position of camera. */
  var currentCameraPosition = MutableStateFlow<CameraPosition?>(null)
    private set

  /** Last camera position. */
  var lastCameraPosition: CameraPosition? = null
    private set

  init {
    mapType = mapStateRepository.mapTypeFlowable.toLiveData()
    offlineTileSources =
      offlineAreaRepository
        .getOfflineTileSourcesFlow()
        .combine(mapStateRepository.offlineImageryEnabledFlow) { offlineSources, enabled ->
          if (enabled) offlineSources else listOf()
        }
        .asLiveData()

    viewModelScope.launch(ioDispatcher) { updateCameraPositionOnLocationChange() }
    viewModelScope.launch(ioDispatcher) { updateCameraPositionOnSurveyChange() }
  }

  private suspend fun toggleLocationLock() {
    if (locationLock.value.getOrDefault(false)) {
      disableLocationLock()
    } else {
      try {
        enableLocationLockAndGetUpdates()
      } catch (e: Exception) {
        when (e) {
          is PermissionDeniedException,
          is ResolvableApiException -> handleRequestLocationUpdateFailed(e)
          else -> throw e
        }
      }
    }
  }

  suspend fun enableLocationLockAndGetUpdates() {
    permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)
    enableLocationLock()
    locationManager.requestLocationUpdates()
  }

  private suspend fun handleRequestLocationUpdateFailed(e: Exception) {
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
    mapStateRepository.isLocationLockEnabled = isLocked
  }

  fun getLocationLockEnabled(): LiveData<Boolean> = locationLockEnabled

  fun setLocationLockEnabled(enabled: Boolean) {
    locationLockEnabled.postValue(enabled)
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
  fun getCameraUpdateRequests(): Flow<CameraUpdateRequest> = _cameraUpdateRequests.filterNotNull()

  /** Emits a stream of current camera position. */
  fun getCurrentCameraPosition(): Flow<CameraPosition> = currentCameraPosition.filterNotNull()

  fun getLocationUpdates() = locationManager.locationUpdates.distinctUntilChanged()

  /**
   * Updates map camera when location changes. The first update pans and zooms the camera to the
   * appropriate zoom level and subsequent ones only pan the map.
   */
  @OptIn(ExperimentalCoroutinesApi::class)
  private suspend fun updateCameraPositionOnLocationChange() {
    locationLock
      .flatMapLatest { enabled ->
        getLocationUpdates()
          .map { it.toCoordinates() }
          .filter { enabled.getOrDefault(false) }
          .withIndex()
      }
      .collect { (index, coordinates) ->
        if (index == 0) {
          panAndZoomCamera(coordinates)
          // TODO(#1889): Track the zoom level in a VM associated with the MapFragment and use it in
          //  panCamera().

          // Set a small delay before emitting another value to allow previous zoom animation to
          // finish. Otherwise, the map camera stops at some other zoom level.
          delay(3000)
        } else {
          panCamera(coordinates)
        }
      }
  }

  /** Updates map camera when active survey changes. */
  private suspend fun updateCameraPositionOnSurveyChange() {
    surveyRepository.activeSurveyFlow
      .filterNotNull()
      .transform { getLastSavedPositionOrDefaultBounds(it)?.apply { emit(this) } }
      .collect { setCameraPosition(it, false) }
  }

  private suspend fun getLastSavedPositionOrDefaultBounds(survey: Survey): CameraPosition? {
    // Attempt to fetch last saved position from local storage.
    val savedPosition = mapStateRepository.getCameraPosition(survey.id)
    if (savedPosition != null) {
      return savedPosition.copy(isAllowZoomOut = true)
    }

    // Compute the default viewport which includes all LOIs in the given survey.
    val geometries = locationOfInterestRepository.getAllGeometries(survey)
    return geometries.toBounds()?.let { CameraPosition(bounds = it) }
  }

  private fun panCamera(coordinates: Coordinates) {
    setCameraPosition(CameraPosition(coordinates), true)
  }

  private fun panAndZoomCamera(coordinates: Coordinates) {
    setCameraPosition(CameraPosition(coordinates, DEFAULT_LOI_ZOOM_LEVEL), true)
  }

  /**
   * Requests moving the map camera to the given position.
   *
   * @param cameraPosition new position
   * @param shouldAnimate whether to animate the map camera or not
   */
  fun setCameraPosition(cameraPosition: CameraPosition, shouldAnimate: Boolean) {
    _cameraUpdateRequests.value = CameraUpdateRequest(cameraPosition, shouldAnimate)
  }

  /** Called when the map camera is moved. */
  open fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    lastCameraPosition = currentCameraPosition.value
    currentCameraPosition.value = newCameraPosition
  }

  companion object {
    private val LOCATION_LOCK_ICON_TINT_ENABLED = R.color.md_theme_primary
    private val LOCATION_LOCK_ICON_TINT_DISABLED = R.color.md_theme_onSurfaceVariant

    // TODO(#1789): Consider adding another icon for representing "GPS disabled" state.
    private val LOCATION_LOCK_ICON_ENABLED = R.drawable.ic_gps_lock
    private val LOCATION_LOCK_ICON_DISABLED = R.drawable.ic_gps_lock_not_fixed

    private val DEFAULT_MAP_CONFIG: MapConfig = MapConfig(showOfflineTileOverlays = true)
  }
}
