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

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.ground.R
import com.google.android.ground.coroutines.DefaultDispatcher
import com.google.android.ground.model.geometry.Coordinates
import com.google.android.ground.system.GeocodingManager
import com.google.android.ground.system.PermissionDeniedException
import com.google.android.ground.system.SettingsChangeRequestCanceled
import com.google.android.ground.ui.home.mapcontainer.MapTypeDialogFragmentDirections
import com.google.android.ground.ui.map.CameraPosition
import com.google.android.ground.ui.map.CameraUpdateRequest
import com.google.android.ground.ui.map.MapFragment
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineDispatcher
import timber.log.Timber

/** Injects a [MapFragment] in the container with id "map" and provides shared map functionality. */
abstract class AbstractMapContainerFragment : AbstractFragment() {

  @Inject lateinit var map: MapFragment
  @Inject lateinit var navigator: Navigator
  @Inject lateinit var geocodingManager: GeocodingManager
  @Inject @DefaultDispatcher lateinit var defaultDispatcher: CoroutineDispatcher

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    map.attachToParent(this, R.id.map) { onMapAttached(it) }
  }

  private fun onMapAttached(map: MapFragment) {
    val viewModel = getMapViewModel()

    // Removes all markers, overlays, polylines and polygons from the map.
    map.clear()

    launchWhenStarted { map.cameraMovedEvents.collect { viewModel.onMapCameraMoved(it) } }
    launchWhenStarted { map.startDragEvents.collect { viewModel.onMapDragged() } }
    launchWhenStarted { viewModel.locationLock.collect { onLocationLockStateChange(it, map) } }
    launchWhenStarted {
      viewModel.getCameraUpdateRequests().collect { onCameraUpdateRequest(it, map) }
    }

    applyMapConfig(map)
    onMapReady(map)
  }

  private fun applyMapConfig(map: MapFragment) {
    val viewModel = getMapViewModel()
    val config = viewModel.mapConfig

    // Map type
    if (config.overrideMapType != null) {
      map.mapType = config.overrideMapType
    } else {
      launchWhenStarted { viewModel.mapType.collect { map.mapType = it } }
    }

    // Tile overlays.
    if (config.showOfflineImagery) {
      viewModel.offlineTileSources.observe(viewLifecycleOwner) {
        map.clearTileOverlays()
        it.forEach(map::addTileOverlay)
      }
    }

    // Map gestures
    if (config.allowGestures) {
      map.enableGestures()
    } else {
      map.disableGestures()
    }

    if (config.allowRotateGestures) {
      map.enableRotation()
    } else {
      map.disableRotation()
    }
  }

  /** Opens a dialog for selecting a [MapType] for the basemap layer. */
  fun showMapTypeSelectorDialog() {
    val types = map.supportedMapTypes.toTypedArray()
    navigator.navigate(MapTypeDialogFragmentDirections.showMapTypeDialogFragment(types))
  }

  private fun onLocationLockStateChange(result: Result<Boolean>, map: MapFragment) {
    result.fold(
      onSuccess = {
        Timber.d("Location lock: $it")
        if (it) {
          try {
            map.enableCurrentLocationIndicator()
          } catch (t: Throwable) {
            // User disabled permission while the lock icon was enabled.
            onLocationLockStateError(t)
          }
        }
      },
      onFailure = { exception -> onLocationLockStateError(exception) },
    )
  }

  private fun onLocationLockStateError(t: Throwable?) {
    val messageId =
      when (t) {
        is PermissionDeniedException -> R.string.no_fine_location_permissions
        is SecurityException -> R.string.no_fine_location_permissions
        is SettingsChangeRequestCanceled -> R.string.location_disabled_in_settings
        else -> R.string.location_updates_unknown_error
      }
    Toast.makeText(context, messageId, Toast.LENGTH_LONG).show()
  }

  /** Moves the camera to a given position. */
  fun moveToPosition(
    coordinates: Coordinates,
    shouldAnimate: Boolean = true,
    isAllowZoomOut: Boolean = false,
  ) {
    onCameraUpdateRequest(
      CameraUpdateRequest(CameraPosition(coordinates), shouldAnimate, isAllowZoomOut),
      map,
    )
  }

  private fun onCameraUpdateRequest(cameraUpdateRequest: CameraUpdateRequest, map: MapFragment) {
    Timber.v("Update camera: $cameraUpdateRequest")
    val newPosition = cameraUpdateRequest.cameraPosition
    val shouldAnimate = cameraUpdateRequest.shouldAnimate
    val isAllowZoomOut = cameraUpdateRequest.isAllowZoomOut
    val bounds = newPosition.bounds
    val target = newPosition.target
    var zoomLevel = newPosition.zoomLevel

    if (target != null && zoomLevel != null && !isAllowZoomOut) {
      zoomLevel = max(zoomLevel, map.currentZoomLevel)
    }

    // TODO(#1712): Fix this once CameraPosition is refactored to not contain duplicated state
    if (bounds != null) {
      map.moveCamera(bounds, shouldAnimate)
    } else if (target != null && zoomLevel != null) {
      map.moveCamera(target, zoomLevel, shouldAnimate)
    } else if (target != null) {
      map.moveCamera(target, shouldAnimate)
    } else {
      error("Must have either target or bounds set")
    }
  }

  /** Called when the map is attached to the fragment. */
  protected open fun onMapReady(map: MapFragment) {}

  /** Provides an implementation of [BaseMapViewModel]. */
  protected abstract fun getMapViewModel(): BaseMapViewModel
}
