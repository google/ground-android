/*
 * Copyright 2021 Google LLC
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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.ground.R
import com.google.android.ground.rx.RxAutoDispose
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.home.mapcontainer.MapTypeDialogFragmentDirections
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.Map
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.launch
import timber.log.Timber

/** Injects a [Map] in the container with id "map" and provides shared map functionality. */
abstract class AbstractMapContainerFragment : AbstractFragment() {

  @Inject lateinit var map: Map
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var geocodingManager: GeocodingManager

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    map.attachToFragment(this, R.id.map) { onMapAttached(it) }
  }

  private fun onMapAttached(map: Map) {
    // Removes all markers, overlays, polylines and polygons from the map.
    map.clear()

    map.cameraMovedEvents
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { onMapCameraMoved(it) }
    map.startDragEvents
      .onBackpressureLatest()
      .`as`(RxAutoDispose.disposeOnDestroy(this))
      .subscribe { getMapViewModel().onMapDragged() }

    lifecycleScope.launch {
      getMapViewModel().locationLock.collect { onLocationLockStateChange(it, map) }
    }
    lifecycleScope.launch {
      getMapViewModel().getCameraUpdates().collect { onCameraUpdateRequest(it, map) }
    }

    // Enable map controls
    getMapViewModel().setLocationLockEnabled(true)

    applyMapConfig(map)
    onMapReady(map)
  }

  private fun applyMapConfig(map: Map) {
    val config = getMapViewModel().mapConfig

    // Map Type
    if (config.overrideMapType != null) {
      map.mapType = config.overrideMapType
    } else {
      getMapViewModel().mapType.observe(viewLifecycleOwner) { map.mapType = it }
    }

    // Offline imagery
    if (config.showTileOverlays) {
      lifecycleScope.launch {
        getMapViewModel().offlineImageryEnabled.collect { enabled ->
          if (enabled) addTileOverlays() else map.clearTileOverlays()
        }
      }
    }
  }

  @SuppressLint("FragmentLiveDataObserve")
  private fun addTileOverlays() {
    // TODO(#1756): Clear tile overlays on change to stop accumulating them on map.

    // TODO(#1782): Changing the owner to `viewLifecycleOwner` in observe() causes a crash in task
    //  fragment and converting live data to flow results in clear tiles not working. Figure out a
    //  better way to fix the IDE warning.
    getMapViewModel().tileOverlays.observe(this) { it.forEach(map::addTileOverlay) }
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  fun showMapTypeSelectorDialog() {
    val types = map.supportedMapTypes.toTypedArray()
    navigator.navigate(MapTypeDialogFragmentDirections.showMapTypeDialogFragment(types))
  }

  private fun onLocationLockStateChange(result: Result<Boolean>, map: Map) {
    result.fold(
      onSuccess = {
        Timber.d("Location lock: $it")
        if (it) {
          map.enableCurrentLocationIndicator()
        }
      },
      onFailure = { exception -> onLocationLockStateError(exception) }
    )
  }

  private fun onLocationLockStateError(t: Throwable?) {
    val messageId =
      when (t) {
        is PermissionDeniedException -> R.string.no_fine_location_permissions
        is SettingsChangeRequestCanceled -> R.string.location_disabled_in_settings
        else -> R.string.location_updates_unknown_error
      }
    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show()
  }

  private fun onCameraUpdateRequest(newPosition: CameraPosition, map: Map) {
    Timber.v("Update camera: %s", newPosition)
    val bounds = newPosition.bounds
    val target = newPosition.target
    var zoomLevel = newPosition.zoomLevel

    if (target != null && zoomLevel != null && !newPosition.isAllowZoomOut) {
      zoomLevel = max(zoomLevel, map.currentZoomLevel)
    }

    // TODO(#1712): Fix this once CameraPosition is refactored to not contain duplicated state
    if (bounds != null) {
      map.moveCamera(bounds)
    } else if (target != null && zoomLevel != null) {
      map.moveCamera(target, zoomLevel)
    } else if (target != null) {
      map.moveCamera(target)
    } else {
      error("Must have either target or bounds set")
    }

    // Manually notify that the camera has moved as `map.cameraMovedEvents` only returns
    // an event when the map is moved by the user (REASON_GESTURE).
    onMapCameraMoved(newPosition)
  }

  /** Called when the map camera is moved by the user or due to current location/survey changes. */
  protected open fun onMapCameraMoved(position: CameraPosition) {
    getMapViewModel().onMapCameraMoved(position)
  }

  /** Called when the map is attached to the fragment. */
  protected abstract fun onMapReady(map: Map)

  /** Provides an implementation of [BaseMapViewModel]. */
  protected abstract fun getMapViewModel(): BaseMapViewModel
}
