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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.ground.R
import com.google.android.ground.model.imagery.MbtilesFile
import com.google.android.ground.model.imagery.TileSource
import com.google.android.ground.repository.MapStateRepository
import com.google.android.ground.repository.OfflineAreaRepository
import com.google.android.ground.repository.SurveyRepository
import com.google.android.ground.rx.Event
import com.google.android.ground.rx.annotations.Hot
import com.google.android.ground.system.FINE_LOCATION_UPDATES_REQUEST
import com.google.android.ground.system.LocationManager
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.PermissionsManager
import com.google.android.ground.system.SettingsManager
import com.google.android.ground.ui.map.Bounds
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.MapController
import com.google.android.ground.ui.map.MapType
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
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
  mapController: MapController,
  surveyRepository: SurveyRepository,
) : AbstractViewModel() {

  private val cameraZoomSubject: @Hot Subject<Float> = PublishSubject.create()
  val cameraZoomUpdates: Flowable<Float> = cameraZoomSubject.toFlowable(BackpressureStrategy.LATEST)

  private val cameraBoundsSubject: @Hot Subject<Bounds> = PublishSubject.create()
  val cameraBoundUpdates: Flowable<Bounds> =
    cameraBoundsSubject.toFlowable(BackpressureStrategy.LATEST)

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
  val cameraUpdateRequests: LiveData<Event<CameraPosition>>

  val locationAccuracy: StateFlow<Float?> =
    locationLock
      .combine(locationManager.locationUpdates) { locationLock, latestLocation ->
        if (locationLock.getOrDefault(false)) {
          latestLocation.accuracy
        } else {
          null
        }
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val tileOverlays: LiveData<List<TileSource>>
  val mbtilesFilePaths: LiveData<Set<String>>
  val offlineImageryEnabled: Flow<Boolean> = mapStateRepository.offlineImageryFlow

  init {
    cameraUpdateRequests = mapController.getCameraUpdates().map { Event.create(it) }.toLiveData()
    mapType = mapStateRepository.mapTypeFlowable.toLiveData()
    tileOverlays =
      surveyRepository.activeSurveyFlow
        .mapNotNull { it?.tileSources?.mapNotNull(this::toLocalTileSource) ?: listOf() }
        .asLiveData()
    mbtilesFilePaths =
      offlineAreaRepository
        .downloadedTileSetsOnceAndStream()
        .map { set: Set<MbtilesFile> -> set.map(MbtilesFile::path).toSet() }
        .toLiveData()
  }

  // TODO(#1790): Maybe create a new data class object which is not of type TileSource.
  private fun toLocalTileSource(tileSource: TileSource): TileSource? {
    if (tileSource.type != TileSource.Type.MOG_COLLECTION) return null
    return TileSource(
      "file://${offlineAreaRepository.getLocalTileSourcePath()}/{z}/{x}/{y}.jpg",
      TileSource.Type.TILED_WEB_MAP
    )
  }

  private suspend fun toggleLocationLock() {
    if (locationLock.value.getOrDefault(false)) {
      disableLocationLock()
    } else {
      try {
        permissionsManager.obtainPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        settingsManager.enableLocationSettings(FINE_LOCATION_UPDATES_REQUEST)

        enableLocationLock()

        locationManager.requestLocationUpdates()
      } catch (e: PermissionDeniedException) {
        locationLock.value = Result.failure(e)
        locationManager.disableLocationUpdates()
      }
    }
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
  fun onMapDragged() {
    if (locationLock.value.getOrDefault(false)) {
      Timber.d("User dragged map. Disabling location lock")
      viewModelScope.launch { disableLocationLock() }
    }
  }

  /** Called when the map camera is moved. */
  open fun onMapCameraMoved(newCameraPosition: CameraPosition) {
    newCameraPosition.zoomLevel?.let { cameraZoomSubject.onNext(it) }
    newCameraPosition.bounds?.let { cameraBoundsSubject.onNext(it) }
  }

  companion object {
    private const val LOCATION_LOCK_ICON_TINT_ENABLED = R.color.md_theme_primary
    private const val LOCATION_LOCK_ICON_TINT_DISABLED = R.color.md_theme_onSurfaceVariant

    // TODO(Shobhit): Consider adding another icon for representing "GPS disabled" state.
    private const val LOCATION_LOCK_ICON_ENABLED = R.drawable.ic_gps_lock
    private const val LOCATION_LOCK_ICON_DISABLED = R.drawable.ic_gps_lock_not_fixed
  }
}
